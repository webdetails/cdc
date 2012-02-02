package pt.webdetails.cdc.ws;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.ws.MapInfo;

import com.hazelcast.core.Member;

public class MemberInfo {

  private static Log logger = LogFactory.getLog(MemberInfo.class);
  
  private String address;
  private boolean isSuperClient;
//  private MapInfo cdaCacheInfo;
//  private MapInfo mondrianCacheInfo;
  private MapInfo mapInfo;
  
  private RuntimeInfo javaRuntimeInfo;
  
  public RuntimeInfo getJavaRuntimeInfo() {
    return javaRuntimeInfo;
  }

  public void setJavaRuntimeInfo(RuntimeInfo javaRuntimeInfo) {
    this.javaRuntimeInfo = javaRuntimeInfo;
  }
  
  public MemberInfo(){}
  
  public MemberInfo(Member member, RuntimeInfo runtimeInfo , MapInfo mapInfo) {

    if(member != null) {
      this.address = member.getInetSocketAddress().toString();
      this.isSuperClient = member.isSuperClient();
      
      //java runtime info
      this.javaRuntimeInfo = runtimeInfo;

      //map stats
      this.mapInfo = mapInfo;
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

  
  public MapInfo getMapInfo() {
    return mapInfo;
  }

  public void setMapInfo(MapInfo mapInfo) {
    this.mapInfo = mapInfo;
  }

  
}
