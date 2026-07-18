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


package pt.webdetails.cdc;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pt.webdetails.cdc.core.IHazelcastLauncher;
import pt.webdetails.cpf.bean.IBeanFactory;
import pt.webdetails.cpf.bean.AbstractBeanFactory;

public class BaseCdcEnvironment implements ICdcEnvironment {

  private static Log logger = LogFactory.getLog( BaseCdcEnvironment.class );

  private IBeanFactory beanFactory;

  public BaseCdcEnvironment() {
    init();
  }

  @Override
  public void init() {
    beanFactory = new AbstractBeanFactory(){
      @Override
      public String getSpringXMLFilename(){ return "cdc.spring.xml"; }
    };
  }

  @Override
  public IHazelcastLauncher getInnerHazelcastProccessLauncher() {
    try {
      String id = "IHazelcastLauncher";
      if ( beanFactory.containsBean( id ) ) {
        return (IHazelcastLauncher) beanFactory.getBean( id );
      }
    } catch ( Exception e ) {
      logger.error( "Cannot get bean IHazelcastLauncher", e );
    }
    return null;
  }
}
