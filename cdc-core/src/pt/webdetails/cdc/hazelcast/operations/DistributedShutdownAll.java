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


package pt.webdetails.cdc.hazelcast.operations;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.hazelcast.core.Hazelcast;

/**
 * Shutdown every instance in JVM
 */
public class DistributedShutdownAll implements Callable<Boolean>, Serializable {

  private static final long serialVersionUID = 1L;

  @Override
  public Boolean call() throws Exception {
    Hazelcast.shutdownAll();
    return true;
  }

}
