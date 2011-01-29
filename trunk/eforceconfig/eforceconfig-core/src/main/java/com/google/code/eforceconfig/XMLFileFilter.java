package com.google.code.eforceconfig;

import java.io.FilenameFilter;
import java.io.File;

public class XMLFileFilter implements FilenameFilter
{
  public boolean accept(File dir, String name)
  {
    if (name.toUpperCase().endsWith(".XML"))
      return true;
      
    return false;
  }
}
