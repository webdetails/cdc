/*!
* Copyright 2002 - 2014 Webdetails, a Pentaho company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

package pt.webdetails.cdc.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.core.HazelcastManager;
import pt.webdetails.cdc.core.ICdcConfig;

/**
 * CDC initializer (work in progress). Alternative to CdcLifeCycleListener.
 */
public class CdcServletContextListener implements ServletContextListener {

  private static Log logger = LogFactory.getLog( CdcServletContextListener.class );

  @Override
  public void contextInitialized( ServletContextEvent sce ) {
    logger.info( "contextInitialized" );

    HazelcastManager manager = HazelcastManager.INSTANCE;
    ICdcConfig cdcConfig =
      new CdcServletConfig(
        new ServletContextConfig( sce.getServletContext() ) );
    manager.configure( cdcConfig );
    try {
      manager.init();
    } catch ( IOException e ) {
      logger.error( "Hazelcast Manager failed to initialize",e );
    }
  }

  @Override
  public void contextDestroyed( ServletContextEvent sce ) {
    logger.info( "contextDestroyed" );
    HazelcastManager.INSTANCE.tearDown();
  }

  private static class ServletContextConfig implements ServletConfig {

    private ServletContext context;

    public ServletContextConfig( ServletContext context ) {
      this.context = context;
    }

    public String getInitParameter( String param ) {
      String value = System.getProperty( param );
      if ( StringUtils.isEmpty( value ) ) {
        value = context.getInitParameter( param );
      }
      return value;
    }

    @SuppressWarnings( "rawtypes" )
    public Enumeration getInitParameterNames() {
      return context.getInitParameterNames();
    }

    public ServletContext getServletContext() {
      return context;
    }

    public String getServletName() {
      return null;
    }

  }

}
