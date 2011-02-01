package com.google.code.eforceconfig.initializers;

import com.google.code.eforceconfig.ConfigSourceManager;
import com.google.code.eforceconfig.ConfigSource;
import com.google.code.eforceconfig.sources.managers.FileSourceManager;

public class FileConfigInitializer extends BaseConfigInitializer
{
  private ConfigSourceManager csm;

  public FileConfigInitializer()
  {}
  
  public FileConfigInitializer(String configPath)
  {
      csm= new FileSourceManager(configPath);
  }

  public ConfigSource[] getConfigSources()
  {
    return new ConfigSource[0];
  }
  
  public ConfigSourceManager getConfigSourceManager()
  {
    return csm;
  }

  public void setConfigSourceManager(ConfigSourceManager sourceManager)
  {
    csm= sourceManager;
  }
  
}
