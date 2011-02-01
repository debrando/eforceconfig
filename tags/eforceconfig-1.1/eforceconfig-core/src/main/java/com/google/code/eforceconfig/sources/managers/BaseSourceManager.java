package com.google.code.eforceconfig.sources.managers;

import com.google.code.eforceconfig.util.StringHandler;
import java.io.File;

import com.google.code.eforceconfig.ConfigSource;
import com.google.code.eforceconfig.ConfigSourceManager;
import com.google.code.eforceconfig.sources.FileConfigSource;

public class BaseSourceManager implements ConfigSourceManager
{
  private String configPath;

  public BaseSourceManager(String configPath)
  {
    this.configPath= configPath;
  }

  public ConfigSource getEntitySource(String entityName)
  {
     String ep= StringHandler.replace(entityName,".",File.separator);
     return new FileConfigSource(configPath+File.separator+ep);
  }
  
  public boolean supportsChangeControl()
  {
    return true;
  }
  
}
