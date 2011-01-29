package com.google.code.eforceconfig.jdbc;

/**
 *
 * Static resultset container
 *
 */
public class DBData
{
  private String[][] data;

  private String[] aliases;

  private int size;

  private int cols;  
  
  private Exception exception;

  /**
   * Constructor
   */
  public DBData(String[] al, String[][] dt,int sz,int cs)
  {
   data= dt;
   aliases= al;
   size= sz;
   cols= cs;
  }

  public String[] getAliases()
  {
   return aliases;
  }
  
  public int getIndexFor(String colName)
  throws Exception
  {
    int res=0;
    boolean found= false;
    
    for (;res<aliases.length;res++)
     if (aliases[res].toUpperCase().equals(colName.toUpperCase()))
     {
       found= true;
       break;
     }
     
    if (!found) throw new RuntimeException("invalid column name");
    
    return res;
  }

  public String[][] getData()
  {
   return data;
  }

  public String getFirst()
  {
    try
    {
      return data[0][0];
    }
    catch (Exception e)
    {}
    return null;
  }
  
  public double getDouble()
  {
    try
    {
      return Double.parseDouble(data[0][0]);
    }
    catch (Exception e)
    {}
    return Double.parseDouble("0.00");
  }
  
  public String getString()
  {
    try
    {
      return data[0][0];
    }
    catch (Exception e)
    {}
    return null;
  }  
  
  public String getString(String colName, int row)
  throws Exception
  {
    return getString(getIndexFor(colName), row);
  }
  
  public String getString(int col, int row)
  {
    return data[col][row];
  }

  public int getInt(String colName, int row, int nvl)
  throws Exception
  {
    return getInt(getIndexFor(colName), row, nvl);
  }

  public int getInt(int col, int row)
  {
      return getInt(col, row, 0);
  }

  public int getInt(int col, int row, int nvl)
  {
      return (data[col][row]==null||"".equals(data[col][row]))? nvl : Integer.parseInt(data[col][row]);
  }

  public double getDouble(String colName, int row, double nvl)
  throws Exception
  {
    return getDouble(getIndexFor(colName), row, nvl);
  }

  public double getDouble(int col, int row)
  {
      return getDouble(col, row, 0);
  }

  public double getDouble(int col, int row, double nvl)
  {
      return (data[col][row]==null||"".equals(data[col][row]))? nvl : Double.parseDouble(data[col][row]);
  }
  
  public String[] getStringColumn(String colName)
  throws Exception
  {
      return getStringColumn(getIndexFor(colName));
  }
  
  public String[] getStringColumn(int col)
  throws Exception
  {
      return data[col];
  }

  /**
   * returns the resultset line number
   */
  public int getSize()
  {
   return size;
  }

  /**
   * returns the resultset cols number
   */
  public int getCols()
  {
   return cols;
  }


  public Exception getException()
  {
    return exception;
  }


  public void setException(Exception exception)
  {
    this.exception = exception;
  }

}


