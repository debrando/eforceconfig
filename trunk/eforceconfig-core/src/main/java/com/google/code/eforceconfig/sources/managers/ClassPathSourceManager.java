package com.google.code.eforceconfig.sources.managers;
import com.google.code.eforceconfig.util.StringHandler;
import com.google.code.eforceconfig.ConfigSource;
import com.google.code.eforceconfig.ConfigSourceManager;
import com.google.code.eforceconfig.sources.ClassPathConfigSource;
import java.io.File;

import org.apache.log4j.Logger;

public class ClassPathSourceManager implements ConfigSourceManager
{
  private static Logger logger= Logger.getLogger(ClassPathSourceManager.class);  
  private String configPath;
  private ClassLoader classLoader;

  public ClassPathSourceManager(ClassLoader classLoader, String configPath)
  {
    this.classLoader= classLoader;
    this.configPath= configPath;
  }

  public ConfigSource getEntitySource(String entityName)
  {
     String confPath = StringHandler.replace(configPath,".","/"); 
     String ep= StringHandler.replace(entityName,".","/");
     return new ClassPathConfigSource(classLoader, confPath+"/"+ep);     
  }

  public boolean supportsChangeControl()
  {
    return false;
  }
}
