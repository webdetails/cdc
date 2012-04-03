package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.Hazelcast;

public class StartServer extends com.hazelcast.examples.StartServer {
  
  
  public static void main(String[] args) 
  {
    //be more permissive
    Hazelcast.getConfig().setCheckCompatibility(false);
    Hazelcast.getCluster();
  }

  
  
}
