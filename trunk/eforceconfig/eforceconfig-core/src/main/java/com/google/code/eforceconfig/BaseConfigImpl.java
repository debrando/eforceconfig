package com.google.code.eforceconfig;

import com.google.code.eforceconfig.util.StringHandler;
import com.google.code.eforceconfig.util.concurrency.RWLock;
import com.google.code.eforceconfig.util.xml.ErrorPrinter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public abstract class BaseConfigImpl extends ErrorPrinter implements BaseConfig
{
  private static Logger logger= Logger.getLogger(BaseConfigImpl.class);;
  protected boolean warnNotFound= false; 
  protected Hashtable sqlStatements= new Hashtable();
  protected Hashtable parameters= new Hashtable();
  protected RWLock rwlock= new RWLock();
  
  protected String name;
  
  public void startElement (String uri, String local, String qName, Attributes atts)
   throws SAXException     
  {
  
      if (qName.equals("parameters")) params= true;
      else
      if (qName.equals("sql")) sql= true;
      else
      if (qName.equals("statement"))
      {
        if (params)
        {
          logger.error("Element 'statement' cannot be written between 'parameters'");
          throw new SAXException("Element 'statement' cannot be written between 'parameters'");
        }
        
        save= true;
        key= atts.getValue("name");
        
        try
        {
          cached= Boolean.valueOf(atts.getValue("cached")).booleanValue();
        }
        catch (Exception e)
        {
          cached= false;
        }

        try
        {
          bindingType= StringHandler.nvl(atts.getValue("binding-type"),SQLStatement.BASIC_TYPE);
        }
        catch (Exception e)
        {
          bindingType= SQLStatement.BASIC_TYPE;
        }

      }
      else
      if (qName.equals("parameter"))
      {
      
        if (sql)
        {
          logger.error("Element 'parameter' cannot be written between 'sql'");
          throw new SAXException("Element 'parameter' cannot be written between 'sql'");
        }
      
        key= atts.getValue("name");
        type= atts.getValue("type");
        
        if ((type==null||"string".equals(type)))
        {
          value= StringHandler.nvl(atts.getValue("value"),"");
          save= value.equals("");
        }
        else
        if ("list".equals(type)) list= new ConfigValueList();
        else
        if ("table".equals(type)) table= new ConfigValueTable();
        
      }
      else
      if (qName.equals("value"))
      {
        vname= atts.getValue("name");
        value= StringHandler.nvl(atts.getValue("value"),"");
        save= value.equals("");
      }
      else
      {
       logger.error("Unknown Element: "+qName);
       throw new SAXException("Unknown Element: "+qName);
      }
  }

  public void endElement(String uri, String localName, String qName)
      throws SAXException 
  {

      if (qName.equals("parameters")) params= false;
      else
      if (qName.equals("sql")) sql= false;
      else
      if (qName.equals("statement"))
      {
         sqlStatements.put(key,new SQLStatement(key,new ConfigValue(getEntity(),value),cached, bindingType));
      }
      else
      if (qName.equals("parameter"))
      {
        if (type==null||"string".equals(type)) parameters.put(key,new ConfigValue(getEntity(),value));
        else
        if ("list".equals(type)) parameters.put(key,list);
        else
        if ("table".equals(type)) parameters.put(key,table);
      }
      else
      if (qName.equals("value"))
      {
        if ("list".equals(type)) list.add(new ConfigValue(getEntity(),value));
        else
        if ("table".equals(type)) table.put(vname,new ConfigValue(getEntity(),value));
        vname= null;
      }
     
      value="";
      save= false;
  }
    
  public void characters (char ch[], int start, int length)
  {
     if (save)  value+= new String(ch,start,length);
  }

  public String getName()
  {
     return name;
  }    
  
  public String getSQLstmt(String name)
  {
    RWLock.Lock l= rwlock.getReadLock();
    String res= null;
    try
    {
      res= ((SQLStatement) sqlStatements.get(name)).getStmt();
      rwlock.releaseLock(l);
    }
    catch (NullPointerException e)
    { 
      rwlock.releaseLock(l);
      if (getSuper()==null)
      {
          if (warnNotFound) logger.warn("getSQLstmt() statement not found: "+name+" in container: "+this.getName());
      }
      else
          res= getSuper().getSQLstmt(name);
    }
    
    return res;
  }
  
  protected String getSQLstmtNoLock(String name)
  {
    String res= null;
    try
    {
      res= ((SQLStatement) sqlStatements.get(name)).getStmt();
    }
    catch (NullPointerException e)
    {
       if (getSuper()==null)
         throw new RuntimeException("getSQLstmt() statement not found: "+name+" in container: "+this.getName()+" (may be not already loaded)");
       else
         res= ((BaseConfigImpl)getSuper()).getSQLstmtNoLock(name);
    }
    return res;
  }  

  public SQLStatement getSQLStatement(String name)
  {
    RWLock.Lock l= rwlock.getReadLock();
    SQLStatement res= (SQLStatement) sqlStatements.get(name);
    rwlock.releaseLock(l);
    if (res==null) 
       if (getSuper()==null)
       {
            if (warnNotFound) logger.warn("getSQLStatement: "+name+" not found in container: "+this.getName());
       }
       else
         res= ((BaseConfigImpl)getSuper()).getSQLStatement(name);
       
    return res;
  }

  protected SQLStatement getSQLStatementNoLock(String name)
  {
    SQLStatement res= (SQLStatement) sqlStatements.get(name);

    if (res==null) 
        if (getSuper()==null)
            throw new RuntimeException("getSQLStatement: "+name+" not found in container: "+this.getName());
        else
            res= ((BaseConfigImpl)getSuper()).getSQLStatementNoLock(name);
    
    return res;
  }
  
  //backword compatibility
  public Hashtable getSQLstmts()
  {
    RWLock.Lock l= rwlock.getReadLock();
    Enumeration keys=  sqlStatements.keys();
    Hashtable stmts= new Hashtable();
      
    while (keys.hasMoreElements())
    {
      String key= (String) keys.nextElement();
      stmts.put(key,((SQLStatement)sqlStatements.get(key)).getStmt());
    }

    rwlock.releaseLock(l);
    
    if (getSuper()!=null)
        stmts.putAll(((BaseConfigImpl)getSuper()).getSQLstmts());
    
    return stmts;
  }

  public Hashtable getSQLStatements()
  {
    RWLock.Lock l= rwlock.getReadLock();
    Hashtable res= (Hashtable) sqlStatements.clone();
    rwlock.releaseLock(l);

    if (getSuper()!=null)
        res.putAll(((BaseConfigImpl)getSuper()).getSQLStatements());
    
    return res;
  }
  
  private String translateParameter(String name)
  {
     Object o= parameters.get(name);
     
     if (o==null)
      return null;
     else
      return o.toString();
  }
  
  public String getParameter(String name)
  {
    RWLock.Lock l= rwlock.getReadLock();
    String res= translateParameter(name);
    rwlock.releaseLock(l);
    
    if (res==null)
       if (getSuper()==null)
       {
           if (warnNotFound) logger.warn("getParameter: "+name+" not found in container: "+this.getName());
       }
       else
           res= ((BaseConfigImpl)getSuper()).getParameter(name);
    
    return res;
  }    

  protected String getParameterNoLock(String name)
  {
    String res= translateParameter(name);
    
    if (res==null)
      if (getSuper()==null)
        throw new RuntimeException("getParameter: "+name+" not found in container: "+this.getName()+" (may be not already loaded)");
      else
        res= ((BaseConfigImpl)getSuper()).getParameterNoLock(name);
        
    return res;
  }    

  public int getIntParameter(String name)
  {
    int res= -1;
    try
    {
      res= Integer.parseInt(getParameter(name));
    }
    catch (Exception e)
    {
        if (warnNotFound) logger.warn("getIntParameter: "+name+" in container: "+this.getName(),e);        
    }
    return res;
  }    

  public double getDoubleParameter(String name)
  {
    double res= -1.0;
    try
    {
      res= Double.parseDouble(getParameter(name));
    }
    catch (Exception e)
    {
        if (warnNotFound) logger.warn("getDoubleParameter: "+name+" in container: "+this.getName(),e);        
    }
    return res;
  }    

  public boolean getBooleanParameter(String name)
  {
    return getBooleanParameter(name,false);
  }
  
  public boolean getBooleanParameter(String name, boolean nvl)
  {
    boolean res= nvl;
    try
    {
      res= Boolean.valueOf(getParameter(name)).booleanValue();
    }
    catch (Exception e)
    {
        if (warnNotFound) logger.warn("getBooleanParameter: "+name+" in container: "+this.getName(),e);        
    }
    return res;
  }  
  
  public Class getClassParameter(String name)
  {
    Class res= null;
    try
    {
      res= Thread.currentThread().getContextClassLoader().loadClass(getParameter(name));
    }
    catch (Exception e)
    {
        if (warnNotFound) logger.warn("getClassParameter: "+name+" in container: "+this.getName(),e);        
    }
    return res;
  }   
  
  public Object getClassInstanceParameter(String name)
  {
    Object res= null;
    try
    {
      res= Thread.currentThread().getContextClassLoader().loadClass(getParameter(name)).newInstance();
    }
    catch (Exception e)
    {
        if (warnNotFound) logger.warn("getClassInstanceParameter: "+name+" in container: "+this.getName(),e);        
    }
    return res;
  }
  
  private ArrayList translateList(String name)
  {
      ArrayList al= null;
      Object o= parameters.get(name);
      
      if (o==null)
          return null;
      else
      if (o instanceof ConfigValueList)
      {
          al= new ArrayList(((ConfigValueList)o).size());
          Iterator i= ((ConfigValueList)o).iterator();
          
          while (i.hasNext())
              al.add(((ConfigValue)i.next()).toString());
          
          parameters.put(name, al);  // WARNING: writing with read lock ???!!??
      }
      else
      {
         al= (ArrayList)o;
      }
      
      return al;
  }

  public ArrayList getListParameter(String name)
  {
    RWLock.Lock l= rwlock.getReadLock();
    ArrayList res= translateList(name);
    rwlock.releaseLock(l);
    
    if (res==null)
    {
        if (getSuper()==null)
        {
            if (warnNotFound) logger.warn("getListParameter: "+name+" not found in container: "+this.getName());
        }
        else
            res= ((BaseConfigImpl)getSuper()).getListParameter(name);
    }
    else
      res= (ArrayList)res.clone();
    
    return res;
  }    

  protected ArrayList getListParameterNoLock(String name)
  {
    ArrayList res= translateList(name);
    
    if (res==null)
    {
       if (getSuper()==null)
         throw new RuntimeException("getListParameter: "+name+" not found in container: "+this.getName()+" (may be not already loaded)");
       else
         res= ((BaseConfigImpl)getSuper()).getListParameterNoLock(name);
    }
    else
       res= (ArrayList)res.clone();
    
    return res;
  }
  
  private Hashtable translateTable(String name)
  {
      Hashtable ht= null;
      Object o= parameters.get(name);
      
      if (o==null)
          return null;
      else
      if (o instanceof ConfigValueTable)
      {
          ht= new Hashtable(((ConfigValueTable)o).size());
          Iterator i= ((ConfigValueTable)o).entrySet().iterator();
          
          while (i.hasNext())
          {
              Map.Entry e= (Map.Entry)i.next();
              ht.put(e.getKey(),((ConfigValue)e.getValue()).toString());
          }
          
          parameters.put(name, ht);  // WARNING: writing with read lock ???!!??
      }
      else
      {
          ht= (Hashtable)o;
      }
      
      return ht;
  }
  
  public Hashtable getTableParameter(String name)
  {
    RWLock.Lock l= rwlock.getReadLock();
    Hashtable res= translateTable(name);
    rwlock.releaseLock(l);
    
    if (res==null)
    {
      if (getSuper()==null)
      {
          if (warnNotFound) logger.warn("getTableParameter: "+name+" not found in container: "+this.getName());
      }
      else
        res= ((BaseConfigImpl)getSuper()).getTableParameter(name);
    }
    else
       res= (Hashtable)res.clone();
    
    return res;
  }    

  protected Hashtable getTableParameterNoLock(String name)
  {
    Hashtable res= translateTable(name);
    
    if (res==null)
    {
      if (getSuper()==null)
        throw new RuntimeException("getTableParameter: "+name+" not found in container: "+this.getName()+" (may be not already loaded)");
      else
        res= ((BaseConfigImpl)getSuper()).getTableParameterNoLock(name);
    }
    else
      res= (Hashtable)res.clone();
    
    return res;
  }    

  public Properties getPropertiesParameter(String name)
  {
    RWLock.Lock l= rwlock.getReadLock();
    Properties res= null;
    
    try
    {
        Hashtable table= getTableParameterNoLock(name);
        res= new Properties();
        
        Iterator i= table.entrySet().iterator();
        
        while (i.hasNext())
        {
            Map.Entry e= (Map.Entry)i.next();
            res.setProperty((String)e.getKey(), (String)e.getValue());
        }
    }
    catch (Exception ex)
    {
        if (warnNotFound) logger.warn("getPropertiesParameter: "+name+" not found in container: "+this.getName());
    }
        
    rwlock.releaseLock(l);
    return res;
  }    

  public Hashtable getParameters()
  {
    RWLock.Lock l= rwlock.getReadLock();
    Hashtable res= (Hashtable) parameters.clone();
    rwlock.releaseLock(l);
    
    if (getSuper()!=null)
        res.putAll(((BaseConfigImpl)getSuper()).getParameters());
        
    return res;
  }
  
  public void setWarnNotFound(boolean warnNotFound)
  {
      this.warnNotFound= warnNotFound;
  }
  
  protected abstract EntityConfig getEntity();
  
  protected abstract BaseConfig getSuper();

  protected String getValue(BaseConfigImpl bc, String[] pieces, int base)
  {
      if (pieces[base].equals("parameter"))
      {
          if (pieces.length==base+2)
            return bc.getParameterNoLock(pieces[base+1]);
          else
          {
              try
              {
                  Hashtable ht= bc.getTableParameterNoLock(pieces[base+1]);
                  return (String)ht.get(pieces[base+2]);
              }
              catch (ClassCastException e)
              {
                  try
                  {
                     ArrayList al= bc.getListParameterNoLock(pieces[base+1]);
                     return (String)al.get(Integer.parseInt(pieces[base+2]));
                  }
                  catch (Exception ex)
                  {
                     throw new RuntimeException("Invalid configset expression: '"+pieces[base+2]+"' must be replaced with an integer or should be removed");   
                  }
              }
          } 
      }
      else
      if (pieces[base].equals("statement"))
      {
          /* NOTE: ritorno la stringa senza decode delle bindVariables sql, perche' lo statment verra' parsato insieme allo statement chiamante */
          return bc.getSQLStatementNoLock(pieces[base+1]).getStringValue();
      }
      else
        throw new RuntimeException("Invalid configset expression: '"+pieces[base]+"' must be replaced with one of [component|parameter|statement]");
      
  }

  boolean save=false;
  boolean parse=false;    
  boolean cached= false;
  boolean params= false;
  boolean sql= false;
  String key;
  String vname;
  String type;  
  String value="";
  String bindingType="";
  int parsno=0;
  ArrayList list;
  Hashtable table;
}
