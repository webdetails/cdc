/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2002 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


package pt.webdetails.cdc.hazelcast;

import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;

public class ExitingInstanceListener implements InstanceListener {

  private int exit = 1;

  public ExitingInstanceListener( int exitCode ) {
    exit = exitCode;
  }

  @Override
  public void instanceCreated( InstanceEvent arg0 ) {
  }

  @Override
  public void instanceDestroyed( InstanceEvent arg0 ) {
    System.out.println( ">> Exiting <<" );
    System.exit( exit );
  }

}
