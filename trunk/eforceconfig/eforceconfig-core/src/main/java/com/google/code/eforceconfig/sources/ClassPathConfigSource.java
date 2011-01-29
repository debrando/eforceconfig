package com.google.code.eforceconfig.sources;
import com.google.code.eforceconfig.ConfigSource;
import java.io.InputStream;
import java.util.Date;

import org.apache.log4j.Logger;

public class ClassPathConfigSource implements ConfigSource
{
  private static Logger logger= Logger.getLogger(ClassPathConfigSource.class);
  private String config_path;
  private ClassLoader classLoader;
  
  public ClassPathConfigSource(ClassLoader classLoader, String path)
  {
      this.classLoader= classLoader;
      config_path = path;
  }

  public InputStream getInputStream()
  {
    InputStream ret= classLoader.getResourceAsStream(config_path+".xml");  
    
    if (ret==null)
    {
       ret= classLoader.getResourceAsStream(config_path+"/entity.xml");
       
       if (ret==null)
        logger.debug("classpath resource not found: "+config_path+" (may be a dummy Config.getInstanceByUrl())");
    }
    
    return ret;
  }

  public Date getLastChangeDate()
  {
      return null;
  }
  
}
