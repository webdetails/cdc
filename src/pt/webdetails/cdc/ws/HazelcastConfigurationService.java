package pt.webdetails.cdc.ws;

import java.util.Arrays;

import org.json.JSONArray;

import pt.webdetails.cdc.CdcLifeCycleListener;
import pt.webdetails.cdc.HazelcastProcessLauncher;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.Hazelcast;

public class HazelcastConfigurationService {
  

  enum MapConfigOption {
    
    maxSizePolicy,
    maxSize,
    evictionPercentage,
    evictionPolicy,
    timeTolive
    
  }
 
  private static final String[] MAX_SIZE_POLICIES = new String[]{
    MaxSizeConfig.POLICY_CLUSTER_WIDE_MAP_SIZE,
    MaxSizeConfig.POLICY_MAP_SIZE_PER_JVM,
    MaxSizeConfig.POLICY_PARTITIONS_WIDE_MAP_SIZE,
    MaxSizeConfig.POLICY_USED_HEAP_PERCENTAGE,
    MaxSizeConfig.POLICY_USED_HEAP_SIZE
  };
  static{
    Arrays.sort(MAX_SIZE_POLICIES);
  }
  
  private static final String[] EVICTION_POLICIES = new String[]{
    "LRU", "LFU", "NONE"
  };
  static{
    Arrays.sort(EVICTION_POLICIES);
  }
  
  public enum CacheMap {
    
    cda(new String[]{"cdaCache", "cdaCacheStats" }),
    mondrian(new String[]{"mondrian"});
    
    private String[] maps;
    
    CacheMap(final String[] maps){
      this.maps = maps;
    }
  }
 
  public String setMapOption(String map, String name, String value){
    MapConfigOption option = MapConfigOption.valueOf(name);
    CacheMap cacheMap = CacheMap.valueOf(map);
    
    if(option == null) return Result.getError("No such option " + name).toString();
    if(cacheMap == null) return Result.getError("No such map " + name).toString();
    if(value == null) return Result.getError("Must supply value").toString();
    
    Result result =  new Result(Result.Status.OK, "Option changed to '" + value + "'");
    for(String hMap : cacheMap.maps){
      MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(hMap);
      switch(option){
        case maxSizePolicy:
          if(Arrays.binarySearch(MAX_SIZE_POLICIES, value) >= 0){
            mapConfig.getMaxSizeConfig().setMaxSizePolicy(value);
          }
          else return Result.getError("Unrecognized size policy.").toString();
          
          break;
        case evictionPercentage:
          try{
            int evictionPercentage = Integer.parseInt(value);
            if(evictionPercentage <= 0 || evictionPercentage > 100) return Result.getError("Invalid domain for percentage.").toString();
            mapConfig.setEvictionPercentage(evictionPercentage);
          } catch(NumberFormatException nfe){
            return Result.getFromException(nfe).toString();
          }
          break;
        case evictionPolicy:
          if(Arrays.binarySearch(EVICTION_POLICIES, value) >= 0){
            mapConfig.setEvictionPolicy(value);
          }
          else return Result.getError("Unrecognized eviction policy").toString();
              
          mapConfig.setEvictionPolicy(value);
          break;
        case maxSize:
          try{
            int maxSize = Integer.parseInt(value);
            mapConfig.getMaxSizeConfig().setSize(maxSize);
          }
          catch (NumberFormatException nfe){
            return Result.getFromException(nfe).toString();
          }

          break;
        case timeTolive:
          try{
            int timeToLiveSeconds = Integer.parseInt(value);
            mapConfig.setTimeToLiveSeconds(timeToLiveSeconds);
          }
          catch (NumberFormatException nfe){
            return Result.getFromException(nfe).toString();
          }
      }

    }
    return result.toString();
  }
  
  public String getMapOption(String map, String name){
    MapConfigOption option = MapConfigOption.valueOf(name);
    
    if(option == null) return new Result(Result.Status.ERROR, "No such option " + name).toString();

    MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(CacheMap.valueOf(map).maps[0]);
    
//    Result response = new Result();
//    response.setStatus(Result.Status.OK);
    Object result = null;
    switch(option){
      case maxSizePolicy:
        result = mapConfig.getMaxSizeConfig().getMaxSizePolicy();
        break;
      case evictionPercentage:
        result = mapConfig.getEvictionPercentage();
        break;
      case evictionPolicy:
        result = mapConfig.getEvictionPolicy();
        break;
      case maxSize:
        result = mapConfig.getMaxSizeConfig().getSize();
        break;
      case timeTolive:
        result = mapConfig.getTimeToLiveSeconds();
    }
    
    return Result.getOK(result).toString();
//    return response;
    
  }

  public static String getMaxSizePolicies(){
    JSONArray results = new JSONArray();
    for(String value : MAX_SIZE_POLICIES){
      results.put(value);  
    }
    return Result.getOK(results).toString();
  }
  
  public static String getEvictionPolicies(){
    JSONArray results = new JSONArray();
    for(String value : EVICTION_POLICIES){
      results.put(value);  
    }
    return Result.getOK(results).toString();
  }

  public String saveConfig(){
    if( CdcLifeCycleListener.saveConfig(Hazelcast.getConfig())){
      return new Result(Result.Status.OK, "Configuration saved.").toString();
    }
    else {
      return new Result(Result.Status.ERROR, "Error saving file.").toString();
    }
  }
  
  public String loadConfig(){
    try{
      CdcLifeCycleListener.reloadConfig(null);
      return new Result(Result.Status.OK, "Configuration read from file.").toString();
    }
    catch(Exception e){
      return Result.getFromException(e).toString();
    }
  }
  
  //TODO:temp
  public String launchJvmInstance(){
    HazelcastProcessLauncher.launchProcess(null);
    return Result.getOK("Process launched.").toString();
  }
  
  //TODO: temporary, will be removed
  @Deprecated
  public String shutdownAll(){
    try{
      Hazelcast.shutdownAll();
      return new Result(Result.Status.OK, "shutdown all.").toString();
    }
    catch(Exception e){
      return Result.getFromException(e).toString();
    }
  }  
  
  
}
