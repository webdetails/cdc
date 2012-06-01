/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.Hazelcast;

public class StartServer extends com.hazelcast.examples.StartServer {
  
  
  public static void main(String[] args) 
  {
      try{
        Hazelcast.getConfig().setCheckCompatibility(false);
        Hazelcast.getCluster();
      }
      catch(Exception e){
        System.out.println("LAUNCH EXIT:" + e);
        Runtime.getRuntime().exit(3);
      }
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
