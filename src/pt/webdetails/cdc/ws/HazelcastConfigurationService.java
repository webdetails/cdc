/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.ws;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

import pt.webdetails.cdc.CdcConfig;
import pt.webdetails.cdc.CdcLifeCycleListener;
import pt.webdetails.cdc.ExternalConfigurationsManager;
import pt.webdetails.cdc.HazelcastConfigHelper;
import pt.webdetails.cdc.HazelcastConfigHelper.MapConfigOption;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;

public class HazelcastConfigurationService {
  
  static Log log = LogFactory.getLog(CdcLifeCycleListener.class);
 
  public String setMapOption(String map, String name, String value){
    
    SecurityAssertions.assertIsAdmin();
    
    MapConfigOption option = MapConfigOption.parse(name);
    CacheMap cacheMap = CacheMap.parse(map);
    
    if(option == null) return Result.getError("No such option: " + name).toString();
    if(cacheMap == null) return Result.getError("No such map: " + name).toString();
    if(value == null) return Result.getError("Must supply value").toString();
    
    MapConfig mapConfig = getMapConfig(cacheMap);
    switch(option){
      case enabled:
        Boolean enabled = parseBooleanStrict(value);
        if(enabled != null){
          switch(cacheMap){          
            case Cda:
              try{
                ExternalConfigurationsManager.setCdaHazelcastEnabled(enabled);
                return new Result(Result.Status.OK, "Configuration changed, please restart Pentaho server after finishing changes").toString();
              }
              catch(Exception e){
                return Result.getFromException(e).toString();
              }
            case Mondrian:
              try {
                CdcConfig.getConfig().setMondrianCdcEnabled(enabled);
                return new Result(Result.Status.OK, "Configuration changed, please restart Pentaho server after finishing changes").toString();                
              } catch (Exception e) {
                return Result.getFromException(e).toString();
              }
          }
        }
        else {
          return Result.getError("enabled must be either 'true' or 'false'.").toString();
        }
        break;
      case maxSizePolicy:
        if(Arrays.binarySearch(HazelcastConfigHelper.MAX_SIZE_POLICIES, value) >= 0){
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
        if(Arrays.binarySearch(HazelcastConfigHelper.EVICTION_POLICIES, value) >= 0){
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
      default://shouldn't reach
        return Result.getError("Unrecognized option " + name).toString();
    }
    return Result.getOK("Option " + name + " changed.").toString();
  }
  
  /**
   * Defaults to null instead of false for unparseable values.
   * @param value
   * @return
   */
  private static Boolean parseBooleanStrict(String value){
    if(!StringUtils.isEmpty(value)){
      value = value.trim().toLowerCase();
      if(value.equals("true")) return true;
      else if(value.equals("false")) return false;
    }
    return null;
  }
  
  public String getMapOption(String map, String name){
    
    MapConfigOption option = MapConfigOption.parse(name);
    
    if(option == null) return new Result(Result.Status.ERROR, "No such option: " + name).toString();

    CacheMap cacheMap = CacheMap.parse(map);
    if(cacheMap == null) return Result.getError("No such map: " + map).toString();
    
    MapConfig mapConfig = getMapConfig(cacheMap);
    
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
            result = CdcConfig.getConfig().isMondrianCdcEnabled();
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
    for(String value : HazelcastConfigHelper.MAX_SIZE_POLICIES){
      results.put(value);  
    }
    return Result.getOK(results).toString();
  }
  
  public static String getEvictionPolicies(){
    JSONArray results = new JSONArray();
    for(String value : HazelcastConfigHelper.EVICTION_POLICIES){
      results.put(value);  
    }
    return Result.getOK(results).toString();
  }

  public String saveConfig()
  {
    SecurityAssertions.assertIsAdmin();
    
    if( HazelcastConfigHelper.saveConfig()){
        return new Result(Result.Status.OK, "Configuration saved and propagated.").toString();
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
  
  private static MapConfig getMapConfig(CacheMap cacheMap){
    if(CdcLifeCycleListener.isRunning()){
      return Hazelcast.getConfig().getMapConfig(cacheMap.getName());
    }
    else {
      log.warn("Hazelcast must be enabled for map config to be available.");
      return new MapConfig("Bogus");
    }
  }

  
  
}
