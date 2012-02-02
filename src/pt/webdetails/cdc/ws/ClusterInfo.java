package pt.webdetails.cdc.ws;

import pt.webdetails.cdc.ws.MemberInfo;

public class ClusterInfo {
  
  private MemberInfo localMember;
  private MemberInfo[] otherMembers;
  
  public ClusterInfo(){}

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
