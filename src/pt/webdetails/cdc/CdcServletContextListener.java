/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * CDC initializer (work in progress). Alternative to {@link CdcLifeCycleListener}.
 */
public class CdcServletContextListener implements ServletContextListener {
  
  private static Log logger = LogFactory.getLog(CdcServletContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    logger.info("contextInitialized");
        
    HazelcastManager manager = HazelcastManager.INSTANCE;
    
    String debugMode = System.getProperty("webdetails.cdc.debug");
    if(!StringUtils.isEmpty(debugMode)){
      manager.setDebugMode(Boolean.parseBoolean(debugMode));
    } else {
      manager.setDebugMode(true);
    }
    manager.setLaunchInnerProcess(false);//not supported outside of pentaho yet

    manager.setRegisterMondrian(true);
    
    String liteMode = System.getProperty("hazelcast.lite.member");
    if(!StringUtils.isEmpty(liteMode)){
      manager.setLiteMode(Boolean.parseBoolean(liteMode));
    }
    else {
      manager.setLiteMode(false);
    }
    
    String configFile = sce.getServletContext().getInitParameter("hazelcast.config"); 
    if(StringUtils.isEmpty(configFile)){
      configFile = System.getProperty("hazelcast.config");
    }
    if(StringUtils.isEmpty(configFile)) {
      configFile = "hazelcast.xml";
    }
    manager.init(configFile, true);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    logger.info("contextDestroyed");
    HazelcastManager.INSTANCE.tearDown();
  }

}
