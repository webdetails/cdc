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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;


/**
 * Responsible for setting up distributed cache from configuration.
 */
public class CdcLifeCycleListener implements IPluginLifecycleListener
{
  
  public static final String PROPERTY_SUPER_CLIENT = "hazelcast.super.client";
  
//  private static HazelcastInstance instance;
//  private static HazelcastInstance extraInstance;
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
  
  private static synchronized void init(String configFileName, boolean superClient, boolean forceConfig)
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

    //super client: doesn't hold data but has first class access
    //needs a running instance to work
    try{
      String isSuper = System.getProperty(PROPERTY_SUPER_CLIENT);
      if(Boolean.parseBoolean(isSuper) && !superClient){
        System.setProperty(PROPERTY_SUPER_CLIENT , "false");
      }
      else if(superClient){
        logger.info("Setting SuperClient flag");
        System.setProperty(PROPERTY_SUPER_CLIENT, "true");
        
      }
    } catch (SecurityException e){
      logger.error("Error accessing " + PROPERTY_SUPER_CLIENT, e);
    }
    
//    //TODO:conditional, config
//    registerMondrianCacheSpi();

    try{
       config.setLiteMember(superClient);      
       logger.info("Launching Hazelcast with " + configFileName);
//       Hazelcast.init(config);
       //TODO: can we only not enforce compatibility on maps?
       config.setCheckCompatibility(false);
       initConfig(config);
       if(superClient && !hasProperMembers()){
         logger.warn("In lite mode but no instances are present.");
//         setExtraInstanceActive(true);
//         logger.warn("Running in lite mode but no hazelcast instances in same cluster, temporarily reverting to normal mode!");
//         config.setLiteMember(false);
//         initConfig(config);
       }
       if(superClient){
         launchIfNoMember();
       }
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
    
    if(CdcConfig.getConfig().isDebugMode()){    
        logger.debug("adding mondrian listener");
        IMap<SegmentHeader, SegmentBody> monCache = Hazelcast.getMap(CdcConfig.CacheMaps.MONDRIAN_MAP);
        MondrianVerboseEntryListener monShouter = new MondrianVerboseEntryListener();
        monCache.removeEntryListener(monShouter);
        monCache.addEntryListener(monShouter, false);
    }
    Hazelcast.getCluster().addMembershipListener(new MemberSyncListener());
  }

  @Override
  public void unLoaded() throws PluginLifecycleException
  {
    //teardown etc
    Hazelcast.getLifecycleService().shutdown();
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
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      logger.error("ERROR ###################### :",e);
//    }
    logger.info("Launching Hazelcast...");
    initConfig(config);
//    Hazelcast.init(config);
//    if(config.isLiteMember()){
//      launchIfNoMember();
//    }
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
  
//  //for superClient mode
  private static void launchIfNoMember()
  {
    if(hasProperMembers()) return;
//    for(Member member : Hazelcast.getCluster().getMembers())
//    {
//      if(!member.isLiteMember()){ //member.isLiteMember()
//        logger.debug("Member detected, no launch required.");
//        return;
//      }
//    }
    //no non-superClient members
    logger.info("SuperClient mode: no members found, launching a hazelcast server in a new JVM.");
    innerProcess = HazelcastProcessLauncher.launchProcess();
  }
  
  public static void reloadConfig(String configFileName){
    if(configFileName == null) configFileName = CdcConfig.getHazelcastConfigFile();
    init(configFileName, CdcConfig.getConfig().isLiteMode(), true);
  } 


//  public synchronized static void setExtraInstanceActive(boolean active){
//    if(active && extraInstance == null){
//      logger.info("Launching a temporary hazelcast instance");
//      Config config = Hazelcast.getConfig();
//      config.setLiteMember(false);
//      extraInstance = Hazelcast.newHazelcastInstance(config);
//    }
//    else if (!active && extraInstance != null){
//      logger.info("Shutting down temporary hazelcast instance");
//      Hazelcast.getConfig().setLiteMember(true);
//      extraInstance.getLifecycleService().shutdown();
//      extraInstance = null;
//    }
//  }
  
  public synchronized static boolean isExtraInstanceActive(){
    return innerProcess != null;// extraInstance != null;
  }
  
  public static boolean belongsToInstance(Member member, HazelcastInstance instance){
    if(member instanceof HazelcastInstanceAware){
      //TODO: this only works if MemberImpl, should find another way
      //shouldn't change anything but the localMember flag
      ((HazelcastInstanceAware)member).setHazelcastInstance(instance);
      return member.localMember();
    }
    return false;
  }
  
  
//  /**
//   * @param lite
//   */
//  private static void launchReInitThread(final boolean lite) {
//    new Thread( new Runnable(){
//      @Override
//      public void run() {
//        try {
//          Thread.sleep(1000);
//        } catch (InterruptedException e) {
//          logger.error("init thread interrupted ", e);
//        }
//        init(CdcConfig.getHazelcastConfigFile(), lite ,true);
//      }
//    }).run();
//  }
  
  private static final class MemberSyncListener implements MembershipListener {
    
//    public static ThreadGroup threadGroup;

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
      
      logger.debug("MEMBER ADDED: " + membershipEvent);
      logger.info("Adding maps to new member");
      Member newMember = membershipEvent.getMember();
      
      if(newMember.localMember()){
        logger.error("LOCAL MEMBER ATTEMPTED JOIN");
        return;
      }
      

//      else {
        //just sync map configs
      HazelcastConfigHelper.spreadMapConfigs(newMember);
//      }
        if(CdcConfig.getConfig().isLiteMode() && isExtraInstanceActive() && 
            !newMember.localMember() && !newMember.isLiteMember() &&
            hasProperMembers()){
            logger.info("Non-lite instance found, temporary instance no longer needed");
            try {
              innerProcess.destroy();
              innerProcess = null;
            } catch(Exception e){
              logger.error("Error destroying process.", e);
            }
////          Hazelcast.getCluster().removeMembershipListener(this);
//          new Thread(new Runnable(){
//            @Override
//            public void run() {
//              try {
//                Thread.sleep(1000);
//              } catch (InterruptedException e) {
//                logger.error("init thread interrupted ", e);
//              }
//             if(!belongsToInstance(newMember, extraInstance)){
//              extraInstance.getPartitionService().addMigrationListener(new SelfDestructAfterMigration(extraInstance));
//             setExtraInstanceActive(false);
//             }
////              init(CdcConfig.getHazelcastConfigFile(), true ,true);
//            }
//          }).run();
//
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
        //local may not work when more than one instance, remove membership listener to avoid loop
//        Hazelcast.getCluster().removeMembershipListener(this);
//        setExtraInstanceActive(true);
//        spreadMapConfigs();
//        Hazelcast.getCluster().addMembershipListener(this);
//        Hazelcast.getCluster().removeMembershipListener(this);
//        new Thread(threadGroup, new Runnable(){
//          @Override
//          public void run() {
//            try {
//              Thread.sleep(1000);
//            } catch (InterruptedException e) {
//              logger.error("init thread interrupted ", e);
//            }
//            init(CdcConfig.getHazelcastConfigFile(), false ,true);
//          }
//        }).run();
      }
    }
    
    @Override
    public boolean equals(Object other){//static
      return other instanceof MemberSyncListener;
    }
    
  }
//  
//  private static void registerMondrianCacheSpi(){
//    mondrian.spi.SegmentCache.SegmentCacheInjector.addCache(new SegmentCacheHazelcast());
//  }
  
//  private static final class SelfDestructAfterMigration implements MigrationListener {
//
//    private HazelcastInstance instance;
//    
//    public SelfDestructAfterMigration(HazelcastInstance instance){
//      this.instance = instance;
//    }
//    
//    @Override
//    public void migrationCompleted(MigrationEvent arg0) {
//      logger.debug("Migration ended, shutting down...");
//      instance.getPartitionService().removeMigrationListener(this);
//      instance.getLifecycleService().shutdown();
//    }
//
//    @Override
//    public void migrationStarted(MigrationEvent arg0) {
//      logger.debug("mig start ##################");//TODO:delete
//    }
//
//    
//    
//  }
  
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

