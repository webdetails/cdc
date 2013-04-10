/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class StartServer extends com.hazelcast.examples.StartServer {

  public static int ERR_INI = 1;
  public static int ERR_ILLEGAL_STATE = 2;
  public static int ERR_OTHER = 3;

  public static void main(String[] args) 
  {
    try {
      HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
      hazelcast.getConfig().setCheckCompatibility(false);
      hazelcast.getCluster();
      hazelcast.addInstanceListener(new ExitingInstanceListener(3));
      
      new Thread(null, new PulseChecker(hazelcast), "check hazelcast death" ).start();
    }
    catch(Exception e){
      System.out.println("LAUNCH EXIT:" + e);
      Runtime.getRuntime().exit(ERR_INI);
    }
  }

  static class PulseChecker implements Runnable {
    HazelcastInstance hazelcast;
    public PulseChecker(HazelcastInstance hz) {
      this.hazelcast = hz;
    }
    public void run() {
      boolean exit = false;
      while(!exit){
        try {
          Thread.sleep(5000);
          try{
            if(!hazelcast.getLifecycleService().isRunning()){
              System.exit(5);
            }
          }
          catch(IllegalStateException e){
            System.out.println(e.getLocalizedMessage());
            System.exit(ERR_ILLEGAL_STATE);
          }
          catch(Exception e){
            e.printStackTrace();
            System.exit(ERR_OTHER);
          }
        } catch (InterruptedException e) {
          System.exit(0);
        }
      }
    }
  }
  
}
