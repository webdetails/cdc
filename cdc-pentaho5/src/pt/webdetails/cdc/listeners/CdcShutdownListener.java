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
