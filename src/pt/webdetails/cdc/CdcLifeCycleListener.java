package pt.webdetails.cdc;

import java.io.FileNotFoundException;

import mondrian.rolap.agg.SegmentBody;
import mondrian.rolap.agg.SegmentHeader;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;


/**
 * Responsible for setting up distributed cache from configuration.
 */
public class CdcLifeCycleListener implements IPluginLifecycleListener
{
  
  private static final String PLUGIN_NAME = "cdc";
  private static final String PLUGIN_PATH = "system/" + PLUGIN_NAME + "/";
  private static final String CACHE_CFG_FILE_HAZELCAST = "hazelcast.xml";
  public static final String PROPERTY_SUPER_CLIENT = "hazelcast.super.client";
  
  static Log logger = LogFactory.getLog(CdcLifeCycleListener.class);


  public void init() throws PluginLifecycleException
  {
    logger.debug("init started");
    
    logger.debug("init ended");
  }


  public void loaded() throws PluginLifecycleException
  {
    logger.debug("CDC loaded.");
    init(PentahoSystem.getApplicationContext().getSolutionPath(PLUGIN_PATH + CACHE_CFG_FILE_HAZELCAST),false,false);
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
        System.setProperty(PROPERTY_SUPER_CLIENT, "true");
      }
    } catch (SecurityException e){
      logger.error("Error accessing " + PROPERTY_SUPER_CLIENT, e);
    }

    try{
      
//      //testing
//      if(Hazelcast.getLifecycleService().isRunning()){
//        logger.info("Hazelcast instance running.");
//        Config runningConfig = Hazelcast.getConfig();
//        if(runningConfig.isCompatible(runningConfig)){
//          logger.info("Incompatible config " + runningConfig.getGroupConfig().getName());//TODO:act
//        }
//        else logger.info("compatible config running");
//        if(runningConfig.equals(config)){
//          logger.info("EQUALS " + runningConfig.getGroupConfig().getName());
//        }
//      }
//      
//      logger.info("Shutdown ALL Hazelcast instances!!");
//       Hazelcast.shutdownAll();//TODO: this forces reload of init; move elsewhere
       logger.info("Launching Hazelcast with " + configFileName);
       Hazelcast.init(config);
    }
    catch(IllegalStateException e){

      if(Hazelcast.getLifecycleService().isRunning()){
        Config runningConfig = Hazelcast.getConfig();
        String currentGroup = runningConfig.getGroupConfig().getName();
        logger.info("Hazelcast instance already running with [" + currentGroup +"] config.");
        
        if(forceConfig || !StringUtils.equals(config.getGroupConfig().getName(), currentGroup))
        {
          logger.info("Unknown configuration running, forcing reload.");
          //2nd attempt
          try{
            //TODO: we should somehow detect whenthis is really needed
            logger.info("Shutdown ALL Hazelcast instances!!");
            Hazelcast.shutdownAll();//TODO: this forces reload of init; move elsewhere
            logger.info("Launching Hazelcast with " + configFileName);
            Hazelcast.init(config);
          }
          catch(IllegalStateException ise){
            logger.warn("Hazelcast already started, could not load configuration. Shutdown all instances and restart if configuration needs changes.");
          }
        }
        else {
          logger.info("Hazelcast configuration not reloaded.");
        }
      }
      else {
        logger.error("Could not initialize Hazelcast but no running instances were found.");
      }
      

    }
    
    //debugging...
    //cda needs its own PluginClassLoader
//    logger.debug("adding cda listener");
//    IMap<TableCacheKey, TableModel> cdaCache = Hazelcast.getMap("cdaCache");
//    CdaVerboseEntryListener cdaShouter = new CdaVerboseEntryListener();
//    cdaCache.removeEntryListener(cdaShouter);
//    cdaCache.addEntryListener(cdaShouter, false);
    
    logger.debug("adding mondrian listener");
    IMap<SegmentHeader, SegmentBody> monCache = Hazelcast.getMap("mondrian");
    MondrianVerboseEntryListener monShouter = new MondrianVerboseEntryListener();
    monCache.removeEntryListener(monShouter);
    monCache.addEntryListener(monShouter, false);
    
//    try {
//      
//      ConfigManager.testCdaConfigSwitch();
//    } catch (DocumentException e) {
//      // TODO Auto-generated catch block
//      logger.error(e.getMessage());
//      e.printStackTrace();
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      logger.error(e.getMessage());
//      e.printStackTrace();
//    }
    
  }


  public void unLoaded() throws PluginLifecycleException
  {
    //teardown etc
  }
  
  
//  private static final class CdaVerboseEntryListener implements EntryListener<TableCacheKey, TableModel>  {
//    
//    @Override
//    public void entryAdded(EntryEvent<TableCacheKey, TableModel> event) 
//    {
//      logger.debug("CDA ENTRY ADDED");
//    }//ignore
//    @Override
//    public void entryUpdated(EntryEvent<TableCacheKey, TableModel> event) {}//ignore
//    
//    @Override
//    public void entryRemoved(EntryEvent<TableCacheKey, TableModel> event) 
//    {
//      logger.debug("CDA ENTRY REMOVED");
//    }
//
//    @Override
//    public void entryEvicted(EntryEvent<TableCacheKey, TableModel> event) {
//      logger.debug("CDA ENTRY EVICTED");
//    }
//    
//    @Override
//    public boolean equals(Object other){
//      return other instanceof CdaVerboseEntryListener;
//    }
//
//  }
  
  //TODO:testing purposes only
  private static final class MondrianVerboseEntryListener implements EntryListener<SegmentHeader, SegmentBody>  {
    
    
    @Override
    public void entryAdded(EntryEvent<SegmentHeader, SegmentBody> event) 
    {
      SegmentHeader key = event.getKey();
      logger.debug("Mondrian ENTRY ADDED:" + key);
    }//ignore
    @Override
    public void entryUpdated(EntryEvent<SegmentHeader, SegmentBody> event) {}//ignore
    
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

