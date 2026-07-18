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

public class DistributedRestart extends DistributedInstanceOperation<Boolean> {

  private static final long serialVersionUID = 1L;

  @Override
  public Boolean call() throws Exception {
    getHazelcastInstance().getLifecycleService().restart();
    return true;
  }

}
