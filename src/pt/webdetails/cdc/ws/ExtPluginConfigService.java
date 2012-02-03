package pt.webdetails.cdc.ws;

import pt.webdetails.cdc.ExternalConfigurationsManager;

public class ExtPluginConfigService {

  
  public Result isMondrianHazelcastEnabled(){
    boolean enabled = ExternalConfigurationsManager.isMondrianHazelcastEnabled();
    return new Result(Result.Status.OK, enabled);
  }
  
  public Result isCdaHazelcastEnabled(){
    try 
    {
      boolean enabled = ExternalConfigurationsManager.isCdaHazelcastEnabled();
      return new Result(Result.Status.OK, enabled);
    } 
    catch (Exception e) {
      return new Result(Result.Status.ERROR, e.getLocalizedMessage());
    }
  }
  
  public Result setMondrianHazelcastEnabled(boolean enabled){
    try {
      ExternalConfigurationsManager.setMondrianHazelcastEnabled(enabled);
      return new Result(Result.Status.OK, "Please restart Pentaho server.");//TODO: may not be needed
    } catch (Exception e) {
      return new Result(Result.Status.ERROR, e.getLocalizedMessage());
    }
  }
    
  
  public Result setCdaHazelcastEnabled(boolean enabled){
    try{
      ExternalConfigurationsManager.setCdaHazelcastEnabled(enabled);
      return new Result(Result.Status.OK, "Please refresh plugins.");
    }
    catch(Exception e){
      return new Result(Result.Status.ERROR, e.getLocalizedMessage());
    }
  }
  
}
