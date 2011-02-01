package com.google.code.eforceconfig;
import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Logger;

public class SQLBindVariable 
{
  private static Logger logger= Logger.getLogger(SQLBindVariable.class);
  private String name;
  private Vector parPositions;
  
  public SQLBindVariable(String name)
  {
    this.name= name;
    this.parPositions= new Vector();
  }


  public void setName(String name)
  {
    this.name = name;
  }


  public String getName()
  {
    return name;
  }
  
  public Iterator getPositionIterator()
  {
    return parPositions.iterator();
  }

  public void addPosition(Integer i)
  {
    logger.debug("adding position: "+i+" to bind variable:"+name);
    parPositions.add(i);
    logger.debug("parPositions:"+parPositions.size());
  }

}
