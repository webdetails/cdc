package pt.webdetails.cdc.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;
import com.hazelcast.monitor.DistributedMapStatsCallable;
import com.hazelcast.monitor.DistributedMemberInfoCallable;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.DistributedMapStatsCallable.MemberMapStat;

public class HazelcastMonitorService {
  
  private static Log logger = LogFactory.getLog(HazelcastMonitorService.class);
  
  public static final String MONDRIAN_MAP = "mondrian";
  public static final String CDA_MAP = "cdaCache";
  
 
  private static final Map<String, String> mapNameResolver = new HashMap<String,String>();
  
  static{
    mapNameResolver.put("cdaCache", CDA_MAP);
    mapNameResolver.put("cda", CDA_MAP);
    mapNameResolver.put("cdacache", CDA_MAP);
    mapNameResolver.put("CDA", CDA_MAP);
    
    mapNameResolver.put("mondrian", MONDRIAN_MAP);
    mapNameResolver.put("mondrianCache", MONDRIAN_MAP);
    mapNameResolver.put("mondriancache", MONDRIAN_MAP);
    mapNameResolver.put("MONDRIAN", MONDRIAN_MAP);
  }

  
  public ClusterInfo getClusterInfo(String map){
    
    map = mapNameResolver.get(map);
    
    Cluster cluster = Hazelcast.getCluster();
    ClusterInfo clusterInfo = new ClusterInfo();
    
    List<MemberInfo> extMembers = new ArrayList<MemberInfo>();
    for(Member member : cluster.getMembers()){
      
      MapInfo mapInfo = new MapInfo( getLocalMapStats(member, map));
      RuntimeInfo runtimeInfo = new RuntimeInfo( getMemberInfo(member));
      
      if (!member.localMember()) {
        extMembers.add(new MemberInfo(member, runtimeInfo, mapInfo));
      }
      else {
         clusterInfo.setLocalMember(new MemberInfo(member, runtimeInfo, mapInfo));
      }
    }
    
    clusterInfo.setOtherMembers(extMembers.toArray(new MemberInfo[extMembers.size()]));
    return clusterInfo;
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
 
  private static DistributedMemberInfoCallable.MemberInfo getMemberInfo(Member member){
    DistributedTask<DistributedMemberInfoCallable.MemberInfo> runtimeInfoTask =
        new DistributedTask<DistributedMemberInfoCallable.MemberInfo>(new DistributedMemberInfoCallable(), member);
    ExecutorService execService = Hazelcast.getExecutorService();
    execService.execute(runtimeInfoTask);
    try{
      return runtimeInfoTask.get();
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
