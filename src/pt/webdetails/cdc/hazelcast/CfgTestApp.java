package pt.webdetails.cdc.hazelcast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
