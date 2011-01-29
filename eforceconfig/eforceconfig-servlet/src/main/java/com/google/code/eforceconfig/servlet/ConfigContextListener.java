package com.google.code.eforceconfig.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.code.eforceconfig.Config;
import com.google.code.eforceconfig.ConfigException;
import com.google.code.eforceconfig.ConfigInitializer;
import com.google.code.eforceconfig.ConfigSource;
import com.google.code.eforceconfig.ConfigSourceManager;
import com.google.code.eforceconfig.sources.managers.FileSourceManager;

public class ConfigContextListener implements ServletContextListener, ConfigInitializer
{
    private Config config;
    private ServletContext ctx;
    
    public void contextDestroyed(ServletContextEvent event)
    {
        config.stop();   
    }

    public void contextInitialized(ServletContextEvent event)
    {
        ctx= event.getServletContext();
        config= new Config(ctx.getInitParameter("com.google.code.eforceconfig.CONFIGSET_NAME"));
        try
        {
            config.init(this);
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
        }
    }
    
    public int getCacheSize()
    {
        try
        {
           return Integer.parseInt(ctx.getInitParameter("com.google.code.eforceconfig.CACHE_SIZE"));
        }
        catch (Exception ex)
        {
           return 20;
        }
    }

    public int getChangeControlInterval()
    {
        try
        {
           return Integer.parseInt(ctx.getInitParameter("com.google.code.eforceconfig.CHANGE_CONTROL_INTERVAL"));
        }
        catch (Exception ex)
        {
           return 10000;
        }
    }

    public ConfigSourceManager getConfigSourceManager()
    {
       return new FileSourceManager(ctx.getRealPath("/WEB-INF/entity-config"));
    }

    public ConfigSource[] getConfigSources()
    {
        return new ConfigSource[0];
    }

    public int getGarbageCollectorInterval()
    {
        try
        {
           return Integer.parseInt(ctx.getInitParameter("com.google.code.eforceconfig.GARBAGE_COLLECTOR_INTERVAL"));
        }
        catch (Exception ex)
        {
           return 10000;
        }
    }

    public String getJAXPFactoryClassName()
    {
        return null;
    }

    public boolean getPreloadCacheOnInit()
    {
        return false;
    }

    public boolean getStartChangeControl()
    {
        try
        {
           return Boolean.valueOf(ctx.getInitParameter("com.google.code.eforceconfig.START_CHANGE_CONTROL")).booleanValue();
        }
        catch (Exception ex)
        {
            return true;
        }
    }

    public boolean getStartGarbageCollector()
    {
        try
        {
           return Boolean.valueOf(ctx.getInitParameter("com.google.code.eforceconfig.START_GARBAGE_COLLECTOR")).booleanValue();
        }
        catch (Exception ex)
        {
            return true;
        }
    }

}
