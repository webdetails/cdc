/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cpf;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON method call result wrapper
 */
public class Result implements JsonSerializable {
  
  private static Log logger = LogFactory.getLog(Result.class);
  
  public enum Status {
    OK,
    ERROR
  }
  
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

  public static Result getFromException(Exception e)
  {  
    String msg = e.getLocalizedMessage();
    if(StringUtils.isEmpty(msg)){
      msg = e.getMessage();
      if(StringUtils.isEmpty(msg)){
        msg = e.getClass().getName();
      }
    }
    return getError(msg);
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
  @Override
  public JSONObject toJSON() throws JSONException {
    return json;
  }

  
}
