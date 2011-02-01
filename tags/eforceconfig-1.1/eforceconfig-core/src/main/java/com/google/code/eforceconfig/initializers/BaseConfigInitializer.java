package com.google.code.eforceconfig.initializers;
import com.google.code.eforceconfig.ConfigInitializer;

public abstract class BaseConfigInitializer implements ConfigInitializer
{

  private String JAXPFactoryClassName= null;
  private boolean startChangeControl= false;
  private boolean preloadCacheOnInit= false;
  private boolean startGarbageCollector= true;
  private int changeControlInterval= 10000;
  private int garbageCollectorInterval= 10000;
  private int cacheSize= 20;

  public void setJAXPFactoryClassName(String jaxpFactoryClassName)
  {
    this.JAXPFactoryClassName = jaxpFactoryClassName;
  }

  public String getJAXPFactoryClassName()
  {
    return JAXPFactoryClassName;
  }

  public void setStartChangeControl(boolean haveToStart)
  {
    startChangeControl= haveToStart;
  }

  public boolean getStartChangeControl()
  {
    return startChangeControl;
  }
  
  public void setChangeControlInterval(int interval)
  {
      changeControlInterval= interval;
  }

  public int getChangeControlInterval()
  {
    return changeControlInterval;
  }

  public int getCacheSize()
  {
    return cacheSize;
  }

  public void setCacheSize(int size)
  {
    cacheSize= size;
  }
  
  public void setPreloadCacheOnInit(boolean haveToPreload)
  {
    preloadCacheOnInit= haveToPreload;
  }

  public boolean getPreloadCacheOnInit()
  {
    return preloadCacheOnInit;
  }

  public void setStartGarbageCollector(boolean start)
  {
    startGarbageCollector= start;
  }

  public boolean getStartGarbageCollector()
  {
    return startGarbageCollector;
  }
  
  public void setGarbageCollectorInterval(int interval)
  {
    garbageCollectorInterval= interval;
  }

  public int getGarbageCollectorInterval()
  {
    return garbageCollectorInterval;
  }

}
