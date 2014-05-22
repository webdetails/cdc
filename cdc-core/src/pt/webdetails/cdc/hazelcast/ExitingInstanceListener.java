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
