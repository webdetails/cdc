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

import org.json.JSONException;
import org.json.JSONObject;

import pt.webdetails.cpf.messaging.JsonSerializable;

import com.hazelcast.monitor.DistributedMemberInfoCallable;

public class RuntimeInfo implements JsonSerializable {

  private long totalMemory;
  private long freeMemory;
  private long maxMemory;

  public RuntimeInfo( DistributedMemberInfoCallable.MemberInfo mInfo ) {
    if ( mInfo == null ) {
      return;
    }

    totalMemory = mInfo.getTotalMemory();
    freeMemory = mInfo.getFreeMemory();
    maxMemory = mInfo.getMaxMemory();
  }

  public long getTotalMemory() {
    return totalMemory;
  }

  public void setTotalMemory( long totalMemory ) {
    this.totalMemory = totalMemory;
  }

  public long getFreeMemory() {
    return freeMemory;
  }

  public void setFreeMemory( long freeMemory ) {
    this.freeMemory = freeMemory;
  }

  public long getMaxMemory() {
    return maxMemory;
  }

  public void setMaxMemory( long maxMemory ) {
    this.maxMemory = maxMemory;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject json = new JSONObject();
    json.put( "totalMemory", totalMemory );
    json.put( "freeMemory", freeMemory );
    json.put( "maxMemory", maxMemory );
    return json;
  }


}
