/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.ws;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import pt.webdetails.cdc.ws.MapInfo;

import com.hazelcast.core.Member;

public class MemberInfo implements JsonSerializable{

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

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject result = new JSONObject();
    result.put("address", this.address);
    result.put("isSuperClient", this.isSuperClient);
    result.put("mapInfo", this.mapInfo != null ? this.mapInfo.toJSON() : null);
    result.put("javaRuntimeInfo", this.javaRuntimeInfo != null ? this.javaRuntimeInfo.toJSON() : null);
    return result;
  }

  
  
}
