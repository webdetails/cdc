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

package pt.webdetails.cdc.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Launches a non-lite Hazelcast instance.
 */
public class HazelcastSimpleLauncher implements IHazelcastLauncher {

  private static Log logger = LogFactory.getLog( HazelcastSimpleLauncher.class );

  HazelcastInstance hazelcast;
  Config hzConfig;

  public HazelcastSimpleLauncher( Config cfg ) {
    hzConfig = CoreHazelcastConfigHelper.clone( cfg );
    hzConfig.setLiteMember( false );
  }

  @Override
  public void start() {
    if ( hazelcast == null ) {
      //      boolean lite = hzConfig.isLiteMember();
      //      hzConfig.setLiteMember(false);
      logger.debug( "starting.." );
      hazelcast = Hazelcast.newHazelcastInstance( hzConfig );
      logger.debug( "started" );
      //      hzConfig.setLiteMember(lite);
    }
  }

  @Override
  public void stop() {
    if ( hazelcast != null ) {
      logger.debug( "shutting down" );
      hazelcast.getLifecycleService().shutdown();
      hazelcast = null;
    }
  }

  @Override
  public boolean isRunning() {
    boolean isRunning = hazelcast != null && hazelcast.getLifecycleService().isRunning();
    logger.debug( isRunning ? "running" : "-not running-" );
    return isRunning;
  }

  @Override
  public void setMaxMemory( String maxMem ) {
  }

}
