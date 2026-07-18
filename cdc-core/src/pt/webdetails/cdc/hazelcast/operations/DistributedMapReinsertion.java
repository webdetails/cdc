/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2002 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


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
