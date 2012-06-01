/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast.operations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.hazelcast.config.MapConfig;

public class DistributedMapConfig extends DistributedInstanceOperation<Boolean> implements Serializable {

  //TODO:revert back to non-blob version, problem wasn't serialization
  
  private static final long serialVersionUID = 1L;
//  private MapConfig newMapConfig; 
  protected byte[] mapConfigBlob;
  
  DistributedMapConfig(){}
  
  public DistributedMapConfig(MapConfig mapConfig){
    mapConfigBlob = serializeMapConfig(mapConfig);
  }
  
  @Override
  public Boolean call() throws Exception {
    MapConfig newMapConfig = deserializeMapConfig(mapConfigBlob);
    String mapName = newMapConfig.getName();
    Map<String,MapConfig> mapConfigs = getHazelcastInstance().getConfig().getMapConfigs();
    HashMap<String, MapConfig> newMapConfigs = new HashMap<String, MapConfig>(mapConfigs.size());
    newMapConfigs.putAll(mapConfigs);
    newMapConfigs.put(newMapConfig.getName(), newMapConfig);
    //mapConfigs.put(mapName, newMapConfig);
    getHazelcastInstance().getConfig().setMapConfigs(newMapConfigs);
    return getHazelcastInstance().getConfig().getMapConfig(mapName).isCompatible(newMapConfig);
  }

  
  private static MapConfig deserializeMapConfig(byte[] bytes) {
    try {
      ByteArrayInputStream byteInStream = new ByteArrayInputStream(bytes);
      DataInputStream in = new DataInputStream(byteInStream);
      MapConfig mapConfigNew = new MapConfig();
      mapConfigNew.readData(in);
      in.close();
      byteInStream.close();
      return mapConfigNew;
    } catch (IOException e) {
      return null;
    }
  }

  private static byte[] serializeMapConfig(MapConfig mapConfig)  {
    try {
      ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(byteOutStream);
      mapConfig.writeData(out);
      out.flush();
      out.close();
      byte[] bytes = byteOutStream.toByteArray();
      byteOutStream.close();
      return bytes;
    } catch (Exception e) {
      return new byte[0];
    }
  }

}
