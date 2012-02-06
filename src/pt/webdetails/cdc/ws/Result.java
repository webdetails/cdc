package pt.webdetails.cdc.ws;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;




public class Result {
  
  private static Log logger = LogFactory.getLog(Result.class);//TODO:
  
  public enum Status {
    OK,
    ERROR
  }

  
//  private Status status;
//  private String result;
  
  private JSONObject json;
  
  public Result() {}
  public Result(Status status, Object result) {
    json = new JSONObject();
    try {
      json.put("status", status.toString());
      
      if(result != null && result instanceof JsonSerializable){
        json.put("result",  ((JsonSerializable)result).toJSON());
      }
      else json.put("result", result);

    } catch (JSONException e) {
      logger.error("Error writing JSON",e);
    }
    
  }

  
  public static Result getFromException(Exception e){
    return getError(e.getLocalizedMessage());
  }
  public static Result getOK(Object result){
    
    return new Result(Status.OK, result);
  }
  

  public static Result getError(String msg){
    return new Result(Status.ERROR, msg);
  }
  
  @Override
  public String toString(){
    return json != null ? json.toString() : "null";
  }

  
}
