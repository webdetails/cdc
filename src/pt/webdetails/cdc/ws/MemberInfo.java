package pt.webdetails.cdc.ws;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.ws.MapInfo;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;
import com.hazelcast.monitor.DistributedMapStatsCallable;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.DistributedMapStatsCallable.MemberMapStat;

public class MemberInfo {

  private static Log logger = LogFactory.getLog(MemberInfo.class);
  
  private String address;
  private boolean isSuperClient;
  private MapInfo cdaCacheInfo;
  private MapInfo mondrianCacheInfo;
  
  private static final String MONDRIAN_MAP = "mondrian";
  private static final String CDA_MAP = "cdaCache";
  
  public MemberInfo(){}
  
  public MemberInfo(Member member) {
    //map stats
    if(member != null) {
      address = member.getInetSocketAddress().toString();
      isSuperClient = member.isSuperClient();
      
      cdaCacheInfo = new MapInfo( getLocalMapStats(member, CDA_MAP));
      mondrianCacheInfo = new MapInfo( getLocalMapStats(member, MONDRIAN_MAP));
    }
    else {
      logger.error("Member NULL");//TODO: remove this
    }
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public boolean isSuperClient() {
    return isSuperClient;
  }

  public void setSuperClient(boolean isSuperClient) {
    this.isSuperClient = isSuperClient;
  }

  public MapInfo getCdaCacheInfo() {
    return cdaCacheInfo;
  }

  public void setCdaCacheInfo(MapInfo cdaCacheInfo) {
    this.cdaCacheInfo = cdaCacheInfo;
  }

  public MapInfo getMondrianCacheInfo() {
    return mondrianCacheInfo;
  }

  public void setMondrianCacheInfo(MapInfo mondrianCacheInfo) {
    this.mondrianCacheInfo = mondrianCacheInfo;
  }
  
  private static LocalMapStats getLocalMapStats(Member member, String mapName){
    
    DistributedTask<DistributedMapStatsCallable.MemberMapStat> mapStatTask = 
        new DistributedTask<DistributedMapStatsCallable.MemberMapStat>(new DistributedMapStatsCallable(mapName), member) ;
    
    ExecutorService execService = Hazelcast.getExecutorService();
    execService.execute(mapStatTask);
    try 
    {
      MemberMapStat mapStat = mapStatTask.get();
      return mapStat.getLocalMapStats();
    } 
    catch (InterruptedException e) 
    {
      logger.error("Timeout waiting for LocalMapStats on member " + member.getInetSocketAddress(), e);
    } 
    catch (ExecutionException e) 
    {
      logger.error("Error waiting for LocalMapStats on member " + member.getInetSocketAddress(), e);
    }
    
    return null;
  }
  
}
