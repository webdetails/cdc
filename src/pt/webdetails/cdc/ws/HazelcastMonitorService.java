package pt.webdetails.cdc.ws;

import com.hazelcast.core.Hazelcast;

public class HazelcastMonitorService {
  
  
  public ClusterInfo getClusterInfo(){
    return new ClusterInfo(Hazelcast.getCluster());
  }
//  
//  public MemberInfo getMemberDetails(){
//    
//  }
  
}
