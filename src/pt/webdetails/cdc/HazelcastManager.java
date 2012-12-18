/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.FileNotFoundException;

import mondrian.olap.Connection;
import mondrian.olap.DriverManager;
import mondrian.olap.Util;
import mondrian.spi.SegmentCache;
import mondrian.spi.SegmentCache.SegmentCacheInjector;
import mondrian.spi.SegmentCache.SegmentCacheListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;

import pt.webdetails.cdc.mondrian.SegmentCacheHazelcast;

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
  
  //TODO: refactor: separate singleton, factory,
  
  INSTANCE;
  
  
  private static Log logger = LogFactory.getLog(HazelcastManager.class);
  
  private static final String PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
  
  /* ***************
   * CONFIGURATION *
   */
  //TODO: move?
  
  private boolean liteMode = true;
  private boolean debugMode = true;
  private boolean registerMondrian = true;
  private boolean launchInnerProcess = false;
  private boolean syncConfig = true;
  private boolean secondaryMode = false;
  private boolean syncCacheOnStart = true;
  /* 
   * configuration *
   * ***************/
  
  /* ***************
   * RUNNING STATE *
   */
  private boolean master = true;
  private HazelcastInstance localInstance;
  private Process innerProcess;
  private Object innerProcessLock = new Object();
  private boolean running = false;
  /* 
   * running state *
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

  public boolean isDebugMode() {
    return debugMode;
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }

  public boolean isRegisterMondrian() {
    return registerMondrian;
  }

  public void setRegisterMondrian(boolean mondrianEnabled) {
    this.registerMondrian = mondrianEnabled;
  }

  public boolean isLaunchInnerProcess() {
    return launchInnerProcess && isMaster();
  }

  public void setLaunchInnerProcess(boolean launchInnerProcess) {
    this.launchInnerProcess = launchInnerProcess;
  }

  public boolean isSyncConfig() {
    return syncConfig && isMaster();
  }

  public void setSyncConfig(boolean syncConfig) {
    this.syncConfig = syncConfig;
  }

  public boolean isSecondaryMode() {
    return secondaryMode;
  }

  public boolean isMaster() {
    return master;
  }

  public void setMaster(boolean isMaster) {
    master = isMaster;
  }

  public boolean isSyncCacheOnStart() {
    return syncCacheOnStart;
  }

  public void setSyncCacheOnStart(boolean syncCacheOnStart) {
    this.syncCacheOnStart = syncCacheOnStart;
  }
  /* 
   * configuration *
   * ***************/

  public void setSecondaryMode(boolean secondaryMode) {
    this.secondaryMode = secondaryMode;
  }

  public boolean isRunning() {
    return running;
  }

  private void setRunning(boolean running) {
    this.running = running;
  }

  
  public HazelcastInstance getHazelcast() {
    return localInstance;
  }

  public synchronized void init(String configFileName, boolean forceConfig)
  {  
    
    checkPreferIPv4Stack();
    
    //set log4j logging
    System.setProperty("hazelcast.logging.type","log4j");

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
       initHazelcast(config);
    }
    catch(IllegalStateException e){

      if(getHazelcast() != null && getHazelcast().getLifecycleService().isRunning()){
        Config runningConfig = getHazelcast().getConfig();
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
    if(isRegisterMondrian()){
      registerMondrianCacheSpi(isDebugMode());
    }
    if(isSyncConfig()){
      MembershipListener memberListener = new MemberSyncListener();
  //    InstanceListener instanceListener = new InstanceReInitListener();
  //    Hazelcast.removeInstanceListener(instanceListener);
  //    Hazelcast.addInstanceListener(instanceListener);
      getHazelcast().getCluster().removeMembershipListener(memberListener);
      getHazelcast().getCluster().addMembershipListener(memberListener);
    }
  }
  
  public synchronized void tearDown(){
    if(isRunning()){
      shutdownHazelcast();
      // removeExtraInstance();
      setRunning(false);
      removeExtraInstance();       
    }
  }


  private void shutdownHazelcast() {
    try {
        getHazelcast().getLifecycleService().shutdown();
    } catch (IllegalStateException e) {
      //if full shutdown, this is normal
      logger.info("Hazelcast already shutdown.");
    }
  }
  
  /**
   * Registers {@link SegmentCache} as mondrian cache
   * @param addDebugger add {@link MondrianLoggingListener} 
   */
  public void registerMondrianCacheSpi(boolean addDebugger){
    
    logger.info("registering with mondrian");
    
    SegmentCacheHazelcast mondrianCache = new SegmentCacheHazelcast(syncCacheOnStart);
    
    if(isRegisterMondrian() && isDebugMode()){
      logger.debug("adding mondrian listener");
      SegmentCacheListener listener = new MondrianLoggingListener();
      mondrianCache.removeListener(listener);
      mondrianCache.addListener(listener);
    }

    boolean cacheAlreadyIn = false;
    for(SegmentCache cache : SegmentCacheInjector.getCaches()) {
      if (cache instanceof SegmentCacheHazelcast) {
        cacheAlreadyIn = true;
        mondrianCache = (SegmentCacheHazelcast) cache;
        break;
      }
    }
    if (!cacheAlreadyIn) {
      mondrian.spi.SegmentCache.SegmentCacheInjector.addCache(mondrianCache);
    }
    else {
      logger.info("CDC cache already in mondrian. Restart server if you want to replace it");
    }
    
    if(syncCacheOnStart) {
      // have to make sure stars are loaded before adding cache
      IMondrianCatalogService mondrianCatalogService =
          PentahoSystem.get(IMondrianCatalogService.class, IMondrianCatalogService.class.getSimpleName(), null);
      for (MondrianCatalog catalog :
           mondrianCatalogService.listCatalogs(PentahoSessionHolder.getSession(), true))
      {
        String connectStr = catalog.getDataSourceInfo() +
            "; Catalog= " + catalog.getDefinition();
        Util.PropertyList properties = Util.parseConnectString(connectStr);
        logger.debug("loading connection: " + connectStr);
        Connection conn = DriverManager.getConnection(properties, null);
        if (conn == null) {
          logger.error("Problem getting connection for " + connectStr);
        }
      }
    }
    for (SegmentCacheListener listener : mondrianCache.getListenersToSync()) {
      if (listener instanceof MondrianLoggingListener) {
        logger.debug("Sync: skipping " + listener.getClass().getName());
        continue;
      }
      logger.debug("Sync: notifying " + listener.getClass().getName() + " of cache contents. ");
      int loadCount = mondrianCache.syncWithListener(listener);
      if (loadCount > 0) {
        logger.info("Sync: notified " + listener.getClass().getName() + " of " + loadCount + " cache entries.");
      }
    }
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
    synchronized(innerProcessLock){
      return innerProcess != null;
    }
  }

  private boolean hasProperMembers(){
    int properMemberCount = 0;
    for(Member member : getHazelcast().getCluster().getMembers()){
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

  private void launchIfNoMember()
  {
    synchronized(innerProcessLock){
      if(hasProperMembers() || !isLaunchInnerProcess()) return;
      
      //no non-superClient members
      logger.info("lite mode: no members found, launching a hazelcast server in a new JVM.");
      innerProcess = HazelcastProcessLauncher.launchProcess();
    }
  }

  private void removeExtraInstance(){
    synchronized(innerProcessLock){
      try {
        if(innerProcess != null) {
          logger.debug("Destroying inner JVM instance.");
          innerProcess.destroy();
          innerProcess = null;
        }
      } catch(Exception e){
        logger.error("Error destroying process.", e);
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
    if(localInstance != null) logger.error("Hazelcast instance already started, shutdown first!");
    logger.info("starting hazelcast");
    localInstance = Hazelcast.newHazelcastInstance(config); //two instances were being generated
    if(isSyncConfig()){
      HazelcastConfigHelper.spreadMapConfigs();
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
   * Mondrian listener that just logs cache additions and removals.
   */
  static class MondrianLoggingListener implements SegmentCacheListener{

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

  
  /**
   * Member listener that synchronizes map configurations across members
   * and checks if an inner process is needed.
   */
  private class MemberSyncListener implements MembershipListener {

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

      if(getHazelcast().getConfig().isLiteMember() && 
          isExtraInstanceActive() && 
          !newMember.localMember() && 
          !newMember.isLiteMember() && //TODO: ignore if innerProc from another member
          hasProperMembers())
      {
          logger.info("Non-lite instance found, temporary instance no longer needed");
          removeExtraInstance();
      }
      
    }


    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      logger.debug("MEMBER REMOVED: " + membershipEvent);

      if(!membershipEvent.getMember().localMember() &&
          getHazelcast().getConfig().isLiteMember() && 
          !hasProperMembers())
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
}
