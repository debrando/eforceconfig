package com.google.code.eforceconfig;

public class ConfigValue
{
    private EntityConfig ec;
    private String value;
    private String decodedValue= null;
    
    protected ConfigValue(EntityConfig ec, String value)
    {
        this.value= value;
        this.ec= ec; 
    }
    
    public String toString()
    {
        return getString();
    }

    public boolean equals(Object obj)
    {
        return getString().equals(obj);
    }

    public int hashCode()
    {
        return getString().hashCode();
    }
    
    private String getString()
    {
        if (decodedValue==null)
            decodedValue= Config.decode(ec, value);
            
        return decodedValue; 
    }

}
