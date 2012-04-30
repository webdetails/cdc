/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.FileNotFoundException;

import mondrian.spi.SegmentCache;
import mondrian.spi.SegmentCache.SegmentCacheListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

import pt.webdetails.cdc.mondrian.SegmentCacheHazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
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
  
  private static boolean running = false;
  
  static Log logger = LogFactory.getLog(CdcLifeCycleListener.class);

  @Override
  public void init() throws PluginLifecycleException
  {
    logger.debug("init");

  }

  public static synchronized boolean isRunning(){
    return running;
  }
  
  private static synchronized void setRunning(boolean isRunning){
    running = isRunning;
  }

  @Override
  public void loaded() throws PluginLifecycleException
  {
    logger.debug("CDC loaded.");
    try {
      if (CdcConfig.getConfig().isMondrianCdcEnabled() || ExternalConfigurationsManager.isCdaHazelcastEnabled()){
        init(CdcConfig.getHazelcastConfigFile(), CdcConfig.getConfig().isLiteMode() ,CdcConfig.getConfig().isForceConfig());
      }
    } catch (Exception e) {
      logger.error(e);
    }

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
    setRunning(true);
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
  logger.info("lite mode: no members found, launching a hazelcast server in a new JVM.");
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
  
  
  private static class MondrianLoggingListener implements SegmentCacheListener{

    @Override
    public void handle(SegmentCacheEvent event) {
      switch(event.getEventType()){
        case ENTRY_CREATED:
          logger.debug("(" + (event.isLocal()? "local" : "remote") + ") " + "Mondrian cache entry ADDED: " + event.getSource());
          break;
        case ENTRY_DELETED:
          logger.debug("(" + (event.isLocal()? "local" : "remote") + ") " + "Mondrian cache entry REMOVED: " + event.getSource());
          break;
      }
    }
        
    @Override 
    public boolean equals(Object other) {
      return other instanceof MondrianLoggingListener;
    }

  };
  
  
  private static void registerMondrianCacheSpi(){
    
    SegmentCache mondrianCache = new SegmentCacheHazelcast();
    
    if(CdcConfig.getConfig().isMondrianCdcEnabled() && CdcConfig.getConfig().isDebugMode()){
      logger.debug("adding mondrian listener");
      SegmentCacheListener listener = new MondrianLoggingListener();
      mondrianCache.removeListener(listener);
      mondrianCache.addListener(listener);
      
      
    }
    
    mondrian.spi.SegmentCache.SegmentCacheInjector.addCache(mondrianCache);
    
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
  
//  private static class  HazelcastKeySerializationListener<KEY,VALUE> implements EntryListener<KEY,VALUE>{
//
//    @Override
//    public void entryAdded(EntryEvent<KEY, VALUE> entryEvent) {
//      
//      logger.debug("ADDED, BIN:");
//      Data keyData = null;
//      if(entryEvent instanceof DataAwareEntryEvent){
//        keyData = ((DataAwareEntryEvent)entryEvent).getKeyData();
////        logger.debug( .toString());
//      }
//      else {
//        logger.debug("serializing..");
//        keyData = IOUtil.toData(entryEvent.getKey());
//      }
//      logger.debug("(as data) " + "[" + keyData.hashCode() + "]" + keyData.toString());
//      StringBuilder sb = new StringBuilder();
//      for(byte b : keyData.buffer){
//        sb.append(b).append(", ");
//      }
//    }
//
//    @Override
//    public void entryEvicted(EntryEvent<KEY, VALUE> arg0) {
//
//    }
//
//    @Override
//    public void entryRemoved(EntryEvent<KEY, VALUE> arg0) {
//
//    }
//
//    @Override
//    public void entryUpdated(EntryEvent<KEY, VALUE> arg0) {
//
//    }
//    
//  }

//  private static final class MondrianVerboseEntryListener implements EntryListener<SegmentHeader, SegmentBody>  {
//    
//    @Override
//    public void entryAdded(EntryEvent<SegmentHeader, SegmentBody> event)
//    {
//      SegmentHeader key = event.getKey();
//      logger.debug("Mondrian ENTRY ADDED:" + key);
//    }
//    
//    @Override
//    public void entryUpdated(EntryEvent<SegmentHeader, SegmentBody> event) {
//      SegmentHeader key = event.getKey();
//      logger.debug("Mondrian ENTRY UPDATED:" + key);
//    }
//    
//    @Override
//    public void entryRemoved(EntryEvent<SegmentHeader, SegmentBody> event) 
//    {
//      SegmentHeader key = event.getKey();
//      logger.debug("Mondrian ENTRY REMOVED:" + key);
//    }
//
//    @Override
//    public void entryEvicted(EntryEvent<SegmentHeader, SegmentBody> event) {
//      SegmentHeader key = event.getKey();
//      logger.debug("Mondrian ENTRY EVICTED:" + key);
//    }
//    
//    @Override
//    public boolean equals(Object other){
//      return other instanceof MondrianVerboseEntryListener;
//    }
//
//  }
  
//
//private void testGlobalParamHack(){
// 
//  String key = "cda.IQueryCache";
//  Object test = null;
//  
//  ClassLoader classLoader = SegmentCacheHazelcast.class.getClassLoader();
//  IPluginManager pluginManager = PentahoSystem.get(IPluginManager.class);
//  if (!pluginManager.isBeanRegistered(key)) {
//    if(pluginManager instanceof DefaultPluginManager){
//      IPentahoObjectFactory factory = ((DefaultPluginManager)pluginManager).getBeanFactory();
//      if(factory instanceof IPentahoDefinableObjectFactory){
//        ((IPentahoDefinableObjectFactory)factory).defineObject(key, HazelcastCdcCdaQueryCache.class.getName(), Scope.GLOBAL, classLoader);
//        
//        try {
//          test = pluginManager.getBean(key);
//          
//        } catch (PluginBeanException e) {
//          logger.error(e);
//        }
//      }
//    }
//  }
//  logger.info(test != null ? test.getClass().getName() : "waaah waaah waaaaaaah");      
//  
//}
}

