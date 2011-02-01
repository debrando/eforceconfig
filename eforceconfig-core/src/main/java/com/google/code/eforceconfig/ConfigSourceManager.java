package com.google.code.eforceconfig;

public interface ConfigSourceManager 
{
   public ConfigSource getEntitySource(String entityName)
   throws ConfigException;
   
   public boolean supportsChangeControl();
}
