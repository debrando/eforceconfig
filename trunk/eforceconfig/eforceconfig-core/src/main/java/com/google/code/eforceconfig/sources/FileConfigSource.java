package com.google.code.eforceconfig.sources;

import com.google.code.eforceconfig.ConfigNotFoundException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import com.google.code.eforceconfig.ConfigSource;

import org.apache.log4j.Logger;
import java.util.Date;
import com.google.code.eforceconfig.ConfigException;

public class FileConfigSource implements ConfigSource
{
  private static Logger logger= Logger.getLogger(FileConfigSource.class);
  private File file;
  
  public FileConfigSource(File file)
  {
    this.file= file;
    
    if (file.exists()&&file.isDirectory())
      this.file= new File(file,"entity.xml");
  }

  public FileConfigSource(String path)
  {
    this.file= new File(path+".xml");
    
    if (!file.exists())
      this.file= new File(path,"entity.xml");
  }

  public InputStream getInputStream()
  throws ConfigException
  {
   try
   {
     return new FileInputStream(file);
   }
   catch (FileNotFoundException e)
   {
     logger.debug("file not found: "+file.getAbsolutePath()+" (may be a dummy Config.getInstanceByUrl())");
     throw new ConfigNotFoundException(e);
   }
  }

  public Date getLastChangeDate()
  throws ConfigException
  {
     return new Date(file.getAbsoluteFile().lastModified());
  }
}
