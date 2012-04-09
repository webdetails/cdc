package pt.webdetails.cdc.hazelcast;

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
    if(command != null && command.trim().equals("m.config")){
      String mapName = getMap().getName();
      MapConfig mapConfig = Hazelcast.getConfig().getMapConfig(mapName);
      println(mapConfig.toString());
    }
    else super.handleCommand(command);
  }
  
  public static void main(String[] args) throws Exception {
    CfgTestApp testApp = new CfgTestApp(Hazelcast.getDefaultInstance(), args);
    //Hazelcast.addInstanceListener(new ExitingInstanceListener(3));
    testApp.start(args);
    
//    new Thread(new Runnable(){
//
//      @Override
//      public void run() {
//        boolean exit = false;
//        while(!exit){
//          try {
//            Thread.sleep(5000);
//            try{
//              Hazelcast.getMap("cdaCache");
//              Hazelcast.getMap("mondrian");
//            }
//            catch(IllegalStateException e){
//              System.exit(3);
//            }
//          } catch (InterruptedException e) {
//            System.exit(0);
//          }
//        }
//        
//
//      }
//      
//    } ).start();
//    
  }
  

}
