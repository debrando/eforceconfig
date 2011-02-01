package com.google.code.eforceconfig.initializers;
import com.google.code.eforceconfig.ConfigSource;
import com.google.code.eforceconfig.ConfigSourceManager;
import com.google.code.eforceconfig.sources.managers.ClassPathSourceManager;

public class ClassPathConfigInitializer extends BaseConfigInitializer
{
  private ConfigSourceManager csm;
  
  public ClassPathConfigInitializer()
  {}

  public ClassPathConfigInitializer(String configRoot)
  {
      this(Thread.currentThread().getContextClassLoader(),configRoot);
  }

  public ClassPathConfigInitializer(ClassLoader classLoader, String configRoot)
  {
      csm= new ClassPathSourceManager(classLoader,configRoot);
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
