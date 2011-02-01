package com.google.code.eforceconfig.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.oro.util.CacheLRU;

/**
 * Easy to use java.sql.Connection wrapper
 * 
 * Connection conn= <get the conn>
 * 
 * DBConnection dbconn= new DBConnection(conn);
 * EntityConfig entity= config.getEntity("customer");
 * 
 * DBStatement dml= new DBStatement(entity.getSQLStatement("MY_CUSTOMER_INSERT_STATEMENT"));
 * dml.bindVariable("fname","Andrea");
 * dml.bindVariable("lname","Gariboldi");
 * 
 * dbconn.executeDML(dml);
 * 
 * dbconn.commit();
 */
public class DBConnection 
{
  private static Logger logger= Logger.getLogger(DBConnection.class);
  
  private Connection oc;
  private boolean debug= false;
  private int maxstmtbc=1000;
  private int execstmt=0;
  private static final int mdCacheSize= 100;
  private static CacheLRU mdCache;
  
  public DBConnection(Connection conn)
  {    
   try
   {
     oc=  conn;
     if (mdCache==null)
         mdCache= new CacheLRU(mdCacheSize);
     debug= logger.isInfoEnabled();
   }
   catch (Exception e)
   {
    logger.error("",e);
   }
  }

  public void setMaxStmtBeforeCommit(int no)
  {
    this.maxstmtbc= no;
  }

  public void setDebug(boolean debug)
  {
    this.debug= debug;
  }

  public void close()
  {
       try
       {
         if (debug) logger.info("DBConnection close()");
         oc.close();
       }
       catch (Exception e)
       {}
  }

  public Connection getConnection()
  {
    return oc;
  }
   
  public DBData getData(DBStatement query)
  throws Exception
  {
    return _getData(query, oc, debug);
  }
  
  public DBFetchableData getFetchableData(DBStatement query)
  throws Exception
  {
     return _getFetchableData(query,oc,debug);
  }

  public void executeDMLC(DBStatement dml)
  throws SQLException
  {
     
     executeDML(dml);

     if (++execstmt%maxstmtbc==0) 
     {
        commit();
        logger.warn("Commit point reached: "+execstmt);
     }
  }

  public void executeDML(DBStatement dml)
  throws SQLException
  {
         SQLException ex= null;
         if (debug) logger.info("DBConnection executeDML(): dml: "+dml.getStatement());
     
         PreparedStatement stmt= oc.prepareStatement(dml.getStatement());
         for (int i=0;i<dml.getParSize();i++)
         {
            Object o= dml.getParameter(i); if (o==null) o=""; // aggiunto per driver ODBC
            if (debug) logger.info("DBConnection executeDML(): dml.getParameter("+i+"): "+o);
            stmt.setObject(i+1,o); 
         }
         
         try
         {
          stmt.execute();
          dml.setUpdateCount(stmt.getUpdateCount());
         }
         catch (SQLException e)
         {
           if (debug) logger.info("DBConnection executeDML():  "+e);
           stmt.close();
           ex=e;
         }
         
         
         stmt.close();
         if (ex!=null) throw ex;         
  }

  public void executeDDL(String ddl)
  throws SQLException
  {
         SQLException ex= null;
         if (debug) logger.info("DBConnection executeDDL(): ddl: "+ddl);
     
         PreparedStatement stmt= oc.prepareStatement(ddl);
         
         try
         {
          stmt.execute();
         }
         catch (SQLException e)
         {
           if (debug) logger.info("DBConnection executeDDL():  "+e);
           stmt.close();
           ex=e;
         }
         
         stmt.close();
         if (ex!=null) throw ex;         
  }
  
  public int getNextVal(String sequenceName)
  {
     Statement stmt=null;
     ResultSet rs=null;
  
    try
    {
            stmt= oc.createStatement();
            rs= stmt.executeQuery("SELECT "+sequenceName+".NEXTVAL FROM DUAL");
            while (rs.next())
            {
              int next_val= rs.getInt(1);
              rs.close();
              stmt.close();
              return next_val;
            }
    }
    catch(Throwable e)
    {
      logger.error("getNextVal("+sequenceName+")",e);
    }
    
    try
    {
    rs.close();
    stmt.close();
    }
    catch(Throwable e)
    {}
    
    return -1;
  }
  
  public long getLongNextVal(String sequenceName)
  {
     Statement stmt=null;
     ResultSet rs=null;
  
    try
    {
            stmt= oc.createStatement();
            rs= stmt.executeQuery("SELECT "+sequenceName+".NEXTVAL FROM DUAL");
            while (rs.next())
            {
              long next_val= rs.getLong(1);
              rs.close();
              stmt.close();
              return next_val;
            }
    }
    catch(Throwable e)
    {
      logger.error("getNextVal("+sequenceName+")",e);
    }
    
    try
    {
    rs.close();
    stmt.close();
    }
    catch(Throwable e)
    {}
    
    return -1;
  }  
  
  public void commit()
  throws SQLException
  {
        oc.commit();
  }

  public void rollback()
  throws SQLException  
  {
        oc.rollback();
  }

  protected void finalize()
  throws Throwable
  {
     if (oc!=null&&!oc.isClosed())
      if (debug) logger.info("finalize() found open connection");
     
     close();
     
     super.finalize();
  }

  private DBData _getData(DBStatement query, Connection conn, boolean debug)
  throws Exception
  {

     if (debug)
     { 
       logger.info("getData() Query: "+query.getStatement());
       for (int i=0;i<query.getParSize();i++)
          logger.info("getData() pars["+i+"]: "+query.getParameter(i));   
     }
     
     String[][]  retData= new String[0][0];
     String[]    aliases= new String[0];
     ArrayList[] arrData= new ArrayList[]{new ArrayList()};
   
     PreparedStatement stmt=null;
     ResultSet rs=null;
     MetaData md=null;
     
     int cols=0,len=0;
   
     try
     {
          stmt= conn.prepareStatement(query.getStatement(),ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);

          if (query.getFetchSize()>0)
              stmt.setFetchSize(query.getFetchSize());

          Object o; // aggiunto per driver ODBC
          for (int i=0;i<query.getParSize();i++)
            if ((o=query.getParameter(i))==null) // aggiunto per driver ODBC
               stmt.setObject(i+1,"");
            else
               stmt.setObject(i+1,o);
             
          rs= stmt.executeQuery();

          md= cacheMetaData(query.getStatement(),rs);
              
          cols= md.getColumnCount();
          aliases= new String[cols];
          arrData= new ArrayList[cols];
            
          for (int i=0;i<cols;i++)
          {
           aliases[i]= md.getColumnName(i);
           arrData[i]= new ArrayList();
          }
              
          String dat;
          while (rs.next()) 
            for (int i=0;i<cols;i++)
               arrData[i].add(((dat= rs.getString(i+1))==null)? "" : dat);

                
          rs.close();
          stmt.close();
            
          if (arrData[0].isEmpty()) throw new RuntimeException("no_data_found");
            
          retData= new String[cols][arrData[0].size()];
          len= arrData[0].size();
            
          for (int i=0;i<len;i++)
            for (int j=0;j<cols;j++)
               retData[j][i]= (String) arrData[j].get(i);
            
     }
     catch (RuntimeException e)
     {
         if (e.getMessage().equals("no_data_found"))
         {
             retData= new String[cols][0];
             if (debug) logger.info("no_data_found");             
         }
         else
          throw e;
     }
     catch (Exception e)
     {
         throw e;
     }
     finally
     {
         try
         {
            rs.close();
            stmt.close();
         }
         catch(Throwable e)
         {}
     }
     return new DBData(aliases,retData,len,cols);
  }
  
  public MetaData cacheMetaData(String stmt, ResultSet rs)
  throws SQLException
  {
    MetaData md;
    
    if ((md=(MetaData)mdCache.getElement(stmt))==null)
    {
       md= new MetaData(rs.getMetaData());
       mdCache.addElement(stmt,md);
    }

    return md;
  }
  
  private DBFetchableData _getFetchableData(DBStatement query, Connection conn, boolean debug)
  throws Exception
  {

     if (debug)
     { 
       logger.info("getData() Query: "+query.getStatement());
       for (int i=0;i<query.getParSize();i++)
          logger.info("getData() pars["+i+"]: "+query.getParameter(i));   
     }

     String[] aliases= new String[0];
     PreparedStatement stmt=null;
     ResultSet rs=null;
     MetaData md=null;

     int cols=0,len=0;
   
      stmt= conn.prepareStatement(query.getStatement(),ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
 
      if (query.getFetchSize()>0)
       stmt.setFetchSize(query.getFetchSize());

      for (int i=0;i<query.getParSize();i++)
         stmt.setObject(i+1,query.getParameter(i));  
      
      rs= stmt.executeQuery();
      
      md= cacheMetaData(query.getStatement(),rs);
      
      cols= md.getColumnCount();
      aliases= new String[cols];
    
      for (int i=0;i<cols;i++)
       aliases[i]= md.getColumnName(i);
            
      return new DBFetchableData(aliases,rs,cols);
  }
  
  private static class MetaData 
  {
    private int colsCount;
    private String[] cols;
    
    public MetaData(ResultSetMetaData md)
    throws SQLException
    {
       this.colsCount= md.getColumnCount();
       this.cols= new String[colsCount];
       
       for (int i=0;i<colsCount;i++)
         cols[i]= md.getColumnName(i+1);
       
    }

    public int getColumnCount()
    {
      return colsCount;
    }

    public String getColumnName(int i)
    {
      return cols[i];
    }
  }
  
}
