package com.google.code.eforceconfig;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

public interface BaseConfig extends ExpressionHandler
{
    
    public String getName();
    
    public EntityConfig getParent();

    public String getSQLstmt(String name);

    public SQLStatement getSQLStatement(String name);

    public Hashtable getSQLstmts();
    
    public Hashtable getSQLStatements();
    
    public String getParameter(String name);

    public int getIntParameter(String name);

    public double getDoubleParameter(String name);
    
    public boolean getBooleanParameter(String name);

    public boolean getBooleanParameter(String name, boolean nvl);

    public Class getClassParameter(String name);

    public Object getClassInstanceParameter(String name);

    public ArrayList getListParameter(String name);

    public Hashtable getTableParameter(String name);

    public Properties getPropertiesParameter(String name);

    public Hashtable getParameters();
    
    public void setWarnNotFound(boolean warnNotFound);

}
