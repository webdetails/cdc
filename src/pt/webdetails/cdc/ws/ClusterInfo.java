/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.ws;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pt.webdetails.cdc.ws.MemberInfo;
import pt.webdetails.cpf.JsonSerializable;

public class ClusterInfo implements JsonSerializable {
  
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

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("localMember", localMember.toJSON());
    JSONArray others = new JSONArray();
    for(MemberInfo other : otherMembers){
      others.put(other.toJSON());
    }
    obj.put("otherMembers", others);
    return obj;
  }
  
  

}
