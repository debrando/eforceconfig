// Copyright (c) 2000 E-Force
package com.google.code.eforceconfig.util;

import java.util.Iterator;
import java.util.StringTokenizer;

import java.util.Vector;
import org.apache.commons.lang.StringUtils;
import org.apache.regexp.RE;

public class StringHandler {

  public static final String CRLF= "\r\n";
    
  public static String getCRLFs(int no)
  {
    StringBuffer sb= new StringBuffer();
      for (int i=0;i<no;i++) sb.append(CRLF);
    return sb.toString();
  }

  public static String arrayToString(String[] arr)
  {
    String str="";
    
    for (int i=0;i<arr.length;i++) str+= i+arr[i];
    
    return str;
  }
  
  public static String[] split(String string, String sep)
  {
    String tmp_string= string;
    Vector v= new Vector();
    
    while (true)
    {
	     int splitidx= tmp_string.indexOf(sep);
       if (splitidx==-1) break;
       v.add(tmp_string.substring(0,splitidx));
	     tmp_string= tmp_string.substring(splitidx+sep.length());
    }

	  v.add(tmp_string);
    String[] res= new String[v.size()];
    Iterator i= v.iterator();
    int cnt=0;
    while (i.hasNext()) res[cnt++]= (String)i.next();
    
    return res;
  }

  public static String replace(String string, String tosearch, String toreplace)
  {
    return StringUtils.replace(string,tosearch,toreplace);
  }

  public static boolean match(String string, String tosearch)
  {
    try
    {
        return (new RE(tosearch)).match(string);
    }
    catch (Exception e)
    {}

    return false;
  }
  
  public static String nvl(String str,String nullrep)
  {
      return (str==null||str.equals("")) ? nullrep : str;
  }

  public static String initCap(String str)
  {
      char[] chars= str.toCharArray();
      char lastChar= ' '; 
      for (int i=0;i<chars.length;i++)
      {
        if (lastChar==' ') 
          chars[i]= Character.toUpperCase(chars[i]);
        else
          chars[i]= Character.toLowerCase(chars[i]);
          
        lastChar= chars[i];
      }
      
      return new String(chars);
  } 

  public static String getStringBetween(String str, String left, String right)
  {
     return getStringBetween(str,left,right,0);
  }

  public static String getStringBetween(String str, String left, String right, int occurence)
  {
     String res= null;
     int i=0;
     int o=-1;
     
     while (str.indexOf(left,i)!=-1 && ++o<occurence) 
       i= str.indexOf(right,str.indexOf(left,i)+left.length())+right.length();
     
     i= str.indexOf(left,i);
     
     if (o==occurence)
      res= str.substring(i+left.length(),str.indexOf(right,i+left.length()));
     
     return res; 
  }

  public static String[] getStringsBetween(String str, String left, String right)
  {
     String[] res= new String[StringUtils.countMatches(str,left)];
          
     for (int i=0;i<res.length;i++)
       res[i]= getStringBetween(str, left, right, i);
     
     return res; 
  }
  
  public static int countOccurrences(String str, String toSearch)
  {
    return StringUtils.countMatches(str,toSearch); 
  }
}
