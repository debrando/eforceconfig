package com.google.code.eforceconfig;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.google.code.eforceconfig.util.StringHandler;
import com.google.code.eforceconfig.util.concurrency.RWLock;

public class EntityConfigImpl extends BaseConfigImpl implements EntityConfig
{   
    private static Logger logger= Logger.getLogger(EntityConfigImpl.class);
    //private static final int cacheSize=3;
    private Config configSet;
    private EntityConfig superEntity;
    private ArrayList componentsNames;
    private ConfigSource xmlSource;
    private HashMap componentsMap;
    private Date lastChanged;
    private SAXParser parser;
    private int cacheMode;
    private ArrayList dependentEntityConfigs= new ArrayList();
    
    protected EntityConfigImpl(ConfigSource xmlSource, Config configSet) 
    throws ConfigException
    {
        this.configSet= configSet;
        this.xmlSource= xmlSource;
        this.lastChanged= xmlSource.getLastChangeDate();
        
        SAXParserFactory saxfactory = SAXParserFactory.newInstance();
        saxfactory.setNamespaceAware(true);
        saxfactory.setValidating(true);
        componentsMap= new HashMap();
        componentsNames= new ArrayList();
        
        try
        {
          parser = saxfactory.newSAXParser();
          parser.setProperty(SCHEMA_LANGUAGE,XML_SCHEMA);
          parser.setProperty(SCHEMA_SOURCE,EntityConfig.class.getResourceAsStream("entity-config.xsd"));
        }
        catch (Exception e)
        {
          throw new ConfigException(e);
        }
        
        notifyChange();
    }
    
    public void startElement (String uri, String local, String qName, Attributes atts) 
    throws SAXException     
    {

      if (!components)
      {
          if (qName.equals("entity"))
          {
                name= atts.getValue("name");
                logger.debug("startElement() name:"+name);
                cacheMode= ("nocache".equals(atts.getValue("cache")) ? 1 : 0 );
                    
                if (atts.getValue("extends")!=null)
                {
                    superEntity= configSet.getEntity(atts.getValue("extends"));
                    EntityConfig current= superEntity;
                    while (current!= null)
                    {
                        if (current.getName().equals(name))
                          throw new RuntimeException("Cyclic inheritance found between '"+name+"' and '"+superEntity.getName()+"'");
                        
                        current= current.getSuperEntity();
                    }
                    superEntity.addDependent(this); // EntityConfigImpl non finalizer altrimenti se un anchestor in cache allora anche tutti i figli (che e' male)
                }
          }
          else
          if (qName.equals("components")) components= true;
          else
            super.startElement(uri,local,qName,atts);
      }
      else 
      {
            if (qName.equals("component"))
            {
               String name= atts.getValue("name");
               
                /* maintain pointers to existent components
                 * for update, or add new component
                 */
               if ((cc=(ComponentConfigImpl)componentsMap.get(name))==null)
               {
                 cc= new ComponentConfigImpl(name,this);
                 componentsNames.add(name);
               }
               else
                 cc.reset();
                 
               save=true;            
            }      
            cc.startElement(uri,local,qName,atts);
      }  
      
    }

    public void endElement(String uri, String localName, String qName)
        throws SAXException 
    {
      if (components)
      { 
         if (qName.equals("component"))
         {
           if (!componentsMap.containsKey(cc.getName()))
           {
             componentsMap.put(cc.getName(),cc);
             logger.debug("endElement() added component: "+cc.getName()+" to: "+this.name);
           }
           else
             logger.debug("endElement() updated component: "+cc.getName()+" for: "+this.name);
          
           save=false;
           cc= null;
           return;
         }
         else
         if (qName.equals("components"))
         {
            components= false;
            return;
         }
         cc.endElement(uri,localName,qName);
      }
      else
        super.endElement(uri,localName,qName);

      
      value="";
      if (!components) save= false;
      key= null;
    }
    
    public void characters (char ch[], int start, int length)
    {
       if (components&&save) cc.characters(ch,start,length);
       else
        super.characters(ch,start,length);
    }

    public void addDependent(EntityConfig ec)
    {
        if (!dependentEntityConfigs.contains(ec))
            dependentEntityConfigs.add(ec);
    }
    
    public ArrayList getDependents()
    {
        return (ArrayList)dependentEntityConfigs.clone();
    }
    
    public ArrayList getComponents()
    {
      RWLock.Lock l= rwlock.getReadLock();
      ArrayList res= (ArrayList) componentsNames.clone();
      rwlock.releaseLock(l);
      
      if (superEntity!=null)
          res.addAll(superEntity.getComponents());
      
      return res;
    }
    
    public ComponentConfig getComponent(String name)
    {
      RWLock.Lock l= rwlock.getReadLock();
      ComponentConfig cc;

        if ((cc=(ComponentConfig)componentsMap.get(name))==null)
        try
        {
           cc= new ComponentConfigImpl(xmlSource.getInputStream(),name,this);
           componentsMap.put(cc.getName(),cc);
        }
        catch (Exception e)
        {
          if (superEntity==null)
          {
            logger.warn("EntityConfig getComponent() component not found:"+name+" in: "+getName());
          }
          else
            cc= superEntity.getComponent(name);
        }

      rwlock.releaseLock(l);

      return cc;
    }
    
    public Date getLastChangeDate()
    {
      return lastChanged;
    }
    
    protected void resetLastChangeDate()
    {
      lastChanged= null;
    }
    
    public ConfigSource getConfigSource()
    {
      return xmlSource;
    }
    
    public void notifyChange()
    throws ConfigException
    {
        logger.debug("notify change called for entity: "+getName());
        InputStream in= xmlSource.getInputStream();

        if (in==null)
         throw new ConfigException("Null inputStream");

        RWLock.Lock l= rwlock.getWriteLock();
        
        logger.debug("write lock acquired for entity: "+getName());
        
        reset();         
               
        //parser.setDTDContentModelSource(new XMLDTDContentModelSource());

        try
        {
           parser.parse(in,this);
           lastChanged= xmlSource.getLastChangeDate();
           logger.debug("applied changes for entity: "+getName()+"!");
           
           Iterator deps= dependentEntityConfigs.iterator();
           
           while (deps.hasNext())
           try
           {
               ((EntityConfig)deps.next()).notifyChange();
           }
           catch (Exception ex)
           {
               logger.error("notifyChange() dependecy loop",ex);
           }
        }
        catch (SAXParseException pex)
        {
           throw new ConfigException(pex);
        }
        catch (SAXNotRecognizedException x)
        {
           throw new ConfigException("Your SAX parser is not JAXP 1.2 compliant.",x);
        }
        catch (SAXException e) 
        {
           if (!e.getMessage().equals("stopParse"))
           {
             throw new ConfigException("EntityConfig() error reading file",e);
           }
        } 
        catch(Exception e)
        {
           throw new ConfigException("parsing config... ",e);
        }
        finally
        {
            rwlock.releaseLock(l);
            logger.debug("released write lock for entity: "+getName());
        }
    }
    
   public void error(SAXParseException x)
      throws SAXParseException   
   {
      logger.error(x);
      throw x;
   }

   public void fatalError(SAXParseException x)
      throws SAXParseException
   {
      logger.fatal(x);
      throw x;
   }
   
    protected void reset()
    {
        sqlStatements= new Hashtable();
        parameters= new Hashtable();
        save=false;
        parse=false;
        components=false;
        cc= null;
        key= null;
        value= null;
    }
   
    protected EntityConfig getEntity()
    {
        return this;
    }
    
    protected BaseConfig getSuper()
    {
        return superEntity;    
    }
    
	public EntityConfig getSuperEntity()
	{
		return superEntity;
	}

	public Config getConfigSet()
    {
        return configSet;
    }
    
    public EntityConfig getParent()
    {
        return configSet.getEntity(name.substring(0,name.lastIndexOf(".")));
    }
    
    public int getCacheMode()
    {
        return cacheMode; 
    }

    public String translate(String value)
    {
        String[] pieces= StringHandler.split(value,"/");
        
        if (pieces.length<2)
            throw new RuntimeException("Invalid config expression: '"+value+"'");
        
        if (pieces[0].equals("component"))
        {
            ComponentConfig cc= (ComponentConfig)componentsMap.get(pieces[1]);
            
            if (cc==null)
                throw new RuntimeException("Component: '"+pieces[1]+"' is not already loaded or does not exists.");    
            
            return cc.translate(value);
        }
        else
        if (pieces[0].equals("parameter")||pieces[0].equals("statement"))
        {
            return getValue(this,pieces,0);
        }
        else
            throw new RuntimeException("Invalid config expression: '"+pieces[0]+"' must be replaced with one of [component|parameter|statement]");
        
    }
    
    boolean components=false;    
    boolean save=false;
    boolean cached= false;
    ComponentConfigImpl cc;
    String key;
    String value="";
}
