package com.google.code.eforceconfig.jdbc;

import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;

import com.google.code.eforceconfig.SQLStatement;
import com.google.code.eforceconfig.BindVariable;

/**
 * Easy to use java.sql.Statement wrapper
 * that uses eforceconfig to bind variables
 */
public class DBStatement 
{
  private SQLStatement statement;
  private String stmt;
  private ArrayList pars;
  private int hash=0;
  private int updateCount=-1;
  private int addParameterIdx=0;
  private int fetchSize= -1;
  
  public DBStatement(String statement)
  {
    this.stmt= statement;
    pars= new ArrayList();
    updateHashCode();
  }
  
  public DBStatement(String statement, String[] pars)
  {
      if (statement==null||statement.equals("")) 
          throw new RuntimeException("Invalid empty statement string");
      
      this.stmt= statement;
      
      this.pars= new ArrayList(pars.length);
      
      for (int i=0;i<pars.length;i++) 
        this.pars.add(pars[i]);
      
      updateHashCode();
  }

  public DBStatement(SQLStatement st)
  {
    statement= st;
    pars= new ArrayList(st.getParsno());
    
    for (int i=0;i<st.getParsno();i++)
        pars.add(i, null);

    this.stmt= st.getStmt();
    updateHashCode();
  }
  
  public DBStatement(SQLStatement st, BindVariable[] bindVariables)
  throws Exception
  {
    statement= st;
    this.stmt= st.getStmt();
    pars= new ArrayList(st.getParsno());

    for (int i=0;i<st.getParsno();i++)
        pars.add(i, null);
    
    for (int i=0;i<bindVariables.length;i++)
       bindVariable(bindVariables[i]);
  }

  public void addParameter(Object par)
  {
    if (par==null) par= new String("");
    
    if (statement!=null) // se possibile binding-type="named"  
        pars.set(addParameterIdx++,par);
    else
        pars.add(par);
    
    updateHashCode();
  }

  public void addParameter(int par)
  {
    addParameter(new Integer(par));
  }

  public void addParameter(double par)
  {
    addParameter(new Double(par));
  }

  public void addParameterToInt(String par)
  {
    addParameter(new Integer(par));
  }

  public String getStatement()
  {
    return stmt;
  }

  public ArrayList getParameters()
  {
    return pars;  
  }

  public void setStatement(String statement)
  {
     setStatement(statement,true);
  }
  
  public void setStatement(String statement, boolean parse)
  {
    stmt= statement;
  }
  
  public int getParSize()
  {
    return pars.size();
  }

  public Object getParameter(int index)
  {
    return pars.get(index);
  }

  public boolean equals(Object obj)
  {
    try
    {
       if (this.pars.size()!=(((DBStatement)obj).pars.size())) return false;
       else
       if (!stmt.equals(((DBStatement)obj).stmt)) return false;
       else
       return this.pars.equals(((DBStatement)obj).pars);
    }
    catch(Exception e)
    {
       return false;
    }

  }

  private void updateHashCode()
  {
    hash=1;
    if (stmt!= null)
      hash= 31*hash+stmt.hashCode();
      
    if (pars!= null)
      hash= 31*hash+pars.hashCode();
  }
  
  public int hashCode()
  {
     return hash;
  }

  public void bindVariable(BindVariable bv)
  throws Exception
  {
      bindVariable(bv.getName(), bv.getValue());
  }
  
  private void setPar(int idx, Object val)
  {
     // System.err.println("setPar: "+idx+" v: "+val);
      int s= pars.size();
      
      if (idx>=s)
      {
        for (int i=s;i<idx;i++)
              pars.add(i, null);
        
        pars.add(idx, val);
      }
      else
        pars.set(idx, val);
  }
  
  public void bindVariable(String name, Object par)
  throws Exception
  {
     if (statement==null)
         throw new RuntimeException("cannot bind DBStatment variables if not initialized from SQLStatement");
     
     if (par==null) par= new String("");

     Iterator i= statement.getPositionIterator(name);
     while (i.hasNext())
         setPar(((Integer)i.next()).intValue(),par);
       
     updateHashCode(); 
  }

  public void bindVariable(String name, int par)
  throws Exception
  {
      bindVariable(name, new Integer(par));
  }

  public void bindVariable(String name, double par)
  throws Exception
  {
      bindVariable(name, new Double(par));
  }

  public void bindVariableToInt(String name, String par)
  throws Exception
  {
      bindVariable(name, new Integer(par));
  }
  
  public void bindVariablesMap(Map variableMap)
  throws Exception
  {
      bindVariables(variableMap);
  }
  
  public void bindVariables(Map variableMap)
  throws Exception
  {
     if (statement==null)
        throw new RuntimeException("cannot bind DBStatment variables if not initialized from SQLStatement");
      
     Iterator i= statement.iterateBindVariables();
     
     while (i.hasNext())
     {
        String name= (String)i.next();
        Object obj= variableMap.get(name);
        if (obj==null) continue;
        String value;
        
        if (obj instanceof String[])
            value= ((String[])obj)[0]; //HTTPServletRequest compatibility
        else
            value= obj.toString();
        
        Iterator p= statement.getPositionIterator(name);
        while (p.hasNext())
        {
          int pos= ((Integer)p.next()).intValue();
          setPar(pos,value);
        }
     }
  }

  public void clearParameters()
  {
      pars.clear();
      
      if (statement!=null)
      {
          addParameterIdx=0;
          for (int i=0;i<statement.getParsno();i++)
              pars.add(i, null);
      }
      
      updateHashCode();
  }

  protected void setUpdateCount(int updateCount)
  {
    this.updateCount = updateCount;
  }

  public int getUpdateCount()
  {
    return updateCount;
  }

  public int getFetchSize()
  {
    return fetchSize;
  }

  public void setFetchSize(int fetchSize)
  {
    this.fetchSize = fetchSize;
  }
}
