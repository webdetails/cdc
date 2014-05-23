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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pt.webdetails.cpf.messaging.JsonSerializable;

public class ClusterInfo implements JsonSerializable {

  private MemberInfo localMember;
  private MemberInfo[] otherMembers;

  public ClusterInfo() {
  }

  public MemberInfo getLocalMember() {
    return localMember;
  }

  public void setLocalMember( MemberInfo localMember ) {
    this.localMember = localMember;
  }

  public MemberInfo[] getOtherMembers() {
    return otherMembers;
  }

  public void setOtherMembers( MemberInfo[] otherMembers ) {
    this.otherMembers = otherMembers;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put( "localMember", localMember.toJSON() );
    JSONArray others = new JSONArray();
    for ( MemberInfo other : otherMembers ) {
      others.put( other.toJSON() );
    }
    obj.put( "otherMembers", others );
    return obj;
  }


}
