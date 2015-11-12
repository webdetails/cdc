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

package pt.webdetails.cdc.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import com.hazelcast.core.Hazelcast;

/**
 * This class is needed only as a workaround solution while BISERVER-12931 is not fixed. Once BISERVER-12931 is fixed
 * you can remove this class. When ContextClosedEvent which happens on shutdown is caught, the CdcShutdownListener will
 * shut down all hazelcast instances running on the same JVM.
 * 
 * @author Joao L. M. Pereira (Joao.Pereira{[at]}pentaho.com)
 * @version 1.0
 */
public class CdcShutdownListener implements ApplicationListener, Ordered {
  static Log logger = LogFactory.getLog( CdcShutdownListener.class );
  private int order = Ordered.LOWEST_PRECEDENCE;

  @Override
  public void onApplicationEvent( final ApplicationEvent event ) {
    if ( event instanceof org.springframework.context.event.ContextClosedEvent ) {
      logger.info( "Shutdown ALL local Hazelcast instances!!" );
      Hazelcast.shutdownAll();
    }
  }

  @Override
  public int getOrder() {
    return order;
  }
}
