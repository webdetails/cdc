/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.util.List;

import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.engine.ISystemSettings;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

public class CdcConfig 
{
  private static final String HAZELCAST_FILE = "hazelcast.xml";
  private static final String HAZELCAST_STANDALONE_FILE = "hazelcast-standalone.xml";
  private static final String SETTINGS_FILE = "settings.xml";
  
  public static final String PLUGIN_ID = "cdc";
  public static final String PLUGIN_TITLE = "cdc";
  public static final String PLUGIN_SYSTEM_PATH = PLUGIN_ID + "/" ;
  public static final String PLUGIN_SOLUTION_PATH = "system/" + PLUGIN_SYSTEM_PATH;

  public static final class CacheMaps {
    public static final String MONDRIAN_MAP = "mondrian";
    public static final String CDA_MAP = "cdaCache";
    public static final String CDA_STATS_MAP = "cdaCacheStats";
  }
  
  private static IPluginManager pluginManager;
  
  private static CdcConfig instance;
  
  public static CdcConfig getConfig(){
    if(instance == null){
      instance = new CdcConfig();
    }
    return instance;
  }
  
  private static IPluginManager getPluginManager(){
    if(pluginManager == null){
      pluginManager = PentahoSystem.get(IPluginManager.class);
    }
    return pluginManager;
  }
   
  public boolean isSuperClient(){
    return getBooleanSetting("superClientMode", true);
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
  
  public String getMondrianHazelcastAdapterClass(){
    return getStringSetting("mondrianConfig/adapterClasses/hazelcast","pt.webdetails.cdc.mondrian.SegmentCacheHazelcast");
  }
  public String getMondrianDefaultAdapterClass(){
    return getStringSetting("mondrianConfig/adapterClasses/default",StringUtils.EMPTY);
  }
  
  public String getMondrianConfigLocation(){
    return getStringSetting("mondrianConfig/location","system/mondrian/mondrian.properties");
  }
  
  public String getCdaHazelcastAdapterClass(){
    return getStringSetting("cdaConfig/adapterClasses/hazelcast","pt.webdetails.cdc.mondrian.SegmentCacheHazelcast");
  }
  public String getCdaDefaultAdapterClass(){
    return getStringSetting("cdaConfig/adapterClasses/default",StringUtils.EMPTY);
  }
  
  public static String getHazelcastConfigFile(){
    String cfg = getStringSetting("hazelcastConfigFile", StringUtils.EMPTY);
    if(StringUtils.isEmpty(cfg)){
      return PentahoSystem.getApplicationContext().getSolutionPath(PLUGIN_SOLUTION_PATH + HAZELCAST_FILE);
    }
    else return cfg;
  }
  public static String getHazelcastStandaloneConfigFile(){
    return PentahoSystem.getApplicationContext().getSolutionPath(PLUGIN_SOLUTION_PATH + HAZELCAST_STANDALONE_FILE);
  }
  
  private static boolean getBooleanSetting(String section, boolean nullValue){
    String setting = getStringSetting(section, null);
    if(setting != null){
      return Boolean.parseBoolean(setting);
    }
    return nullValue;
  }
  
  private static String getStringSetting(String section, String defaultValue){
    return (String) getPluginManager().getPluginSetting(PLUGIN_TITLE, section, defaultValue);
  }
  
//  private int getIntSetting(String section, int defaultValue){
//    String setting = getStringSetting(section, null);
//    if(setting != null){
//      try{
//        return Integer.parseInt(setting);
//      }
//      catch(NumberFormatException e){
//        return defaultValue;
//      }
//    }
//    return defaultValue;
//  }

  @SuppressWarnings("unchecked")
  protected static List<Element> getSettingsXmlSection(String section) {
    ISystemSettings settings = PentahoSystem.getSystemSettings();
    List<Element> elements = settings.getSystemSettings(PLUGIN_SYSTEM_PATH + SETTINGS_FILE, section);
    return elements;
  }
}
