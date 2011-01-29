package com.google.code.eforceconfig.exphandlers;

import com.google.code.eforceconfig.ExpressionHandler;

public class ConstantExpressionHandler implements ExpressionHandler
{
    private ClassLoader classLoader;
    
    public ConstantExpressionHandler()
    {
    }

    public ConstantExpressionHandler(ClassLoader classLoader)
    {
        this.classLoader= classLoader;
    }
    
    public String translate(String value)
    {
        String className= value.substring(0,value.lastIndexOf("."));
        String constantName= value.substring(value.lastIndexOf(".")+1);
        Object val= null;
        
        try
        {
          Class c= (classLoader==null ? Thread.currentThread().getContextClassLoader() : classLoader).loadClass(className);
          val= c.getField(constantName).get(c);
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
        
        return (val==null ? null : val.toString());
    }

}
