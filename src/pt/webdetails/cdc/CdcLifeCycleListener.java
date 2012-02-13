/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import mondrian.rolap.agg.SegmentBody;
import mondrian.rolap.agg.SegmentHeader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigXmlGenerator;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
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
    init(CdcConfig.getHazelcastConfigFile(), CdcConfig.getConfig().isSuperClient() ,CdcConfig.getConfig().isForceConfig());
  }
  
  private static void init(String configFileName, boolean superClient, boolean forceConfig)
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

    try{
       config.setSuperClient(superClient);      
       logger.info("Launching Hazelcast with " + configFileName);
       Hazelcast.init(config);
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
      Hazelcast.getCluster().addMembershipListener(new MemberLogListener());
    }
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

  /**
   * @param config
   */
  private static void forceRestart(Config config) {
    logger.info("Shutdown ALL Hazelcast instances!!");
    Hazelcast.shutdownAll();
    logger.info("Launching Hazelcast...");
    Hazelcast.init(config);
    if(config.isSuperClient()){
      launchIfNoMember();
    }
  }
  
  //for superClient mode
  private static void launchIfNoMember()
  {
    Cluster cluster = Hazelcast.getCluster();
    for(Member member : cluster.getMembers())
    {
      if(!member.isSuperClient()){
        logger.debug("Member detected, no launch required.");
        return;
      }
    }
    //no non-superClient members
    logger.info("SuperClient mode: no members found, launching a hazelcast server in a new JVM.");
    HazelcastProcessLauncher.launchProcess(0L);
  }
  
  public static void reloadConfig(String configFileName){
    if(configFileName == null) configFileName = CdcConfig.getHazelcastConfigFile();
    init(configFileName, CdcConfig.getConfig().isSuperClient(), true);
  }
  
  public static boolean saveConfig(Config config){
    File configFile = new File(CdcConfig.getHazelcastConfigFile());
    FileWriter fileWriter = null;
    try
    {
      ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator(true);
      fileWriter = new FileWriter(configFile);
      fileWriter.write(xmlGenerator.generate(config));
      return true;
    } catch (FileNotFoundException e) {
      String msg = "File not found: " + configFile.getAbsolutePath();
      logger.error(msg);
      return false;
    } catch (IOException e) {
      String msg = "Error writing file " + configFile.getAbsolutePath();
      logger.error(msg, e);
      return false;
    }
    finally {
      IOUtils.closeQuietly(fileWriter);
    }
    
  }
  
  private static final class MemberLogListener implements MembershipListener {

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
      
      logger.debug("MEMBER ADDED: " + membershipEvent);
      
      logger.debug(" ... from " + membershipEvent.getMember().getInetSocketAddress() + ",  source:" + membershipEvent.getSource());
      
      
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      logger.debug("MEMBER REMOVED: " + membershipEvent);
      
      logger.debug(" ... from " + membershipEvent.getMember().getInetSocketAddress() + ",  source:" + membershipEvent.getSource());
    }
    
    @Override
    public boolean equals(Object other){//static
      return other instanceof MemberLogListener;
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

