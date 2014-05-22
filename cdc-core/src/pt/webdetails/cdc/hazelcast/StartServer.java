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

package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class StartServer extends com.hazelcast.examples.StartServer {

  public static int ERR_INI = 1;
  public static int ERR_ILLEGAL_STATE = 2;
  public static int ERR_OTHER = 3;

  public static void main( String[] args ) {
    try {
      HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
      hazelcast.getConfig().setCheckCompatibility( false );
      hazelcast.getCluster();
      hazelcast.addInstanceListener( new ExitingInstanceListener( 3 ) );

      new Thread( null, new PulseChecker( hazelcast ), "check hazelcast death" ).start();
    } catch ( Exception e ) {
      System.out.println( "LAUNCH EXIT:" + e );
      Runtime.getRuntime().exit( ERR_INI );
    }
  }

  static class PulseChecker implements Runnable {
    HazelcastInstance hazelcast;

    public PulseChecker( HazelcastInstance hz ) {
      this.hazelcast = hz;
    }

    public void run() {
      boolean exit = false;
      while ( !exit ) {
        try {
          Thread.sleep( 5000 );
          try {
            if ( !hazelcast.getLifecycleService().isRunning() ) {
              System.exit( 5 );
            }
          } catch ( IllegalStateException e ) {
            System.out.println( e.getLocalizedMessage() );
            System.exit( ERR_ILLEGAL_STATE );
          } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( ERR_OTHER );
          }
        } catch ( InterruptedException e ) {
          System.exit( 0 );
        }
      }
    }
  }

}
