package com.google.code.eforceconfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.code.eforceconfig.util.StringHandler;
import com.google.code.eforceconfig.exphandlers.ConfigSetExpressionHandler;
import com.google.code.eforceconfig.exphandlers.ConstantExpressionHandler;

import org.apache.oro.util.CacheLRU;
import org.apache.log4j.Logger;

public class Config 
{ 
  //updatable
  public static String SINGLETON_CONFIGSET= "singleton";
  //updatable
  public static String expMarker= "%";
  
  private static Logger logger= Logger.getLogger(Config.class);
  private static Config config;
  private static HashMap configSets= new HashMap();
  private static HashMap expHandlers= new HashMap();
  private int cacheSize=10;
  private int fastCacheSize=5;  
  private String name;
  private CacheLRU fastCache;
  private ConfigCache configCache;
  private ConfigSourceManager sourceManager;
  private Thread changeControlThread;
  private Thread garbageCollectorThread;
  private ChangeController changeController;
  private GarbageCollector garbageCollector;
  
  
  /* stand-alone non singleton use */
  public Config()
  {
      registerExpressionHandler("configset",new ConfigSetExpressionHandler());
      registerExpressionHandler("constant",new ConstantExpressionHandler());
  }

  /* registered non singleton use */
  public Config(String name)
  {
      this();
      this.name= name;
      registerConfigSet(name,this);
  }

  /* singleton use */
  public static Config getInstance()
  { 
    if (config==null) 
     synchronized(Config.class)
     {
       config= new Config(SINGLETON_CONFIGSET);
     }
    
    return config;
  }
  
  public static void registerConfigSet(String name, Config config)
  {
      configSets.put(name,config);
  }

  public static void registerExpressionHandler(String name, ExpressionHandler eh)
  {
      expHandlers.put(name,eh);
  }
  
  public static Config getConfigSet(String name)
  {
     Config c= (Config)configSets.get(name);   

     if (c!=null)
         return c;
     else
         throw new RuntimeException("ConfigSet for name '"+name+"' not found.");
  }
  
  private static ExpressionHandler getExpressionHandler(EntityConfig requester, String type)
  {
      if ("local".equals(type)) return (ExpressionHandler)requester;
      
      ExpressionHandler eh= (ExpressionHandler)expHandlers.get(type);
      
      if (eh!=null)
          return eh;
      else
          throw new RuntimeException("ExpressionHandler for type '"+type+"' not found.");
  }
  
  public static String decode(String value)
  {
      return decode(null,value);
  }
  
  protected static String decode(EntityConfig requester, String value)
  {
      if (expMarker.equals(SQLStatement.expMarker))
          throw new RuntimeException("expMarker for Config expressions and SQLStatement bind variables must be different");
      
      String ret= value;
      String[] exprs= StringHandler.getStringsBetween(value,expMarker+"{","}");

      for (int i=0;i<exprs.length;i++)
      {
          int pos= exprs[i].indexOf(":");
          String type= exprs[i].substring(0,pos);
          String exp= exprs[i].substring(pos+1);
          
          ExpressionHandler eh= getExpressionHandler(requester,type);
          String expValue;
          
          if (requester!=null&&eh instanceof ConfigSetExpressionHandler)
          {
             expValue=((ConfigSetExpressionHandler)eh).translate(requester,exp);
          }
          else
             expValue= eh.translate(exp);
          
          ret= StringHandler.replace(ret,expMarker+"{"+exprs[i]+"}",expValue);
      }
      
      
      return ret;
  }

  public boolean isInited()
  {
    return !(configCache==null);
  }

  public void reset()
  {
     logger.info("Called reset(): invalidating caches...");
     configCache= new ConfigCache(cacheSize);
     fastCache= new CacheLRU(fastCacheSize);
  }

  public void reset(int cacheSize, int fastCacheSize)
  {
     logger.info("Called reset(n,n): resizing caches...");
     this.cacheSize= cacheSize;
     this.fastCacheSize= fastCacheSize;
     reset();
  }
  
  public static EntityConfig getInstance(String entityName)
  { 
    return getInstance().getEntity(entityName);
  }

  public EntityConfig getEntity(String entityName)
  {
      EntityConfig ec=null;

      if ((ec=(EntityConfig)configCache.get(entityName))==null)
      {
          try
          {
            ec= configCache.cache(new EntityConfigImpl(sourceManager.getEntitySource(entityName),this));
          }
          catch (ConfigNotFoundException e)
          {
            logger.debug("getEntity(\""+entityName+"\") config not found: "+e.getCause().getMessage());
          }
          catch (Exception e)
          {
            logger.debug("getEntity(\""+entityName+"\") error: ",e);
          }
      }
    
      return ec;
  }

  public void setSourceManager(ConfigSourceManager sourceManager)
  {
    this.sourceManager= sourceManager;
  }

  public synchronized void init(ConfigInitializer ci)
  throws ConfigException
  {
  
     sourceManager= ci.getConfigSourceManager();
     
     logger.info("Config init() from ConfigInitializer: "+ci);
     logger.info("Config init() ConfigSourceManager: "+sourceManager);

     if (ci.getJAXPFactoryClassName()!=null)
        System.setProperty("javax.xml.parsers.SAXParserFactory",
                           ci.getJAXPFactoryClassName());
      
     cacheSize= ci.getCacheSize();
     configCache= new ConfigCache(cacheSize);
     
     fastCache= new CacheLRU(fastCacheSize);
     
     if (ci.getPreloadCacheOnInit())
     {
         ConfigSource[] entityConfigs= ci.getConfigSources();
    
         for (int i=0;i<entityConfigs.length&&i<configCache.size();i++)
           configCache.cache(new EntityConfigImpl(entityConfigs[i],this));
     }
     
     if (ci.getStartChangeControl()&&sourceManager.supportsChangeControl())
     {
       changeController= new ChangeController(ci.getChangeControlInterval());
       changeControlThread= new Thread(changeController,"eforce.util.config.ChangeController["+name+"]");
       changeControlThread.start();
       logger.info("Started change control thread!"); 
     }

     if (ci.getStartGarbageCollector())
     {
         garbageCollector= new GarbageCollector(ci.getGarbageCollectorInterval());
         garbageCollectorThread= new Thread(garbageCollector,"eforce.util.config.GarbageCollector["+name+"]");
         garbageCollectorThread.start();
         logger.info("Started garbage collector thread!"); 
     }

     logger.info("Config inited!");
  }
  
  public void stop()
  {
      if (changeController!=null)
      {
        logger.debug("stopping change controller...");
        changeController.stop();
      }

      if (garbageCollector!=null)
      {
        logger.debug("stopping garbage collector...");
        garbageCollector.stop();
      }
  }

  public static void stopAll()
  {
      Iterator i= configSets.entrySet().iterator();
      
      while (i.hasNext())
      {
          Map.Entry e= (Map.Entry)i.next();
          ((Config)e.getValue()).stop();
      }
  }
  
  private class GarbageCollector implements Runnable
  {
      protected boolean haveToStop= false;
      private int interval;
      
      private GarbageCollector(int interval)
      {
          this.interval= interval;
      }
      
      public void run()
      {
         while (!haveToStop)
         {
              try
              {
                Thread.sleep(interval);  
                if (!haveToStop) configCache.gc();
              }
              catch (Throwable e)
              {
                logger.error("searching changes",e);
              }
         }
      }
      
      public void stop()
      {
         haveToStop= true;
      }
  }
  
  private class ChangeController implements Runnable
  {
     protected boolean haveToStop= false;
     private int interval;
     
     private ChangeController(int interval)
     {
         this.interval= interval;
     }
     
     private void lookForChanges()
     throws Exception
     {
          Iterator i= configCache.entityIterator();
          
          while (i.hasNext())
          {
        	  EntityConfigImpl e= (EntityConfigImpl)i.next();
              
              logger.debug("entity: "+e.getName()+" cs lcd: "+(e.getConfigSource()).getLastChangeDate()+" e lcd: "+e.getLastChangeDate());
              
              if (!(e.getConfigSource()).getLastChangeDate().equals(e.getLastChangeDate()))
              {
                logger.debug("starting notifier");
                (new Thread(new ChangeNotifier(e))).start();
              }
          }
     }
     
     public void run()
     {
        while (!haveToStop)
        {
             try
             {
               Thread.sleep(interval);  
               //long before= System.currentTimeMillis();
               if (!haveToStop) lookForChanges();
               //logger.debug("looked for changes for "+(System.currentTimeMillis()-before)+"ms");
             }
             catch (Throwable e)
             {
               logger.error("searching changes",e);
             }
        }
     }
     
     public void stop()
     {
        haveToStop= true;
     }
     
     class ChangeNotifier implements Runnable
     {
         private EntityConfigImpl e;
         
         ChangeNotifier(EntityConfigImpl e)
         {
           this.e= e;
         }
         
         public void run()
         { 
           try
           {
              e.notifyChange();
            
              if (e.getCacheMode()==EntityConfig.CACHE_MODE_NOCACHE)
                configCache.remove(e.getName());
            
              fastCache= new CacheLRU(fastCacheSize); // FIXME: locking ??? fastCache used only on getInstanceByURL...
           }
           catch (ConfigNotFoundException cnfe)
           {
             haveToStop= true;
             logger.error(this,cnfe);
           }
           catch (Exception e)
           {
             logger.error(this,e);
           }
         }
     }
  }
}
