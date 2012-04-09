/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.FileNotFoundException;

import mondrian.spi.SegmentBody;
import mondrian.spi.SegmentHeader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

import pt.webdetails.cdc.mondrian.SegmentCacheHazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;


/**
 * Responsible for setting up distributed cache from configuration.
 */
public class CdcLifeCycleListener implements IPluginLifecycleListener
{
  
  public static final String PROPERTY_LITE_MODE = "hazelcast.super.client";
  
  private static Process innerProcess;
  
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
    init(CdcConfig.getHazelcastConfigFile(), CdcConfig.getConfig().isLiteMode() ,CdcConfig.getConfig().isForceConfig());
  }
  
  private static synchronized void init(String configFileName, boolean liteMode, boolean forceConfig)
  {  

    logger.debug("CDC init for config " + configFileName);
    Config config = null;
    if(configFileName != null){
      try {
        XmlConfigBuilder configBuilder = new XmlConfigBuilder(configFileName);
        config = configBuilder.build();
      } catch (FileNotFoundException e) {
        logger.error("Config file not found, using defaults", e);
      }
    }
    
    if(config == null){
      config = new Config();
    }

//    //lite mode: doesn't hold data but has first class access
//    //needs a running instance to work
//    try{
//      String isLite = System.getProperty(PROPERTY_LITE_MODE);
//      if(Boolean.parseBoolean(isLite) && !liteMode){
//        System.setProperty(PROPERTY_LITE_MODE , "false");
//      }
//      else if(liteMode){
//        logger.info("Setting SuperClient flag");
//        System.setProperty(PROPERTY_LITE_MODE, "true");
//        
//      }
//    } catch (SecurityException e){
//      logger.error("Error accessing " + PROPERTY_LITE_MODE, e);
//    }

    try{
       config.setLiteMember(liteMode);      
       logger.info("Launching Hazelcast with " + configFileName);
       config.setCheckCompatibility(false);
       initConfig(config);
    }
    catch(IllegalStateException e){

      if(Hazelcast.getLifecycleService().isRunning()){
        Config runningConfig = Hazelcast.getConfig();
        
        reloadConfigurationIfNeeded(config, runningConfig, forceConfig);

      }
      else {
        logger.error("Could not initialize Hazelcast but no running instances were found.");
      }
    }
    
    //at least one non-tile instance must be running
    if(liteMode && !hasProperMembers()){
      logger.warn("In lite mode but no instances are present.");
    }
    if(liteMode){
      launchIfNoMember();
    }
    
    //activate mondrian cache according to settings
    if(CdcConfig.getConfig().isMondrianCdcEnabled()){
      registerMondrianCacheSpi();
    }
    
//    if(CdcConfig.getConfig().isDebugMode()){    
//        logger.debug("adding mondrian listener");
//        IMap<SegmentHeader, SegmentBody> monCache = Hazelcast.getMap(CdcConfig.CacheMaps.MONDRIAN_MAP);
//        MondrianVerboseEntryListener monShouter = new MondrianVerboseEntryListener();
//        monCache.removeEntryListener(monShouter);
//        monCache.addEntryListener(monShouter, false);
//    }
    
    MembershipListener memberListener = new MemberSyncListener();
    InstanceListener instanceListener = new InstanceReInitListener();
    Hazelcast.removeInstanceListener(instanceListener);
    Hazelcast.addInstanceListener(instanceListener);
    Hazelcast.getCluster().removeMembershipListener(memberListener);
    Hazelcast.getCluster().addMembershipListener(memberListener);
  }

  @Override
  public void unLoaded() throws PluginLifecycleException
  {
    //teardown etc
    Hazelcast.getLifecycleService().shutdown();
    removeExtraInstance();
  }

  public static Config getHazelcastConfig(){
    return Hazelcast.getConfig();
  }
  
  public static Config getHazelcastConfig(String configFileName){
    Config config = null;
    if(configFileName != null){
      try {
        XmlConfigBuilder configBuilder = new XmlConfigBuilder(configFileName);
        config = configBuilder.build();
      } catch (FileNotFoundException e) {
        logger.error("Config file not found, using defaults", e);
      }
    }
    return config;
  }
  

  /**
   * @param configFileName
   * @param forceConfig
   * @param config
   * @param runningConfig
   */
  public static void reloadConfigurationIfNeeded(Config config, Config runningConfig, boolean forceConfig) {
    String runningGroupName = runningConfig.getGroupConfig().getName();
    
    boolean isCompatible=false;
    
    try{
      isCompatible = runningConfig.isCompatible(config);
    }
    catch(RuntimeException e){
      isCompatible=false;
    }
    
    if(!forceConfig && isCompatible){
      logger.info("Hazelcast instance already running with [" + runningGroupName +"] config.");
    }
    else
    {
      logger.info("Forcing configuration reload, overriding running group [" + runningGroupName + "]");
      //2nd attempt
      try{
        forceRestart(config);
      }
      catch(IllegalStateException ise){
        logger.warn("Hazelcast already started, could not load configuration. Shutdown all instances and restart if configuration needs changes.");
      }
    }
    
  }


  private static void forceRestart(Config config) {
    logger.info("Shutdown ALL local Hazelcast instances!!");
    Hazelcast.shutdownAll();
    logger.info("Launching Hazelcast...");
    initConfig(config);
  }
  
  private static void initConfig(Config config){
    Hazelcast.init(config);
    HazelcastConfigHelper.spreadMapConfigs();
  }
  
  private static boolean hasProperMembers(){
    int properMemberCount = 0;
    for(Member member : Hazelcast.getCluster().getMembers()){
      if(!member.isLiteMember() && !member.localMember()){
        if(innerProcess == null){
          return true;
        }
        else if(++properMemberCount >= 2) {//need two
          return true;
        }
      }
    }
    return false;
  }
  

  
  public static void reloadConfig(String configFileName){
    if(configFileName == null) configFileName = CdcConfig.getHazelcastConfigFile();
    init(configFileName, CdcConfig.getConfig().isLiteMode(), true);
  } 


private synchronized static void launchIfNoMember()
{
  if(hasProperMembers()) return;
  
  //no non-superClient members
  logger.info("SuperClient mode: no members found, launching a hazelcast server in a new JVM.");
  innerProcess = HazelcastProcessLauncher.launchProcess();
  
}
  
  public synchronized static boolean isExtraInstanceActive(){
    return innerProcess != null;// extraInstance != null;
  }
  
  public synchronized static void removeExtraInstance(){
    try {
      if(innerProcess != null) {
        innerProcess.destroy();
        innerProcess = null;
      }
    } catch(Exception e){
      logger.error("Error destroying process.", e);
    }
  }
  
  
  private static void registerMondrianCacheSpi(){
    mondrian.spi.SegmentCache.SegmentCacheInjector.addCache(new SegmentCacheHazelcast());
  }
  
  
  private static class InstanceReInitListener implements InstanceListener {

    @Override
    public void instanceCreated(InstanceEvent arg0) {}

    @Override
    public void instanceDestroyed(InstanceEvent arg0) {
      init(CdcConfig.getHazelcastConfigFile(), CdcConfig.getConfig().isLiteMode(), false);
    }
    
    @Override
    public boolean equals(Object other){
      return other instanceof InstanceReInitListener;
    }
    
  }
  
  private static final class MemberSyncListener implements MembershipListener {

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
      
      logger.debug("MEMBER ADDED: " + membershipEvent);
      logger.info("Adding maps to new member");
      Member newMember = membershipEvent.getMember();
      
      if(newMember.localMember()){
        logger.error("LOCAL MEMBER ATTEMPTED JOIN");
        return;
      }

      HazelcastConfigHelper.spreadMapConfigs(newMember);

        if(CdcConfig.getConfig().isLiteMode() && isExtraInstanceActive() && 
            !newMember.localMember() && !newMember.isLiteMember() &&
            hasProperMembers()){
            logger.info("Non-lite instance found, temporary instance no longer needed");
            removeExtraInstance();

        }

    }


    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      logger.debug("MEMBER REMOVED: " + membershipEvent);
      if(!membershipEvent.getMember().localMember() &&
          Hazelcast.getConfig().isLiteMember() && !hasProperMembers())
      { 
        logger.warn("Last instance exited, cache lost!");
        logger.warn("In lite mode but no instances are present.");
        launchIfNoMember();

      }
    }
    
    @Override
    public boolean equals(Object other){//static
      return other instanceof MemberSyncListener;
    }
    
  }

  private static final class MondrianVerboseEntryListener implements EntryListener<SegmentHeader, SegmentBody>  {
    
    @Override
    public void entryAdded(EntryEvent<SegmentHeader, SegmentBody> event)
    {
      SegmentHeader key = event.getKey();
      logger.debug("Mondrian ENTRY ADDED:" + key);
    }
    
    @Override
    public void entryUpdated(EntryEvent<SegmentHeader, SegmentBody> event) {
      SegmentHeader key = event.getKey();
      logger.debug("Mondrian ENTRY UPDATED:" + key);
    }
    
    @Override
    public void entryRemoved(EntryEvent<SegmentHeader, SegmentBody> event) 
    {
      SegmentHeader key = event.getKey();
      logger.debug("Mondrian ENTRY REMOVED:" + key);
    }

    @Override
    public void entryEvicted(EntryEvent<SegmentHeader, SegmentBody> event) {
      SegmentHeader key = event.getKey();
      logger.debug("Mondrian ENTRY EVICTED:" + key);
    }
    
    @Override
    public boolean equals(Object other){
      return other instanceof MondrianVerboseEntryListener;
    }

  }
}

