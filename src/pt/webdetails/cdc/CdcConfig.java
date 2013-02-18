/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dom4j.Element;
import pt.webdetails.cpf.PluginSettings;

public class CdcConfig extends PluginSettings
{
  private static final String HAZELCAST_FILE = "hazelcast.xml";
  private static final String HAZELCAST_STANDALONE_FILE = "hazelcast-standalone.xml";
  private static Log logger = LogFactory.getLog(CdcConfig.class);
  
  public static final String PLUGIN_ID = "cdc";
  public static final String PLUGIN_TITLE = "cdc";
  public static final String PLUGIN_SYSTEM_PATH = PLUGIN_ID + "/" ;
  public static final String PLUGIN_SOLUTION_PATH = "system/" + PLUGIN_SYSTEM_PATH;

  public static final class CacheMaps {
    public static final String MONDRIAN_MAP = "mondrian";
    public static final String CDA_MAP = "cdaCache";
    public static final String CDA_STATS_MAP = "cdaCacheStats";
  }
  
  private static CdcConfig instance;
  
  public static CdcConfig getConfig(){
    if(instance == null){
      instance = new CdcConfig();
    }
    return instance;
  }

  @Override
  public String getPluginName() {
    return "cdc";
  }
  
  /* ************
   * Config Items
   * start */
  
  public String getHazelcastConfigFile(){
    String cfg = getStringSetting("hazelcastConfigFile", StringUtils.EMPTY);
    if(StringUtils.isEmpty(cfg)){
      return getSolutionPath(PLUGIN_SOLUTION_PATH + HAZELCAST_FILE);
    }
    else return cfg;
  }
  
  public static String getHazelcastStandaloneConfigFile(){
    return getSolutionPath(PLUGIN_SOLUTION_PATH + HAZELCAST_STANDALONE_FILE);
  }
  
  public boolean enableShutdownThread() {
    return getBooleanSetting("enableShutdownThread", false);
  }
   
  public boolean isLiteMode(){
    return getBooleanSetting("liteMode", true);
  }
  
  public boolean isDebugMode(){
    return getBooleanSetting("debugMode", false);
  }
  
  public boolean isForceConfig(){
    return getBooleanSetting("forceConfig",false);
  }
  
  public String getCdaConfigLocation(){
    return getStringSetting("cdaConfig/location", "system/cda/plugin.xml");
  }
  
  public String getCdaCacheBeanId(){
    return getStringSetting("cdaConfig/beanID","cda.IQueryCache");
  }
  
  public boolean isMondrianCdcEnabled(){
    return getBooleanSetting("mondrianConfig/enabled", false);
  }
  public void setMondrianCdcEnabled(boolean enabled){
    if(!writeSetting("mondrianConfig/enabled", "" + enabled) ){
      logger.error("Could not write property mondrianConfig/enabled");
    }
  }

  public boolean isSyncCacheOnStart() {
    return getBooleanSetting("mondrianConfig/syncCacheOnStart", true);
  }

  public String getCdaHazelcastAdapterClass(){
    return getStringSetting("cdaConfig/adapterClasses/hazelcast", "pt.webdetails.cda.cache.HazelcastQueryCache");
  }
  public String getCdaDefaultAdapterClass(){
    return getStringSetting("cdaConfig/adapterClasses/default",StringUtils.EMPTY);
  }
  
  public String getVmMemory(){
    return getStringSetting("vmMemory", "512m");
  }

  public boolean isMaster() {
    return getBooleanSetting("master", true);
  }

  public List<String> getLocales() {
    List<Element> localesXml = getSettingsXmlSection("locales/locale");
    
    List<String> localesAsStr = new ArrayList<String>(localesXml.size());
    
    for (Element e : localesXml) {
      localesAsStr.add(e.getText());
    }
    
    return localesAsStr;
    
  }
  
  /* end *
   * config items
   * ************/
  

}
