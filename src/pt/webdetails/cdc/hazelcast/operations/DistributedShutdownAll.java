/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

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
