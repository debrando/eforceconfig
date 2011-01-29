package com.google.code.eforceconfig.util.xml;

import java.text.*;
import org.apache.log4j.Logger;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ErrorPrinter
   extends DefaultHandler
{

   public static String SCHEMA_LANGUAGE =
      "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                        XML_SCHEMA =
      "http://www.w3.org/2001/XMLSchema",
                        SCHEMA_SOURCE =
      "http://java.sun.com/xml/jaxp/properties/schemaSource";

   private static Logger logger= Logger.getLogger(ErrorPrinter.class);
   private MessageFormat message= new MessageFormat("({0}: {1}, {2}): {3}");

   private void print(SAXParseException x)
   {
      String msg = message.format(new Object[]
                                  {
                                     x.getSystemId(),
                                     new Integer(x.getLineNumber()),
                                     new Integer(x.getColumnNumber()),
                                     x.getMessage()
                                  });
      logger.debug(msg);
   }

   public void warning(SAXParseException x)
   {
      logger.warn(x);
   }

   public void error(SAXParseException x)
      throws SAXParseException   
   {
      logger.error(x);
   }

   public void fatalError(SAXParseException x)
      throws SAXParseException
   {
      logger.fatal(x);
   }
}
