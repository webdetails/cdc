/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast;

public class DistributedRestart extends DistributedInstanceOperation<Boolean> {

  private static final long serialVersionUID = 1L;

  @Override
  public Boolean call() throws Exception {
    getHazelcastInstance().getLifecycleService().restart();
    return true;
  }

}
