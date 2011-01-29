package com.google.code.eforceconfig;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import java.util.Hashtable;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;

import com.google.code.eforceconfig.util.StringHandler;

public class ComponentConfigImpl extends BaseConfigImpl implements ComponentConfig
{   
    private static Logger logger= Logger.getLogger(ComponentConfigImpl.class);
    private EntityConfigImpl parent;    

    protected ComponentConfigImpl(String name, EntityConfigImpl parent) 
    {
        logger.debug("ComponentConfig() name:"+name);
        this.name= name;
        this.parent= parent;
        rwlock= parent.rwlock;
    }
    
    protected ComponentConfigImpl (InputStream xmlFile, String name, EntityConfigImpl parent)
    throws ConfigException 
    {
        this.name= name;
        this.parent= parent;
        rwlock= parent.rwlock;
        
        SAXParserFactory saxfactory = SAXParserFactory.newInstance();
        saxfactory.setNamespaceAware(true);
        saxfactory.setValidating(true);
        SAXParser parser;
        
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

        try 
        {
            parser.parse(xmlFile, this);
        } 
        catch (SAXException e) 
        {
            if (!e.getMessage().equals("stopParse")) System.err.println (e);
        } 
        catch (IOException e) 
        {
            System.err.println (e);
        }
        
        if (!parsed)
            throw new RuntimeException("Component not found");
    }
    
    public void startElement (String uri, String local, String qName, Attributes atts) 
        throws SAXException     
    {
      if (parse)
        super.startElement(uri,local,qName,atts);
      else
        if (qName.equals("component")&&name.equals(atts.getValue("name"))) parse=true;
    }

    public void endElement(String uri, String localName, String qName)
        throws SAXException 
    {
      if (parse)
      {
        if (qName.equals("component"))
        {  
          parse= false;
          parsed= true;
          throw new SAXException("stopParse");
        }
        else
          super.endElement(uri,localName,qName);
        
        value="";
        save= false;
        key= null;
      }
    }

    public EntityConfig getParent()
    {
       return parent;
    }
    
    protected void reset()
    {
        sqlStatements= new Hashtable();
        parameters= new Hashtable();
        save=false;
        parse=false; 
        key= null;
        value= null;
    }
    
    protected EntityConfig getEntity()
    {
        return getParent();
    }    

    /* TODO: may be implemented to allow component inheritance
     */
    protected BaseConfig getSuper()
    {
        return null;
    }    

    public String translate(String value)
    {
        // TODO Auto-generated method stub
        return getValue(this,StringHandler.split(value,"/"),2);
    }

    boolean save=false;
    boolean parse=false;
    boolean parsed=false;  
    boolean cached= false;
    String key;
    String value="";
}
