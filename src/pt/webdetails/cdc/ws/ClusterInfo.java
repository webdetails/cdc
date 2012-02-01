package pt.webdetails.cdc.ws;

import java.util.ArrayList;
import java.util.List;

import pt.webdetails.cdc.ws.MemberInfo;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.Member;


public class ClusterInfo {
  
  private MemberInfo localMember;
  private MemberInfo[] otherMembers;
  
  public ClusterInfo(){}

  public ClusterInfo(Cluster cluster) {
    
    List<MemberInfo> extMembers = new ArrayList<MemberInfo>();
    for(Member member : cluster.getMembers()){
      if (!member.localMember()) {
        extMembers.add(new MemberInfo(member));
      }
      else {
        localMember = new MemberInfo(member);
      }
    }
    
    otherMembers = extMembers.toArray(new MemberInfo[extMembers.size()]);
  }

  public MemberInfo getLocalMember() {
    return localMember;
  }

  public void setLocalMember(MemberInfo localMember) {
    this.localMember = localMember;
  }

  public MemberInfo[] getOtherMembers() {
    return otherMembers;
  }

  public void setOtherMembers(MemberInfo[] otherMembers) {
    this.otherMembers = otherMembers;
  }
  
  

}
