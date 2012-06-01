/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Formatter;

import org.apache.commons.io.IOUtils;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.examples.TestApp;

public class CfgTestApp extends TestApp {

  public CfgTestApp(HazelcastInstance hazelcast, String[] args) {
    super(hazelcast);
    hazelcast.getConfig().setCheckCompatibility(false);//TODO
    if(args.length > 0){
      handleCommand("ns " + args[0]);
    }
  }
  
  protected void handleCommand(String command) {
    if(command == null) command = "";
    command = command.trim();
      
    if(command.equals("m.config")){
      String mapName = getMap().getName();
      MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(mapName);
      println(mapConfig.toString());
    }
    else if(command.equals("m.binKeys")){
      for(Object key: getMap().keySet()){
        println(key);
        byte[] bytes = serializeObject(key);
        println(toHexString(bytes));
        println("#######################");
      }
    }
    else if(command.equals("m.dumpKeys")){
      String fileName = getMap().getName() + "_" + System.currentTimeMillis() + ".txt";
      File dumpFile = new File(fileName);
      FileOutputStream fos = null;
      try{
        dumpFile.createNewFile();
        fos = new FileOutputStream(dumpFile);
        ArrayList<String> lines = new ArrayList<String>();
        for(Object key: getMap().keySet()){
          lines.add(key.toString());
        }
        IOUtils.writeLines(lines, System.getProperty("line.separator"), fos);
        println("keys dumped to " + dumpFile.getName());
      } catch (FileNotFoundException e) {
        println("");
      } catch (IOException e) {
        println("error creating file: " + e.getMessage());
      }
      finally{
        IOUtils.closeQuietly(fos);
      }
    }
    else super.handleCommand(command);
  }
  
  public static String toHexString(byte[] bytes) {
    Formatter formatter = new Formatter();
    for (byte b : bytes) {
      formatter.format("%02x", b);
    }
    return formatter.toString();
  }
  
  private byte[] serializeObject(Object obj){
    ByteArrayOutputStream bos = null;
    ObjectOutputStream out = null;
    try {
      bos = new ByteArrayOutputStream();
      out = new ObjectOutputStream(bos);
      out.writeObject(obj);
      out.flush();
      bos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      println(e);
      return null;
    }
    finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(bos);
    }
  }
  
  public static void main(String[] args) throws Exception {
    CfgTestApp testApp = new CfgTestApp(Hazelcast.getDefaultInstance(), args);
    testApp.start(args);
  }
  

}
