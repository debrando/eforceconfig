package com.google.code.eforceconfig;

import java.util.ArrayList;
import java.util.Date;


/**
 * TODO: solo metodi usati da classi client
 *
 */
public interface EntityConfig extends BaseConfig 
{
    public static int CACHE_MODE_CACHE= 0;
    public static int CACHE_MODE_NOCACHE= 1;

    public void addDependent(EntityConfig ec);
    
    public ArrayList getDependents();
    
    public ArrayList getComponents();

    public ComponentConfig getComponent(String name);
    
    public Date getLastChangeDate();
    
    public void notifyChange()
    throws ConfigException;
    
    public Config getConfigSet();
    
    public int getCacheMode();
    
    public EntityConfig getSuperEntity();
}
