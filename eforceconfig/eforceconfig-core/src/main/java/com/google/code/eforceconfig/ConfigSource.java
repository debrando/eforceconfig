package com.google.code.eforceconfig;

import java.io.InputStream;
import java.util.Date;

public interface ConfigSource 
{

   public InputStream getInputStream()
   throws ConfigException;

   public Date getLastChangeDate()
   throws ConfigException;

}
