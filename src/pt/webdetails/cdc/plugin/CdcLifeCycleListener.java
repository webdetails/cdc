/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.plugin;

import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;

import org.pentaho.platform.util.messages.LocaleHelper;

import pt.webdetails.cdc.core.HazelcastManager;
import pt.webdetails.cdc.core.ICdcConfig;
import pt.webdetails.cdc.ws.MondrianCacheCleanService;

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
      //do we need it to run?

      hazelcastManager.configure(CdcConfig.getConfig());

      if (CdcConfig.getConfig().isMondrianCdcEnabled() || ExternalConfigurationsHelper.isCdaHazelcastEnabled()) {
        hazelcastManager.setMaster(CdcConfig.getConfig().isMaster());

        if (CdcConfig.getConfig().isAsyncInit()) {
          logger.info("Initializing Hazelcast in new thread.");
          Thread initDaemon = new Thread(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    hazelcastManager.init();
                    listCatalogsInLocales();
                  }
                  catch (Exception e) {
                    logger.fatal("CDC init failed.", e);
                  }
                }
              }
          );
          initDaemon.setDaemon(true);
          initDaemon.start();
        }
        else {
          hazelcastManager.init();
          if (CdcConfig.getConfig().isSyncCacheOnStart()) {
            MondrianCacheCleanService.loadMondrianCatalogs();
            hazelcastManager.reloadMondrianCache();
          }
          listCatalogsInLocales();
        }
      }

    } catch (Exception e) {
      logger.error(e);
      logger.error("CDC couldn't be properly initialized!");
    }

  }

  private void listCatalogsInLocales() {
    //Trying to ensure all locales are covered 
    //TODO: explain why are we doing this
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
    IMondrianCatalogService mondrianCatalogService =
        PentahoSystem.get(IMondrianCatalogService.class, IMondrianCatalogService.class.getSimpleName(), null);
    for (int i=0; i < locales.length; i++) {
      LocaleHelper.setLocale(locales[i]);
      mondrianCatalogService.listCatalogs(CdcLifeCycleListener.getSessionForCatalogCache(), true);
    }
    logger.debug("Reverting to original locale " + originalLocale);
    LocaleHelper.setLocale(originalLocale);
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
    ICdcConfig config = CdcConfig.getConfig();
    hazelcastMgr.configure(config);
  }

  private static IPentahoSession getSessionForCatalogCache() {
    return PentahoSystem.get(IPentahoSession.class, "systemStartupSession", null);
  }

}

