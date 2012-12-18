/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalogHelper;

import org.pentaho.platform.util.messages.LocaleHelper;

/**
 * Responsible for setting up distributed cache from configuration.
 */
public class CdcLifeCycleListener implements IPluginLifecycleListener
{

  private static HazelcastManager hazelcastManager = HazelcastManager.INSTANCE;
  
  static Log logger = LogFactory.getLog(CdcLifeCycleListener.class);

  @Override
  public void init() throws PluginLifecycleException
  {
    logger.debug("init");
  }

  @Override
  public void loaded() throws PluginLifecycleException
  {
    logger.debug("CDC loaded.");
    setHazelcastOptionsFromConfig();
    try {
      if (CdcConfig.getConfig().isMondrianCdcEnabled() || ExternalConfigurationsManager.isCdaHazelcastEnabled()){
        hazelcastManager.setMaster(CdcConfig.getConfig().isMaster());
        hazelcastManager.init(CdcConfig.getConfig().getHazelcastConfigFile(), CdcConfig.getConfig().isForceConfig());
        
//        if(CdcConfig.getConfig().isCdaCdcEnabled() && !ExternalConfigurationsManager.isCdaHazelcastEnabled()){
//          logger.info("Hazelcast cache enabled in CDC config but not in CDA, changing CDA settings.");
//          ExternalConfigurationsManager.setCdaHazelcastEnabled(true);
//        }
        
      }
      
      
      //Trying to ensure all locales are covered 
      //TODO: explain why
      List<String> configuredLocales = CdcConfig.getConfig().getLocales();
      
      Locale[] locales;
      Locale originalLocale = LocaleHelper.getLocale();
      if (configuredLocales.size() == 1 && "all".equals(configuredLocales.get(0))) {
        locales = Locale.getAvailableLocales();
      }
      else {
        locales = new Locale[configuredLocales.size()];
        for (int i=0; i < configuredLocales.size(); i++) {
          String[] splitLocale = configuredLocales.get(i).split("_");
          locales[i] = new Locale(splitLocale[0], splitLocale[1]);
        }
      }
     
      logger.debug("Setting schema cache for " + locales.length + " locales.");
      for (int i=0; i < locales.length; i++) {
        LocaleHelper.setLocale(locales[i]);
        MondrianCatalogHelper.getInstance().listCatalogs(CdcLifeCycleListener.getSessionForCatalogCache(), true);        
        
      }
      logger.debug("Reverting to original locale " + originalLocale);
      LocaleHelper.setLocale(originalLocale);

    } catch (Exception e) {
      logger.error(e);
    }

  }
  
  @Override
  public void unLoaded() throws PluginLifecycleException
  {
    logger.debug("CDC Unloading...");
    hazelcastManager.tearDown();
    logger.debug("CDC Unloaded.");
    
  }

  private void setHazelcastOptionsFromConfig(){
    HazelcastManager hazelcastMgr = HazelcastManager.INSTANCE;
    CdcConfig config = CdcConfig.getConfig();
    hazelcastMgr.setDebugMode(config.isDebugMode());
    hazelcastMgr.setLaunchInnerProcess(true);
    hazelcastMgr.setLiteMode(config.isLiteMode());
    hazelcastMgr.setSyncConfig(true);
    hazelcastMgr.setRegisterMondrian(config.isMondrianCdcEnabled());
  }

  
   private static IPentahoSession getSessionForCatalogCache() {     
       return PentahoSystem.get(IPentahoSession.class, "systemStartupSession", null);     
    } 
  
  
}

