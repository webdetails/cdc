/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.ws;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.CdcConfig;
import pt.webdetails.cdc.hazelcast.operations.DistributedRestart;
import pt.webdetails.cdc.hazelcast.operations.DistributedShutdown;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberLeftException;
import com.hazelcast.monitor.DistributedMapStatsCallable;
import com.hazelcast.monitor.DistributedMemberInfoCallable;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.DistributedMapStatsCallable.MemberMapStat;

public class HazelcastMonitorService {
  
  private static Log logger = LogFactory.getLog(HazelcastMonitorService.class);
  
  public String getClusterInfo(String map){
    
    CacheMap cacheMap = CacheMap.parse(map);
    if(cacheMap == null){
      return Result.getError("Map not found: " + map).toString();
    }
    
    switch(cacheMap){
      case Cda:
        map = CdcConfig.CacheMaps.CDA_MAP;
        break;
      case Mondrian:
        map = CdcConfig.CacheMaps.MONDRIAN_MAP;
        break;
    }
    
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
    return Result.getOK(clusterInfo).toString();
  }
  
  public String shutdownMember(String ip, int port){
    
    Member targetMember = getClusterMember(ip, port);

    if(targetMember == null) Result.getError("Member " + ip + ":" + port + " not found in cluster.");
    
    DistributedTask<Boolean> distributedShutdown =
        new DistributedTask<Boolean>(new DistributedShutdown(), targetMember);
    
    ExecutorService execService = Hazelcast.getExecutorService();
    execService.execute(distributedShutdown);
    
    try 
    {
      Boolean result = distributedShutdown.get();
      return Result.getOK(result).toString();
    } 
    catch(MemberLeftException e){
      //member will leave before being able to respond
      return Result.getOK(e.getMessage()).toString();
    }
    catch (Exception e) 
    {
      return Result.getFromException(e).toString();
    }
    
  }
  
  private Member getClusterMember(String ip, int port){
    Cluster cluster = Hazelcast.getCluster();
    InetSocketAddress addr;
    try {
      addr = new InetSocketAddress(InetAddress.getByName(ip), port);
    } catch (UnknownHostException e) {
      return null;
    }
    for(Member member : cluster.getMembers()){
      if(member.getInetSocketAddress().equals( addr )){
        return member;
      }
    }
    return null;
  }
  
  public String restartMember(String ip, int port){
    Member targetMember = getClusterMember(ip, port);

    if(targetMember == null) Result.getError("Member " + ip + ":" + port + " not found in cluster.");
    
    DistributedTask<Boolean> distributedShutdown =
        new DistributedTask<Boolean>(new DistributedRestart(), targetMember);
    
    ExecutorService execService = Hazelcast.getExecutorService();
    execService.execute(distributedShutdown);
    
    try 
    {
      Boolean result = distributedShutdown.get();
      return Result.getOK(result).toString();
    } 

    catch (Exception e) 
    {
      return Result.getFromException(e).toString();
    }
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
