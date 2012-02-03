package pt.webdetails.cdc.ws;

import java.util.Arrays;

import pt.webdetails.cdc.CdcLifeCycleListener;

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
 
  public Result setMapOption(String map, String name, String value){
    MapConfigOption option = MapConfigOption.valueOf(name);
    CacheMap cacheMap = CacheMap.valueOf(map);
    
    if(option == null) return Result.getError("No such option " + name);
    if(cacheMap == null) return Result.getError("No such map " + name);
    if(value == null) return Result.getError("Must supply value");
    
    Result result =  new Result(Result.Status.OK, "Option changed to '" + value + "'");
    for(String hMap : cacheMap.maps){
      MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(hMap);
      switch(option){
        case maxSizePolicy:
          if(Arrays.binarySearch(MAX_SIZE_POLICIES, value) >= 0){
            mapConfig.getMaxSizeConfig().setMaxSizePolicy(value);
          }
          else return Result.getError("Unrecognized size policy.");
          
          break;
        case evictionPercentage:
          try{
            int evictionPercentage = Integer.parseInt(value);
            if(evictionPercentage <= 0 || evictionPercentage > 100) return Result.getError("Invalid domain for percentage.");
            mapConfig.setEvictionPercentage(evictionPercentage);
          } catch(NumberFormatException nfe){
            return Result.getFromException(nfe);
          }
          break;
        case evictionPolicy:
          if(Arrays.binarySearch(EVICTION_POLICIES, value) >= 0){
            mapConfig.setEvictionPolicy(value);
          }
          else return Result.getError("Unrecognized eviction policy");
              
          mapConfig.setEvictionPolicy(value);
          break;
        case maxSize:
          try{
            int maxSize = Integer.parseInt(value);
            mapConfig.getMaxSizeConfig().setSize(maxSize);
          }
          catch (NumberFormatException nfe){
            return Result.getFromException(nfe);
          }

          break;
        case timeTolive:
          try{
            int timeToLiveSeconds = Integer.parseInt(value);
            mapConfig.setTimeToLiveSeconds(timeToLiveSeconds);
          }
          catch (NumberFormatException nfe){
            return Result.getFromException(nfe);
          }
      }

    }
    return result;
  }
  
  public Result getMapOption(String map, String name){
    MapConfigOption option = MapConfigOption.valueOf(name);
    
    if(option == null) return new Result(Result.Status.ERROR, "No such option " + name);

    MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(CacheMap.valueOf(map).maps[0]);
    
    Result response = new Result();
    response.setStatus(Result.Status.OK);
    switch(option){
      case maxSizePolicy:
        response.setResult(mapConfig.getMaxSizeConfig().getMaxSizePolicy());
        break;
      case evictionPercentage:
        response.setResult(mapConfig.getEvictionPercentage());
        break;
      case evictionPolicy:
        response.setResult(mapConfig.getEvictionPolicy());
        break;
      case maxSize:
        response.setResult(mapConfig.getMaxSizeConfig().getSize());
        break;
      case timeTolive:
        response.setResult(mapConfig.getTimeToLiveSeconds());
    }
    return response;
    
  }

  public static Result getMaxSizePolicies(){
    return Result.getOK(MAX_SIZE_POLICIES);
  }
  
  public static Result getEvictionPolicies(){
    return Result.getOK(EVICTION_POLICIES);
  }
  
//  public static String[] getMaxSizePolicies(){
//    return MAX_SIZE_POLICIES;
//  }

  public Result saveConfig(){
    if( CdcLifeCycleListener.saveConfig(Hazelcast.getConfig())){
    return new Result(Result.Status.OK, "Configuration saved.");
    }
    else {
      return new Result(Result.Status.ERROR, "Error saving file.");
    }
  }
  
  public Result loadConfig(){
    try{
      CdcLifeCycleListener.reloadConfig(null);
      return new Result(Result.Status.OK, "Configuration read from file.");
    }
    catch(Exception e){
      return Result.getFromException(e);
    }
  }
  
  //TODO: temporary, will be removed
  @Deprecated
  public Result shutdownAll(){
    try{
      Hazelcast.shutdownAll();
      return new Result(Result.Status.OK, "shutdown all.");
    }
    catch(Exception e){
      return Result.getFromException(e);
    }
  }  
  
  
}
