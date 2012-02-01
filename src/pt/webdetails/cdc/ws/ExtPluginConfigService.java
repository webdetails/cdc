package pt.webdetails.cdc.ws;

import pt.webdetails.cdc.ExternalConfigurationsManager;
import pt.webdetails.cdc.StatusMessage;

public class ExtPluginConfigService {

  public static final class ResponseStatus{
    public static final String OK = "ok";
    public static final String ERROR = "ok";
  }
  
  public StatusMessage isMondrianHazelcastEnabled(){
    boolean enabled = ExternalConfigurationsManager.isMondrianHazelcastEnabled();
    return new StatusMessage(ResponseStatus.OK, "" + enabled); // enabled? "enabled" : "disabled");
  }
  
  public StatusMessage isCdaHazelcastEnabled(){
    try 
    {
      boolean enabled = ExternalConfigurationsManager.isCdaHazelcastEnabled();
      return new StatusMessage(ResponseStatus.OK, "" + enabled); //enabled? "enabled" : "disabled");
    } 
    catch (Exception e) {
      return new StatusMessage(ResponseStatus.ERROR, e.getLocalizedMessage());
    }
  }
  
  public StatusMessage setMondrianHazelcastEnabled(boolean enabled){
    try {
      ExternalConfigurationsManager.setMondrianHazelcastEnabled(enabled);
      return new StatusMessage(ResponseStatus.OK, "Please restart Pentaho server.");
    } catch (Exception e) {
      return new StatusMessage(ResponseStatus.ERROR, e.getLocalizedMessage());
    }
  }
    
  
  public StatusMessage setCdaHazelcastEnabled(boolean enabled){
    try{
      ExternalConfigurationsManager.setCdaHazelcastEnabled(enabled);
      return new StatusMessage(ResponseStatus.OK, "Please refresh plugins.");
    }
    catch(Exception e){
      return new StatusMessage(ResponseStatus.ERROR, e.getLocalizedMessage());
    }
  }
  
}
