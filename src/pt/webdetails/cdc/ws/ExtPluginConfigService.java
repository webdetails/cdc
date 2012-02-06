package pt.webdetails.cdc.ws;

import pt.webdetails.cdc.ExternalConfigurationsManager;

public class ExtPluginConfigService {

  
  public String isMondrianHazelcastEnabled(){
    boolean enabled = ExternalConfigurationsManager.isMondrianHazelcastEnabled();
    return new Result(Result.Status.OK, enabled).toString();
  }
  
  public String isCdaHazelcastEnabled(){
    try 
    {
      boolean enabled = ExternalConfigurationsManager.isCdaHazelcastEnabled();
      return new Result(Result.Status.OK, enabled).toString();
    } 
    catch (Exception e) {
      return new Result(Result.Status.ERROR, e.getLocalizedMessage()).toString();
    }
  }
  
  public String setMondrianHazelcastEnabled(boolean enabled){
    try {
      ExternalConfigurationsManager.setMondrianHazelcastEnabled(enabled);
      return new Result(Result.Status.OK, "Please restart Pentaho server.").toString();//TODO: may not be needed
    } catch (Exception e) {
      return new Result(Result.Status.ERROR, e.getLocalizedMessage()).toString();
    }
  }
    
  
  public String setCdaHazelcastEnabled(boolean enabled){
    try{
      ExternalConfigurationsManager.setCdaHazelcastEnabled(enabled);
      return new Result(Result.Status.OK, "Please refresh plugins.").toString();
    }
    catch(Exception e){
      return new Result(Result.Status.ERROR, e.getLocalizedMessage()).toString();
    }
  }
  
}
