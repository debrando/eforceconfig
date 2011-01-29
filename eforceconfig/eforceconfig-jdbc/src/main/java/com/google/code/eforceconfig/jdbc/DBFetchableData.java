package com.google.code.eforceconfig.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import java.sql.Date;

/**
 * 
 * Easy to use java.sql.ResultSet wrapper 
 *
 */
public class DBFetchableData
{
  public static int FETCH_FORWARD= ResultSet.FETCH_FORWARD;
  public static int FETCH_REVERSE= ResultSet.FETCH_REVERSE;
  public static int FETCH_UNKNOWN= ResultSet.FETCH_UNKNOWN;
  
  private static Logger logger= Logger.getLogger(DBFetchableData.class);
  private int cols;
  private String[] aliases;
  private ResultSet rs;
  private Exception exception;
  
  public DBFetchableData(String[] aliases, ResultSet rs, int cols)
  {
    this.cols= cols;
    this.aliases= aliases;
    this.rs= rs;
  }

  public String[] getAliases()
  {
   return aliases;
  }

  public int getCols()
  {
   return cols;
  }

  public boolean next()
  throws SQLException
  {
      return rs.next();
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

  public Object getObject(String colName)
  throws Exception
  {
    return getObject(getIndexFor(colName));
  }
  
  public Object getObject(int index)
  throws SQLException
  {
    return rs.getObject(index+1);
  }
  
  public String getString(String colName)
  throws Exception
  {
    return getString(getIndexFor(colName));
  }

  public String getString(int index)
  throws SQLException
  {
    return rs.getString(index+1);
  }
  
  public Date getDate(String colName)
  throws Exception
  {
    return getDate(getIndexFor(colName));
  }

  public Date getDate(int index)
  throws SQLException
  {
    return rs.getDate(index+1);
  }

  public double getDouble(String colName)
  throws Exception
  {
    return getDouble(getIndexFor(colName));
  }

  public double getDouble(int index)
  throws SQLException
  {
    return rs.getDouble(index+1);
  }

  public int getInt(String colName)
  throws Exception
  {
    return getInt(getIndexFor(colName));
  }

  public int getInt(int index)
  throws SQLException
  {
    return rs.getInt(index+1);
  }

  public byte[] getBytes(String colName)
  throws Exception
  {
    return getBytes(getIndexFor(colName));
  }

  public byte[] getBytes(int index)
  throws SQLException
  {
    return rs.getBytes(index+1);
  }

  public void release()
  {
     try
     {
      rs.getStatement().close();
      //rs.close();
     }
     catch (Exception e)
     {
       logger.error("release()",e);
     }
  }

  public int getColumnCount()
  throws SQLException
  {
    return rs.getMetaData().getColumnCount();
  }

  /**
   * usare DBStatement.setFetchSize
   * @deprecated
   * 
   */
  public void setFetchSize(int rows)
  throws SQLException
  {
      rs.setFetchSize(rows);
  }

  public void setFetchDirection(int dir)
  throws SQLException
  {
      rs.setFetchDirection(dir);
  }

  public int getFetchDirection()
  throws SQLException
  {
      return rs.getFetchDirection();
  }  

  public void absolute(int row)
  throws SQLException
  {
      rs.absolute(row);
  }

  public void relative(int rel)
  throws SQLException
  {
      rs.relative(rel);
  }  

  public void first()
  throws SQLException
  {
      rs.first();
  }  

  public boolean isFirst()
  throws SQLException
  {
      return rs.isFirst();
  }  

  public int getRow()
  throws SQLException
  {
      return rs.getRow();
  }    

  public void last()
  throws SQLException
  {
      rs.last();
  }    

  public int getRecordCount()
  throws SQLException
  {
       rs.setFetchDirection(rs.FETCH_REVERSE);
       rs.first();
       return rs.getRow();
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
