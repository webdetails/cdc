package pt.webdetails.cdc.ws;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pt.webdetails.cpf.InterPluginComms;

public class DashboardCacheCleanService {
  
  //cda only
  public String clearDashboard(String dashboard){
    HashMap<String,Object> params = new HashMap<String,Object>();
    params.put("dashboard", dashboard);
    String result = InterPluginComms.callPlugin(InterPluginComms.Plugin.CDE , "listcdasources", params);
    HashMap<String, Object> cdaParams = new HashMap<String,Object>();
    cdaParams.put("method", "removeAll");
    try {
      JSONArray results = new JSONArray(result);
      
      for(int i=0; i<results.length();i++){
        JSONObject dataSource = (JSONObject) results.get(i);
        cdaParams.put("cdaSettingsId", dataSource.getString("cdaSettingsId"));
        if(dataSource.has("dataAccessId")){
          cdaParams.put("dataAccessId", dataSource.getString("dataAccessId"));
        }
        else {
          cdaParams.put("dataAccessId", null);
        }
        String removeResult = InterPluginComms.callPlugin(InterPluginComms.Plugin.CDA , "cacheMonitor", cdaParams, true);
        JSONObject itemsCleared = new JSONObject(removeResult);
        if(StringUtils.equalsIgnoreCase(itemsCleared.getString("status"), "ok")){
          int numCleared = itemsCleared.getInt("result");
          dataSource.put("cleared", numCleared);
        }
      }
      
      return Result.getOK(results).toString();
    } catch (JSONException e) {
      return Result.getFromException(e).toString();
    }
  }
}
