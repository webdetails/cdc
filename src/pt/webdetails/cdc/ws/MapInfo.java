package pt.webdetails.cdc.ws;

import com.hazelcast.monitor.LocalMapStats;

public class MapInfo {
  private long entryCount;
  private long ownedCount;
  private long backupCount;
  
  private long entryMemmory;
  private long ownedMemmory;
  private long backupMemmory;
  
  public MapInfo(){}
  
  public MapInfo(LocalMapStats mapStats){
    
    
    backupCount = mapStats.getBackupEntryCount();
    backupMemmory = mapStats.getBackupEntryMemoryCost();
    
    ownedCount = mapStats.getOwnedEntryCount();
    ownedMemmory = mapStats.getOwnedEntryMemoryCost();
    
    entryCount = backupCount + ownedCount;
    entryMemmory = backupMemmory + ownedMemmory;
    
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
    return entryMemmory;
  }


  public void setEntryMemmory(long entryMemmory) {
    this.entryMemmory = entryMemmory;
  }


  public long getOwnedMemmory() {
    return ownedMemmory;
  }


  public void setOwnedMemmory(long ownedMemmory) {
    this.ownedMemmory = ownedMemmory;
  }


  public long getBackupMemmory() {
    return backupMemmory;
  }


  public void setBackupMemmory(long backupMemmory) {
    this.backupMemmory = backupMemmory;
  }
  
  
  
}
