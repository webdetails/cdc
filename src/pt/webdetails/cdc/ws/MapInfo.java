package pt.webdetails.cdc.ws;

import org.json.JSONException;
import org.json.JSONObject;

import com.hazelcast.monitor.LocalMapStats;

public class MapInfo implements JsonSerializable{
  private long entryCount;
  private long ownedCount;
  private long backupCount;
  
  private long entryMemory;
  private long ownedMemory;
  private long backupMemory;
  
  public MapInfo(){}
  
  public MapInfo(LocalMapStats mapStats){
    
    
    backupCount = mapStats.getBackupEntryCount();
    backupMemory = mapStats.getBackupEntryMemoryCost();
    
    ownedCount = mapStats.getOwnedEntryCount();
    ownedMemory = mapStats.getOwnedEntryMemoryCost();
    
    entryCount = backupCount + ownedCount;
    entryMemory = backupMemory + ownedMemory;
    
  }


  public long getEntryCount() {
    return entryCount;
  }


  public void setEntryCount(long entryCount) {
    this.entryCount = entryCount;
  }


  public long getOwnedCount() {
    return ownedCount;
  }


  public void setOwnedCount(long ownedCount) {
    this.ownedCount = ownedCount;
  }


  public long getBackupCount() {
    return backupCount;
  }


  public void setBackupCount(long backupCount) {
    this.backupCount = backupCount;
  }


  public long getEntryMemmory() {
    return entryMemory;
  }


  public void setEntryMemmory(long entryMemmory) {
    this.entryMemory = entryMemmory;
  }


  public long getOwnedMemmory() {
    return ownedMemory;
  }


  public void setOwnedMemmory(long ownedMemmory) {
    this.ownedMemory = ownedMemmory;
  }


  public long getBackupMemmory() {
    return backupMemory;
  }


  public void setBackupMemmory(long backupMemmory) {
    this.backupMemory = backupMemmory;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject result = new JSONObject();
    result.put("entryCount", entryCount);
    result.put("ownedCount", ownedCount);
    result.put("backupCount", backupCount);
    
    result.put("entryMemory", entryMemory);
    result.put("ownedMemory", ownedMemory);
    result.put("backupMemory", backupMemory);    

    
    return result;
  }
  
   
  
}
