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

package pt.webdetails.cdc;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pt.webdetails.cdc.bean.factory.CoreBeanFactory;
import pt.webdetails.cdc.bean.factory.ICdcBeanFactory;
import pt.webdetails.cdc.core.IHazelcastLauncher;

public class BaseCdcEnvironment implements ICdcEnvironment {

  private static Log logger = LogFactory.getLog( BaseCdcEnvironment.class );

  private ICdcBeanFactory beanFactory;

  public BaseCdcEnvironment() {
    init();
  }

  @Override
  public void init() {
    beanFactory = new CoreBeanFactory();
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
