package com.google.code.eforceconfig.exphandlers;

import com.google.code.eforceconfig.Config;
import com.google.code.eforceconfig.EntityConfig;
import com.google.code.eforceconfig.ExpressionHandler;
import com.google.code.eforceconfig.util.StringHandler;

public class ConfigSetExpressionHandler implements ExpressionHandler
{

    public String translate(EntityConfig requester, String value)
    {
        String[] pieces= StringHandler.split(value,"/");
        
        if (pieces.length<4)
            throw new RuntimeException("Invalid configset expression: '"+value+"'");
        
        Config c= (pieces[0].equals(".") ? requester.getConfigSet() : Config.getConfigSet(pieces[0]));
        
        EntityConfig ec= c.getEntity(pieces[1]);
        
        if (ec==null)
            throw new RuntimeException("EntityConfig: '"+pieces[1]+"' is not already loaded or does not exists.");

        if (requester!=null)
            ec.addDependent(requester);
        
        return ec.translate(value.substring(value.indexOf("/",value.indexOf("/")+1)+1));
    }

    public String translate(String type)
    {
        return translate(null,type);
    }
}
