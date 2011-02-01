package com.google.code.eforceconfig;

public class BindVariable
{
    private String name;
    private Object value;
    
    public BindVariable()
    {}

    public BindVariable(String name, Object value)
    {
        this.name= name;
        this.value= value;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name= name;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object value)
    {
        this.value= value;
    }
}
