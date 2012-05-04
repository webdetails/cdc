/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.hazelcast.operations;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

public abstract class DistributedInstanceOperation<E> implements Callable<E>, HazelcastInstanceAware, Serializable {

  private static final long serialVersionUID = 1L;
  private transient HazelcastInstance hazelcastInstance;
  
  @Override
  public void setHazelcastInstance(HazelcastInstance hazelcastInstance) 
  {
    this.hazelcastInstance = hazelcastInstance;
  }
  
  protected HazelcastInstance getHazelcastInstance()
  {
    return this.hazelcastInstance;
  }

}
