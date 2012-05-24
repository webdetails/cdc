package pt.webdetails.cdc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.hazelcast.operations.DistributedMapConfig;

import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigXmlGenerator;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;

public class HazelcastConfigHelper {
  
  static Log logger = LogFactory.getLog(HazelcastConfigHelper.class);
  
  public enum MapConfigOption {
    
    maxSizePolicy,
    maxSize,
    evictionPercentage,
    evictionPolicy,
    timeTolive,
    
    enabled;
    
    public static MapConfigOption parse(String value){
      try{
        return valueOf(value);
      }
      catch(IllegalArgumentException e){
        return null;
      }
    }
    
  }
 
  public static final String[] MAX_SIZE_POLICIES = new String[]{
    MaxSizeConfig.POLICY_CLUSTER_WIDE_MAP_SIZE,
    MaxSizeConfig.POLICY_MAP_SIZE_PER_JVM,
    MaxSizeConfig.POLICY_PARTITIONS_WIDE_MAP_SIZE,
    MaxSizeConfig.POLICY_USED_HEAP_PERCENTAGE,
    MaxSizeConfig.POLICY_USED_HEAP_SIZE
  };
  static{
    Arrays.sort(MAX_SIZE_POLICIES);
  }
  
  public static final String[] EVICTION_POLICIES = new String[]{
    "LRU", "LFU", "NONE"
  };
  static{
    Arrays.sort(EVICTION_POLICIES);
  }

  /**
   * Sets MapConfig in Member
   * @param mapConfig the map configuration to send
   * @param member
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static Boolean spreadMapConfig(MapConfig mapConfig, Member member)  {
    DistributedTask<Boolean> distribMapConfig = new DistributedTask<Boolean>(new DistributedMapConfig(mapConfig), member);
    ExecutorService execService = Hazelcast.getExecutorService();
    execService.execute(distribMapConfig);
    try {
      return distribMapConfig.get();
    } catch (Exception e) {
      logger.error("Error attempting to send MapConfig " + mapConfig.getName() + " to " + member.toString());
      return false;
    }
  }
  
  public static Collection<Boolean> spreadMapConfigs() {
    Collection<Boolean> result = null;
    for(MapConfig mapConfig: Hazelcast.getConfig().getMapConfigs().values()){
        if(result == null){
          result = spreadMapConfig(mapConfig);
        }
        else {
          result.addAll(spreadMapConfig(mapConfig));
        }
    }
    return result;
  }
  
  public static void spreadMapConfigs(Member member) {
    for(MapConfig mapConfig : Hazelcast.getConfig().getMapConfigs().values()){
      boolean ok = false;
      try {
        ok = spreadMapConfig(mapConfig, member);
      } catch (Exception e) {
        logger.error("Could not send map configuration " + mapConfig.getName() + " to " + member,e);
      }
      logger.info("sending map " + mapConfig.getName() + " to " + member + "..." + (ok ? "OK" : "FAILED"));
    }
  }
  
  public static Collection<Boolean> spreadMapConfig(MapConfig mapConfig) {
    
    ArrayList<Boolean> boolist = new ArrayList<Boolean>();
    for(Member member: Hazelcast.getCluster().getMembers()){
      if(!member.localMember()){//skip this
        boolist.add(HazelcastConfigHelper.spreadMapConfig(mapConfig, member));
      }
    }
    return boolist;
  }
  
  
  public static Config cloneConfig(Config config){
    ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator(false);
    String configXml = xmlGenerator.generate(config);
    return new InMemoryXmlConfig(configXml);
  }
  
  
  public static boolean saveConfig(){
    return saveConfig(Hazelcast.getConfig());
  }
  public static boolean saveConfig(Config config){
    return saveConfig(config, CdcConfig.getConfig().getHazelcastConfigFile());
  }
  public static boolean saveConfig(Config config, String fileName){
    File configFile = new File(fileName);
    
    if(!configFile.exists()){
      logger.info(fileName + " does not exist, creating.");
      try {
        configFile.createNewFile();
      } catch (IOException e) {
        logger.error("Error attempting to create file " + fileName);
        return false;
      }
    }
    
    FileWriter fileWriter = null;
    try
    {
      ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator(true);
      fileWriter = new FileWriter(configFile);
      fileWriter.write(xmlGenerator.generate(config));
      HazelcastConfigHelper.spreadMapConfigs();
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
  
}
