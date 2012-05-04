package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.Hazelcast;

public class StartServer extends com.hazelcast.examples.StartServer {
  
  
  public static void main(String[] args) 
  {
//    try{
//      //be more permissive
      try{
        Hazelcast.getConfig().setCheckCompatibility(false);
        Hazelcast.getCluster();
      }
      catch(Exception e){
        System.out.println("LAUNCH EXIT:" + e);
        Runtime.getRuntime().exit(3);
      }
//    }
//    catch(Exception e){
//      System.err.println(e);
//      System.exit(1);
//    }
    Hazelcast.addInstanceListener(new ExitingInstanceListener(3));
    new Thread(null, new Runnable(){

      @Override
      public void run() {
        boolean exit = false;
        while(!exit){
          try {
            Thread.sleep(5000);
            try{
              if(!Hazelcast.getLifecycleService().isRunning()){
                System.exit(5);
              }
//              Hazelcast.getMap("cdaCache");
            }
            catch(IllegalStateException e){
              System.out.println("ILLEGAL STATE EXCEPTION EXIT");
              System.exit(5);
            }
            catch(Exception e){
              System.out.println("EXCEPTION EXIT");
              System.exit(7);
            }
          } catch (InterruptedException e) {
            System.out.println("Interrupted!");
            System.exit(0);
          }
        }
      }
      
    }, "check hazelcast death" ).start();
  }

  
  
}
