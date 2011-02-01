package com.google.code.eforceconfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.code.eforceconfig.util.concurrency.RWLock;

/**
 * This class caches "size" number
 * of entities if gc notice that the cache
 * "current size" exceed "size" removes
 * ("current size"-"size") REMOVABLE enities
 * to have the cache back to his wished size.
 * 
 * REMOVABLE: means that there are no pointers
 * to the enitity in the JVM. This beacause
 * the change control needs to have instances
 * to update, and more generaly because each 
 * entity (and each entity component) that does not
 * have the "nocache" attribute set in the xml,
 * MUST exists (by rule) only one time in the
 * JVM because of resource consumption and updateability.
 * 
 * So that if you have:
 * 
 *  private static EntityConfig myconfig= Config.getConfigSet("local-prefs").getInstance("myclassconfig");
 * 
 * and you use:
 * 
 *  myconfig.getParmeter("my-par-x");
 * 
 * my-par-x will always be keep up to date from
 * change controller...
 * 
 * TODO: Rendere interfaccia:
 * 
 * 1) con implementazione gc
 * 2) senza
 * 
 * così se non deve partire il gc non vine gestita neanche la logica 
 * 
 * @author andrea
 *
 */
public class ConfigCache
{
   private static final Logger logger= Logger.getLogger(ConfigCache.class);
   private int size=0;
   private HashMap table;
   private RWLock lock= new RWLock();
   private ConfigCacheElement first;
   private ConfigCacheElement last;
   
   public ConfigCache(int cacheSize) // FIXME: not thread safe
   {
       size= cacheSize;
       table= new HashMap(cacheSize);
   }
   
   /* WARNING: do not request to cache a cached entity or we loose finalizers for the entity
    * don't want to add check code, this shold be used only from eforce.util.config.Config !!! */
   public EntityConfig cache(EntityConfig entity)
   {
       if (entity.getCacheMode()!=EntityConfig.CACHE_MODE_NOCACHE)
       {
           ConfigCacheElement cce= new ConfigCacheElement(entity);
           RWLock.Lock l= lock.getWriteLock();
           table.put(entity.getName(), cce);
           lock.releaseLock(l);
           return cce.getFinalizer();
       }
       else
           return entity;
       
   }

   public void remove(String name)
   {
       RWLock.Lock l= lock.getWriteLock();
       ConfigCacheElement cce= (ConfigCacheElement)table.remove(name);
       if (cce!=null)
         removeFromGarbageList(cce);
       lock.releaseLock(l);
   }
   
   public EntityConfig get(String name)
   {
       RWLock.Lock l= lock.getReadLock();
       ConfigCacheElement cce= (ConfigCacheElement)table.get(name);
       lock.releaseLock(l);
       
       return (cce!=null ? cce.getFinalizer() : null); 
   }
   
   public boolean contains(String name)
   {
       RWLock.Lock l= lock.getReadLock();
       boolean res= table.containsKey(name);
       lock.releaseLock(l);
       
       return res;
   }
   
   public void gc()
   {
       long before=0L;

       RWLock.Lock l= lock.getWriteLock();
       
       if (logger.isDebugEnabled())
       {
         before= System.currentTimeMillis();
         logger.debug("cache size: "+size+" current: "+table.size());
       }
       
       if (first!=null)
       while (table.size()>size)
       {
             if (first.alive==0)
             {
               table.remove(first.entity.getName());
               logger.debug("gc removed entity: "+first.entity.getName());
             }
             else
               logger.debug("gc found entity: "+first.entity.getName()+" alive");
    
             if (first.next==null)
             {
                 first= last= null;
                 break;
             }
             else
             {
                 first= first.next;
                 first.prev= null;
             }
       }
       
       if (logger.isDebugEnabled())
         logger.debug("cache gc performed in "+(System.currentTimeMillis()-before)+"ms"); 
           
       lock.releaseLock(l);
   }
   
   public int size()
   {
      return size;       
   }
   
   private void removeFromGarbageList(ConfigCacheElement cce)
   {
       if (last!=null)
       {
           if (cce.next!=null) //i'am not the last
           {
              if (cce.prev!=null) //i'am not the first
              {
                  cce.prev.next= cce.next;
                  cce.next.prev= cce.prev;
              }
              else  // i'am the first
              {
                  first= cce.next;
                  cce.next.prev= null;
              }
           }
           else //i'am the last
           {
              if (cce.prev!=null) //i'am not alone
              {
                cce.prev.next= null;
                last= cce.prev;
              }
              else //i'am alone
              {
                last= first= null;  
              }
           }
       }
       
       cce.next= cce.prev= null;
       cce.ingclist= false;
   }
   
   public Iterator entityIterator()
   {
       final RWLock.Lock l= lock.getReadLock();
       
       return new Iterator()
       {
            Iterator i= table.entrySet().iterator();
            
            public boolean hasNext()
            {
                if (i.hasNext())
                  return true;
                else
                {
                  lock.releaseLock(l);
                  return false;
                }
            }
    
            public Object next()
            {
                return ((ConfigCacheElement)((Map.Entry)i.next()).getValue()).entity;
            }
    
            public void remove()
            {
              throw new RuntimeException("disabled");
            }
           
       };
   }   

   private class ConfigCacheElement
   {
       private EntityConfig entity;
       private ConfigCacheElement prev;
       private ConfigCacheElement next;
       private boolean ingclist= false;
       private int alive= 0;
       
       private ConfigCacheElement(EntityConfig entity)
       {
           this.entity= entity;
       }
       
       private synchronized EntityConfig getFinalizer()
       {
           alive++;
           return new EntityConfigFinalizer(entity);
       } 
   
       private synchronized void releaseFinalizer()
       {
           alive--;
           
           if (alive==0) // move this element to last
           {
               RWLock.Lock l= lock.getWriteLock();
               
               try // aggiunto per controllare uno strano errore
               {
                   if (last==null)
                       first= last= this;
                   else
                   {
                        if (ingclist)
                          removeFromGarbageList(this);
                        
                        prev= last;
                        last= last.next= this;
                        ingclist= true;
                   }
               }
               catch (Throwable tw)
               {
                   logger.error("checking if an exception occurs between getWriteLock and releaseLock",tw);
               }
               
               lock.releaseLock(l);
           }
       }

       private class EntityConfigFinalizer implements EntityConfig
       {
           private EntityConfig wrapped;
           
           private EntityConfigFinalizer(EntityConfig entity)
           {
               this.wrapped= entity;
           }
           
           protected void finalize()
           throws Throwable
           {
               try
               {
                  releaseFinalizer();
               }
               catch (Exception ex)
               {
                  logger.error(ex);
               }
               
               super.finalize();
           }
    
         public void addDependent(EntityConfig ec)
         {
             wrapped.addDependent(ec);
         }
    
         public ComponentConfig getComponent(String name)
         {
             return wrapped.getComponent(name);
         }
    
         public ArrayList getComponents()
         {
             return wrapped.getComponents();
         }
    
         public Config getConfigSet()
         {
             return wrapped.getConfigSet();
         }
    
         public ArrayList getDependents()
         {
             return wrapped.getDependents();
         }
    
         public Date getLastChangeDate()
         {
             return wrapped.getLastChangeDate();
         }
    
         public void notifyChange()
         throws ConfigException
         {
             wrapped.notifyChange();
         }
    
         public boolean getBooleanParameter(String name)
         {
             return wrapped.getBooleanParameter(name);
         }
    
         public boolean getBooleanParameter(String name, boolean nvl)
         {
             return wrapped.getBooleanParameter(name,nvl);
         }
    
         public Object getClassInstanceParameter(String name)
         {
             return wrapped.getClassInstanceParameter(name);
         }
    
         public Class getClassParameter(String name)
         {
             return wrapped.getClassParameter(name);
         }
    
         public double getDoubleParameter(String name)
         {
             return wrapped.getDoubleParameter(name);
         }
    
         public int getIntParameter(String name)
         {
             return wrapped.getIntParameter(name);
         }
    
         public ArrayList getListParameter(String name)
         {
             return wrapped.getListParameter(name);
         }
    
         public String getName()
         {
             return wrapped.getName();
         }
    
         public String getParameter(String name)
         {
             return wrapped.getParameter(name);
         }
    
         public Hashtable getParameters()
         {
             return wrapped.getParameters();
         }
    
         public EntityConfig getParent()
         {
             return wrapped.getParent();
         }
    
         public Properties getPropertiesParameter(String name)
         {
             return wrapped.getPropertiesParameter(name);
         }
    
         public SQLStatement getSQLStatement(String name)
         {
             return wrapped.getSQLStatement(name);
         }
    
         public Hashtable getSQLStatements()
         {
             return wrapped.getSQLStatements();
         }
    
         public String getSQLstmt(String name)
         {
             return wrapped.getSQLstmt(name);
         }
    
         public Hashtable getSQLstmts()
         {
             return wrapped.getSQLstmts();
         }
    
         public Hashtable getTableParameter(String name)
         {
             return wrapped.getTableParameter(name);
         }
    
         public String translate(String value)
         {
             return wrapped.translate(value);
         }
    
         public int getCacheMode()
         {
             return wrapped.getCacheMode();
         }

 		 public EntityConfig getSuperEntity()
		 {
			 return wrapped.getSuperEntity();
		 }

        public void setWarnNotFound(boolean warnNotFound)
        {
            wrapped.setWarnNotFound(warnNotFound);
        }
    
       }

   }
}
