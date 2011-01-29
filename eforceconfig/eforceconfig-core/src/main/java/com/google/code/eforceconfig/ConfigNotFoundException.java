package com.google.code.eforceconfig;

public class ConfigNotFoundException extends ConfigException
{
  public ConfigNotFoundException()
  {
    super();
  }

  public ConfigNotFoundException(String message)
  {
    super(message);
  }

  public ConfigNotFoundException(String message, Throwable cause)
  {
    super(message,cause);
  }

  public ConfigNotFoundException(Throwable cause)
  {
    super(cause);
  }
}
