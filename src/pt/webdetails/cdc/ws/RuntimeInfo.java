package pt.webdetails.cdc.ws;

import org.json.JSONException;
import org.json.JSONObject;

import com.hazelcast.monitor.DistributedMemberInfoCallable;

public class RuntimeInfo implements JsonSerializable{

  private long totalMemory;
  private long freeMemory;
  private long maxMemory;
  
  public RuntimeInfo(DistributedMemberInfoCallable.MemberInfo mInfo){
    if(mInfo == null) return;
    
    totalMemory = mInfo.getTotalMemory();
    freeMemory = mInfo.getFreeMemory();
    maxMemory = mInfo.getMaxMemory();
  }
  
  public long getTotalMemory() {
    return totalMemory;
  }
  public void setTotalMemory(long totalMemory) {
    this.totalMemory = totalMemory;
  }
  public long getFreeMemory() {
    return freeMemory;
  }
  public void setFreeMemory(long freeMemory) {
    this.freeMemory = freeMemory;
  }
  public long getMaxMemory() {
    return maxMemory;
  }
  public void setMaxMemory(long maxMemory) {
    this.maxMemory = maxMemory;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("totalMemory", totalMemory);
    json.put("freeMemory", freeMemory);
    json.put("maxMemory", maxMemory);
    return json;
  }
  
  
}
