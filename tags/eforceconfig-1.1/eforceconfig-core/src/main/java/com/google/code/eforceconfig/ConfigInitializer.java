package com.google.code.eforceconfig;

public interface ConfigInitializer 
{
  public ConfigSource[] getConfigSources();

  public ConfigSourceManager getConfigSourceManager();

  public int getCacheSize();

  public String getJAXPFactoryClassName();

  public boolean getStartChangeControl();
  
  public int getChangeControlInterval();

  public boolean getPreloadCacheOnInit();

  public boolean getStartGarbageCollector();
  
  public int getGarbageCollectorInterval();
}
