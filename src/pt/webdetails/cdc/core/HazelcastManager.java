/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.core;

import java.io.FileNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.plugin.HazelcastProcessLauncher;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

/**
 * Singleton brokering all access to Hazelcast.
 */
public enum HazelcastManager {

  INSTANCE;

  private static Log logger = LogFactory.getLog(HazelcastManager.class);

  private static final String PREFER_IPV4_STACK = "java.net.preferIPv4Stack";

  //public static final String 

  /* ***************
   * CONFIGURATION *
   */
  private boolean liteMode = true;
  private boolean launchInnerProcess = false;
  private boolean syncConfig = true;

  private ICdcConfig cdcConfig;
  /* 
   * configuration *
   * ***************/
  
  /* ***************
   * RUNNING STUFF *
   */
  private boolean master = true;
  private HazelcastInstance cdcHazelcast;

  private IHazelcastLauncher spawned;
  private Object spawnedLock = new Object();

  private boolean running = false;
  private MondrianOverseer mondrianOverseer = new MondrianOverseer();;

  /* 
   * running stuff *
   * ***************/

  
  /* ***************
   * CONFIGURATION *
   */

  public boolean isLiteMode() {
    return liteMode;
  }

  public void setLiteMode(boolean liteMode) {
    this.liteMode = liteMode;
  }

  public boolean isLaunchInnerProcess() {
    return launchInnerProcess && isMaster();
  }

  public boolean isSyncConfig() {
    return syncConfig && isMaster();
  }

  public boolean isMaster() {
    return master;
  }

  public void setMaster(boolean isMaster) {
    master = isMaster;
  }

  /* 
   * configuration *
   * ***************/

  public boolean isRunning() {
    return running;
  }

  private void setRunning(boolean running) {
    this.running = running;
  }

  public HazelcastInstance getHazelcast() {
    return cdcHazelcast;
  }

  public synchronized void configure(ICdcConfig config) {
    this.cdcConfig = config;

    this.launchInnerProcess = config.isLaunchInnerProc();
    this.liteMode = config.isLiteMode();
    this.syncConfig = config.isSyncConfig();
    this.master = config.isMaster();
  }

  public synchronized void init() {
    assert this.cdcConfig != null;
    init(cdcConfig.getHazelcastConfigFile(), cdcConfig.isForceConfig());
  }

  public synchronized void init(String configFileName, boolean forceConfig)
  {  
    //TODO: pre/post-init state

    checkPreferIPv4Stack();

    //set log4j logging
    System.setProperty("hazelcast.logging.type","log4j");

    logger.debug("CDC init for config " + configFileName);
    Config hzConfig = null;
    if(configFileName != null){
      try {
        XmlConfigBuilder configBuilder = new XmlConfigBuilder(configFileName);
        hzConfig = configBuilder.build();
      } catch (FileNotFoundException e) {
        logger.error("Config file not found, using defaults", e);
      }
    }
    
    if(hzConfig == null){
      hzConfig = new Config();
    }

    try{
       hzConfig.setLiteMember(liteMode);      
       logger.info("Launching Hazelcast with " + configFileName);
       hzConfig.setCheckCompatibility(false);
       initHazelcast(hzConfig);
    }
    catch(IllegalStateException e){

      if(getHazelcast() != null && getHazelcast().getLifecycleService().isRunning()){
        Config runningConfig = getHazelcast().getConfig();
        reloadConfigurationIfNeeded(hzConfig, runningConfig, forceConfig);
      }
      else {
        logger.error("Could not initialize Hazelcast but no running instances were found.");
      }
    }
    //activate mondrian cache according to settings
    if(cdcConfig.isRegisterMondrian()){
      try {
        mondrianOverseer.initMondrianCache(cdcConfig);
      } catch (Exception e) {
        logger.error("Couldn't register with mondrian cache", e);
      }
    }
    if(isSyncConfig()){
      MembershipListener memberListener = new MemberSyncListener();
      getHazelcast().getCluster().removeMembershipListener(memberListener);
      getHazelcast().getCluster().addMembershipListener(memberListener);
    }
  }

  public synchronized void tearDown(){
    if(isRunning()){
      logger.debug("shutdown Hazelcast");
      shutdownHazelcast();
      setRunning(false);
      removeExtraInstance();       
    }
    logger.debug("Hazelcast not running.");
  }


  private void shutdownHazelcast() {
    try {
        getHazelcast().getLifecycleService().shutdown();
    } catch (IllegalStateException e) {
      //if full shutdown, this is normal
      logger.info("Hazelcast already shutdown.");
    }
  }

  public int reloadMondrianCache() {
    return mondrianOverseer.reloadMondrianCache();
  }

  
  /**
   * Checks if desired configuration is incompatible with running one, and optionally forces a restart.
   * @param config the desired configuration
   * @param runningConfig configuration of running instance
   * @param forceConfig whether to force restart to run with the current configuration
   */
  public void reloadConfigurationIfNeeded(Config config, Config runningConfig, boolean forceConfig) {
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

  public boolean isExtraInstanceActive(){
    synchronized(spawnedLock){
      return spawned.isRunning();
    }
  }

  private boolean hasProperMembers(){
    int minMemberCount = isExtraInstanceActive() ? 2 : 1;
    int properMemberCount = 0;
    for (Member member : getHazelcast().getCluster().getMembers()) {
      if (!member.isLiteMember()){
        if (++properMemberCount >= minMemberCount) {
          logger.debug("hasProperMembers: at least " + properMemberCount + ".");
          return true;
        }
      }
    }
    logger.debug("hasProperMembers: " + properMemberCount + "/" + minMemberCount);
    return false;
  }

  private void launchIfNoMember()
  {
    synchronized(spawnedLock){

      if(!isMaster() || hasProperMembers()) { return; }

      //no non-superClient members
      logger.info("lite mode and no members found, launching a hazelcast instance");
      logger.warn("cache will not be recoverd as no non-lite instances were found");
      spawned.start();
    }
  }

  private void removeExtraInstance(){
    synchronized(spawnedLock){
      try {
        if (spawned.isRunning()) {
          logger.debug("Stopping spawned node.");
          spawned.stop();
        }
        else {
          logger.debug("Spawned node not running.");
        }
      } catch(Exception e){
        logger.error("Error destroying spawned node.", e);
      }
    }
  }

  /**
   * Forces flag to exist
   */
  private void checkPreferIPv4Stack() {
    String preferIPv4Stack = System.getProperty(PREFER_IPV4_STACK);
    //on hazelcast 2.0 null is changed to true, contrary to what the default should be
    if(preferIPv4Stack == null){
      logger.info(PREFER_IPV4_STACK + " flag is null, forcing default (false)");
      System.setProperty(PREFER_IPV4_STACK, "false");
    }
  }
  
  private void initHazelcast(Config config){ //TODO: review sync?
    if(cdcHazelcast != null) {
      logger.error("Hazelcast instance already started, shutdown first!");
    }

    logger.info("starting hazelcast");

    cdcHazelcast = Hazelcast.newHazelcastInstance(config);

    if (cdcHazelcast == null) {
      throw new RuntimeException("Unable to start Hazelcast instance.");
    }

    if(isSyncConfig()){
      HazelcastConfigHelper.spreadMapConfigs();
    }
    synchronized(spawnedLock) {
      if (isLaunchInnerProcess()) {
        assert !StringUtils.isEmpty(cdcConfig.getVmMemory());
        logger.debug("-Xmx=" + cdcConfig.getVmMemory());
        spawned = new HazelcastProcessLauncher(cdcConfig.getVmMemory());
      }
      else {//if (isMaster()) {
        //thread version
        spawned = new HazelcastSimpleLauncher(config);
      }
    }
    if (spawned != null) {//TODO: toString
      logger.debug("using " + spawned.getClass().getName());
    }
    //at least one non-tile instance must be running
    if(liteMode && !hasProperMembers()){
      logger.warn("In lite mode but no instances are present.");
    }
    if(liteMode){
      launchIfNoMember();
    }
    setRunning(true);
    logger.info("hazelcast running");
  }
  
  private void forceRestart(Config config) {
    logger.info("Shutdown ALL local Hazelcast instances!!");
    Hazelcast.shutdownAll();
    logger.info("Launching Hazelcast...");
    initHazelcast(config);
  }

  
  /* ***********
   * LISTENERS *
   */

  /**
   * Member listener that synchronizes map configurations across members
   * and checks if an inner process is needed.
   */
  private class MemberSyncListener implements MembershipListener {

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
      
      logger.debug("MEMBER ADDED: " + printMember(membershipEvent.getMember()));
      logger.info("Adding maps to new member");
      Member newMember = membershipEvent.getMember();
      
      if(newMember.localMember()){
        logger.error("LOCAL MEMBER ATTEMPTED JOIN");
        return;
      }

      HazelcastConfigHelper.spreadMapConfigs(newMember);

      if(getHazelcast().getConfig().isLiteMember() && 
          isExtraInstanceActive() && 
          !newMember.isLiteMember() && //TODO: ignore if innerProc from another member
          hasProperMembers())
      {
          logger.info("Non-lite instance found, temporary instance no longer needed");
          removeExtraInstance();
      }
      
    }


    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      logger.debug("MEMBER REMOVED: "  + printMember(membershipEvent.getMember()));

      if(getHazelcast().getConfig().isLiteMember() &&
         !isExtraInstanceActive())
      {
        launchIfNoMember();
      }
    }
    
    @Override
    public boolean equals(Object other){//static
      return other instanceof MemberSyncListener;
    }

    private String printMember(Member member) {
      return "" + member
          + ( member.localMember() ? " [local]" : " [remote]");
    }
  }

}
