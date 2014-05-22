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
