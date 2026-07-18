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


package pt.webdetails.cdc.core;

/**
 * Launches a non-lite hazelcast instance.
 */
public interface IHazelcastLauncher {

  public abstract void start();

  public abstract void stop();

  public abstract boolean isRunning();

  public void setMaxMemory( String maxMem );

}
