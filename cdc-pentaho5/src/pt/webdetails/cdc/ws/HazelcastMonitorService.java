/*!
* Copyright 2002 - 2014 Webdetails, a Pentaho company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

package pt.webdetails.cdc.ws;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletResponse;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberLeftException;
import com.hazelcast.monitor.DistributedMapStatsCallable;
import com.hazelcast.monitor.DistributedMapStatsCallable.MemberMapStat;
import com.hazelcast.monitor.DistributedMemberInfoCallable;
import com.hazelcast.monitor.LocalMapStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pt.webdetails.cdc.core.HazelcastManager;
import pt.webdetails.cdc.hazelcast.operations.DistributedRestart;
import pt.webdetails.cdc.hazelcast.operations.DistributedShutdown;
import pt.webdetails.cdc.plugin.CdcConfig;
import pt.webdetails.cdc.plugin.CdcUtil;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;
import pt.webdetails.cpf.utils.PluginIOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Path("cdc/api/services/HazelcastMonitorService")
public class HazelcastMonitorService {

  private static Log logger = LogFactory.getLog( HazelcastMonitorService.class );

  @GET
  @Path("/getClusterInfo")
  public void getClusterInfo( @Context HttpServletResponse response,
      @QueryParam("map") @DefaultValue("") String map ) throws IOException {

    CacheMap cacheMap = CacheMap.parse( map );
    if ( cacheMap == null ) {
      PluginIOUtils
          .writeOutAndFlush( response.getOutputStream(), Result.getError( "Map not found: " + map ).toString() );
      return;
    }

    switch ( cacheMap ) {
      case Cda:
        map = CdcConfig.CacheMaps.CDA_MAP;
        break;
      case Mondrian:
        map = CdcConfig.CacheMaps.MONDRIAN_MAP;
        break;
    }

    Cluster cluster = HazelcastManager.INSTANCE.getHazelcast().getCluster();
    ClusterInfo clusterInfo = new ClusterInfo();

    List<MemberInfo> extMembers = new ArrayList<MemberInfo>();
    for ( Member member : cluster.getMembers() ) {
      try {
        MapInfo mapInfo = new MapInfo( getLocalMapStats( member, map ) );
        RuntimeInfo runtimeInfo = new RuntimeInfo( getMemberInfo( member ) );

        if ( !member.localMember() ) {
          extMembers.add( new MemberInfo( member, runtimeInfo, mapInfo ) );
        } else {
          clusterInfo.setLocalMember( new MemberInfo( member, runtimeInfo, mapInfo ) );
        }
      } catch ( Exception e ) { //MemberLeftException etc
        logger.error( "Error getting map stats: ", e );
      }
    }

    clusterInfo.setOtherMembers( extMembers.toArray( new MemberInfo[extMembers.size()] ) );
    PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( clusterInfo ).toString() );
  }

  @GET
  @Path("/shutdownMember")
  public void shutdownMember( @Context HttpServletResponse response,
      @QueryParam("ip") @DefaultValue("") String ip, @QueryParam("port") Integer port ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    Member targetMember = getClusterMember( ip, port );

    if ( targetMember == null ) {
      Result.getError( "Member " + ip + ":" + port + " not found in cluster." );
    }

    DistributedTask<Boolean> distributedShutdown =
        new DistributedTask<Boolean>( new DistributedShutdown(), targetMember );

    ExecutorService execService = HazelcastManager.INSTANCE.getHazelcast().getExecutorService();
    execService.execute( distributedShutdown );

    try {
      Boolean result = distributedShutdown.get();
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( result ).toString() );
    } catch ( MemberLeftException e ) {
      //member will leave before being able to respond
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( e.getMessage() ).toString() );
    } catch ( Exception e ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
    }

  }

  @GET
  @Path("/isRunningFallback")
  public void isRunningFallback( @Context HttpServletResponse response ) throws IOException {
    PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
        new Result( Result.Status.OK, HazelcastManager.INSTANCE.isExtraInstanceActive() ).toString() );
  }

  private Member getClusterMember( String ip, int port ) {
    Cluster cluster = HazelcastManager.INSTANCE.getHazelcast().getCluster();
    InetSocketAddress addr;
    try {
      addr = new InetSocketAddress( InetAddress.getByName( ip ), port );
    } catch ( UnknownHostException e ) {
      return null;
    }
    for ( Member member : cluster.getMembers() ) {
      if ( member.getInetSocketAddress().equals( addr ) ) {
        return member;
      }
    }
    return null;
  }

  @GET
  @Path("/restartMember")
  public void restartMember( @Context HttpServletResponse response,
      @QueryParam("ip") @DefaultValue("") String ip, @QueryParam("port") Integer port ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    Member targetMember = getClusterMember( ip, port );

    if ( targetMember == null ) {
      Result.getError( "Member " + ip + ":" + port + " not found in cluster." );
    }

    DistributedTask<Boolean> distributedShutdown =
        new DistributedTask<Boolean>( new DistributedRestart(), targetMember );

    ExecutorService execService = HazelcastManager.INSTANCE.getHazelcast().getExecutorService();
    execService.execute( distributedShutdown );

    try {
      Boolean result = distributedShutdown.get();
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( result ).toString() );
    } catch ( Exception e ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
    }
  }


  private static LocalMapStats getLocalMapStats( Member member, String mapName )
      throws InterruptedException, ExecutionException {

    DistributedTask<MemberMapStat> mapStatTask =
        new DistributedTask<MemberMapStat>( new DistributedMapStatsCallable( mapName ),
            member );

    ExecutorService execService = HazelcastManager.INSTANCE.getHazelcast().getExecutorService();
    execService.execute( mapStatTask );

    MemberMapStat mapStat = mapStatTask.get();
    return mapStat.getLocalMapStats();
  }


  private static DistributedMemberInfoCallable.MemberInfo getMemberInfo( Member member ) {
    DistributedTask<DistributedMemberInfoCallable.MemberInfo> runtimeInfoTask =
        new DistributedTask<DistributedMemberInfoCallable.MemberInfo>( new DistributedMemberInfoCallable(), member );
    ExecutorService execService = HazelcastManager.INSTANCE.getHazelcast().getExecutorService();
    execService.execute( runtimeInfoTask );
    try {
      return runtimeInfoTask.get();
    } catch ( InterruptedException e ) {
      logger.error( "Timeout waiting for LocalMapStats on member " + member.getInetSocketAddress(), e );
    } catch ( ExecutionException e ) {
      logger.error( "Error waiting for LocalMapStats on member " + member.getInetSocketAddress(), e );
    }

    return null;
  }


}
