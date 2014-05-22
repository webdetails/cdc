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

package pt.webdetails.cdc.hazelcast.operations;

import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEntry;
//import com.hazelcast.query.Predicate;
//import com.hazelcast.query.SqlPredicate;

public class DistributedMapReinsertion extends DistributedInstanceOperation<Integer> {

  private static final long serialVersionUID = 1L;

  private String mapName;

  public DistributedMapReinsertion( String mapName ) {
    this.mapName = mapName;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Integer call() throws Exception {
    if ( getHazelcastInstance().getConfig().isLiteMember() ) {
      return 0;
    }
    IMap map = getHazelcastInstance().getMap( mapName );
    int count = 0;
    for ( Object key : map.localKeySet() ) { //map.localKeySet(testp)) {
      MapEntry entry = map.getMapEntry( key );
      map.put( key, entry.getValue() ); //TODO: use/update ttl
      count++;
    }
    return count;
  }

}
