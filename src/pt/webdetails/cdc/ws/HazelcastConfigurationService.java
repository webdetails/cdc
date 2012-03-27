/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.ws;

import java.util.Arrays;

import org.json.JSONArray;

import pt.webdetails.cdc.CdcLifeCycleListener;
import pt.webdetails.cdc.ExternalConfigurationsManager;
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
 
  public String setMapOption(String map, String name, String value){
    MapConfigOption option = MapConfigOption.parse(name);
    CacheMap cacheMap = CacheMap.parse(map);
    
    if(option == null) return Result.getError("No such option: " + name).toString();
    if(cacheMap == null) return Result.getError("No such map: " + name).toString();
    if(value == null) return Result.getError("Must supply value").toString();
    
    Result result =  new Result(Result.Status.OK, "Option changed to '" + value + "'");
    
    MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(cacheMap.getName());
    switch(option){
      case enabled:
        boolean enabled = Boolean.parseBoolean(value);
        switch(cacheMap){
          case Cda:
            try{
              ExternalConfigurationsManager.setCdaHazelcastEnabled(enabled);
              return new Result(Result.Status.OK, "Please refresh plugins (Tools -> Refresh -> System Settings) or restart Pentaho server.").toString();
            }
            catch(Exception e){
              return new Result(Result.Status.ERROR, e.getLocalizedMessage()).toString();
            }
          case Mondrian:
            try {
              ExternalConfigurationsManager.setMondrianHazelcastEnabled(enabled);
              return new Result(Result.Status.OK, "Please restart Pentaho server.").toString();//TODO: may not be needed
            } catch (Exception e) {
              return new Result(Result.Status.ERROR, e.getLocalizedMessage()).toString();
            }
        }
        break;
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
    return result.toString();
  }
  
  public String getMapOption(String map, String name){
    
    MapConfigOption option = MapConfigOption.parse(name);
    
    if(option == null) return new Result(Result.Status.ERROR, "No such option: " + name).toString();

    CacheMap cacheMap = CacheMap.parse(map);
    if(cacheMap == null) return Result.getError("No such map: " + map).toString();
    
    MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(cacheMap.getName());
    
    Object result = null;
    switch(option){
      case enabled:
        switch(cacheMap){
          case Cda:
            try 
            {
              result = ExternalConfigurationsManager.isCdaHazelcastEnabled();
            } 
            catch (Exception e) {
              return new Result(Result.Status.ERROR, e.getLocalizedMessage()).toString();
            }
            break;
          case Mondrian:
            result = ExternalConfigurationsManager.isMondrianHazelcastEnabled();
            break;
        }
        break;
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
  
  //TODO:temp
  public String createLauncher(){
    String serverLauncher = HazelcastProcessLauncher.createLauncherFile(false);
    String debugLauncher = HazelcastProcessLauncher.createLauncherFile(true);
    String msg = serverLauncher + "\n" + debugLauncher;
    return (serverLauncher == null || debugLauncher == null)?
        Result.getError(msg).toString() :
        Result.getOK(msg).toString();
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
