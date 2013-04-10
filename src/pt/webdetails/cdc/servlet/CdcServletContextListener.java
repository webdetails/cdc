/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.servlet;

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
import pt.webdetails.cdc.plugin.CdcLifeCycleListener;


/**
 * CDC initializer (work in progress). Alternative to {@link CdcLifeCycleListener}.
 */
public class CdcServletContextListener implements ServletContextListener {
  
  private static Log logger = LogFactory.getLog(CdcServletContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    logger.info("contextInitialized");

    HazelcastManager manager = HazelcastManager.INSTANCE;
    ICdcConfig cdcConfig =
        new CdcServletConfig(
            new ServletContextConfig(sce.getServletContext()));
    manager.configure(cdcConfig);
    manager.init();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    logger.info("contextDestroyed");
    HazelcastManager.INSTANCE.tearDown();
  }

  private static class ServletContextConfig implements ServletConfig {

    private ServletContext context;

    public ServletContextConfig(ServletContext context) {
      this.context = context;
    }

    public String getInitParameter(String param) {
      String value = System.getProperty(param);
      if (StringUtils.isEmpty(value)) {
        value = context.getInitParameter(param);
      }
      return value;
    }

    @SuppressWarnings("rawtypes")
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
