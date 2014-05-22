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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import pt.webdetails.cpf.messaging.JsonSerializable;

import com.hazelcast.core.Member;

public class MemberInfo implements JsonSerializable {

  private static Log logger = LogFactory.getLog( MemberInfo.class );

  private String address;
  private boolean liteMember;

  private MapInfo mapInfo;

  private RuntimeInfo javaRuntimeInfo;

  public RuntimeInfo getJavaRuntimeInfo() {
    return javaRuntimeInfo;
  }

  public void setJavaRuntimeInfo( RuntimeInfo javaRuntimeInfo ) {
    this.javaRuntimeInfo = javaRuntimeInfo;
  }

  public MemberInfo() {
  }

  public MemberInfo( Member member, RuntimeInfo runtimeInfo, MapInfo mapInfo ) {

    if ( member != null ) {
      this.address = member.getInetSocketAddress().toString();
      this.liteMember = member.isLiteMember();

      //java runtime info
      this.javaRuntimeInfo = runtimeInfo;

      //map stats
      this.mapInfo = mapInfo;
    } else {
      logger.error( "Member NULL" );
    }
  }

  public String getAddress() {
    return address;
  }

  public void setAddress( String address ) {
    this.address = address;
  }

  public boolean isLiteMember() {
    return liteMember;
  }

  public void setLiteMember( boolean isLiteMember ) {
    this.liteMember = isLiteMember;
  }


  public MapInfo getMapInfo() {
    return mapInfo;
  }

  public void setMapInfo( MapInfo mapInfo ) {
    this.mapInfo = mapInfo;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject result = new JSONObject();
    result.put( "address", this.address );
    result.put( "isSuperClient", this.liteMember );
    result.put( "mapInfo", this.mapInfo != null ? this.mapInfo.toJSON() : null );
    result.put( "javaRuntimeInfo", this.javaRuntimeInfo != null ? this.javaRuntimeInfo.toJSON() : null );
    return result;
  }


}
