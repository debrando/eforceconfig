package com.google.code.eforceconfig;
import com.google.code.eforceconfig.util.StringHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class SQLStatement
{
  public static final String BASIC_TYPE= "basic";
  public static final String NAMED_TYPE= "named";
  
  //updatable
  public static String expMarker= "$";
  
  private static Logger logger= Logger.getLogger(SQLStatement.class);
  private String name;
  private ConfigValue stmtVal;
  private String stmt;
  private boolean cached;
  private int parsno= 0;
  private String bindingType= BASIC_TYPE;
  private HashMap bindVariables;
  private ArrayList bindParameters;
  
  protected SQLStatement()
  {
  }

  protected SQLStatement(String name, ConfigValue stmt, boolean cached)
  {
    this.name= name;
    this.stmtVal= stmt;
    this.cached= cached;
  }

  protected SQLStatement(String name, ConfigValue stmt, boolean cached, String bindingType)
  {
    this.name= name;
    this.stmtVal= stmt;
    this.cached= cached;
    this.bindingType= bindingType;
  }

  private void parseCount(String even)
  {
    parsno+= StringHandler.countOccurrences(even,"?");
  }
  
  private void parseEven(String even)
  {
    String[] names= StringHandler.getStringsBetween(even,expMarker+"{","}");
    for (int i=0;i<names.length;i++)
    {     
      bindParameters.add(names[i].toLowerCase());
      SQLBindVariable sbv= (SQLBindVariable)bindVariables.get(names[i].toLowerCase());
      if (sbv==null)
      {
         sbv= new SQLBindVariable(names[i]);
         sbv.addPosition(new Integer(parsno));
         bindVariables.put(sbv.getName().toLowerCase(),sbv);
      }
      else
         sbv.addPosition(new Integer(parsno));
      
      parsno++;   
    }
    
  }
  
  private void translate()
  {
     Iterator i= bindVariables.values().iterator();
     while (i.hasNext())
       stmt= StringHandler.replace(stmt,expMarker+"{"+((SQLBindVariable)i.next()).getName()+"}","?");
  }
  
  private synchronized void parse()
  {
    if (stmt!=null) return;
    
    parsno=0;
    stmt= stmtVal.toString();
    
    if (stmt==null)
    {
       logger.error("null statement");
       return;
    }   
    String[] takeEven= StringHandler.split(stmt,"'");
    String[] even= new String[((int)(Math.floor(takeEven.length/2)))+1];
    
    for (int i=-1;++i<takeEven.length;i++)
       even[((int)(Math.ceil(i/2)))]= takeEven[i];
      
    if (BASIC_TYPE.equals(bindingType))    
      for (int i=0;i<even.length;i++)
         parseCount(even[i]);    
    else
    {
      bindVariables= new HashMap();
      bindParameters= new ArrayList();

      for (int i=0;i<even.length;i++)
         parseEven(even[i]); 
         
      translate();
    }
    
  }
  
  protected String getStringValue()
  {
    return stmtVal.toString();
  }

  public boolean isCached()
  {
    return cached;
  }

  public String getName()
  {
    return name;
  }

  public String getStmt()
  {
    parse();
    return stmt;
  }

  public String toString()
  {
    parse();
    return stmt;
  }

  public int getParsno()
  {
    parse();
    return parsno;
  }
  
  public Iterator getPositionIterator(String bindVarableName)
  throws ConfigException
  {
    parse();
      
    if (!NAMED_TYPE.equals(bindingType))
     throw new ConfigException("this statement has not 'named' binding type");
     
    SQLBindVariable sbv= (SQLBindVariable)bindVariables.get(bindVarableName.toLowerCase());

    if (sbv==null)
     throw new ConfigException("bind variable '"+bindVarableName+"' not found");
         return sbv.getPositionIterator();
  }
  
  public Iterator iterateBindVariables()
  throws ConfigException
  {
     parse();
      
     if (!NAMED_TYPE.equals(bindingType))
        throw new ConfigException("this statement has not 'named' binding type");
      
     return bindVariables.keySet().iterator();
  }

  public Iterator iterateBindParameters()
  {
     parse();
     
     return bindParameters.iterator();
  }

  public String getBindingType()
  {
    parse();
    
    return bindingType;
  }
}
