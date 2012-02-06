package pt.webdetails.cdc.ws;

import org.json.JSONException;
import org.json.JSONObject;

public interface JsonSerializable {

  public JSONObject toJSON() throws JSONException;
  
}
