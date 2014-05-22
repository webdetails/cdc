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

package pt.webdetails.cdc.bean.factory;


import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URL;

public class CoreBeanFactory implements ICdcBeanFactory {

  public static final String CDC_SPRING_BEAN = "cdc.spring.xml";

  private static final Log logger = LogFactory.getLog( CoreBeanFactory.class );

  protected static ConfigurableApplicationContext context;

  public CoreBeanFactory() {
    context = getSpringBeanFactory( CDC_SPRING_BEAN );
  }

  public CoreBeanFactory( String config ) {
    context = getSpringBeanFactory( config );
  }

  @Override
  public Object getBean( String id ) {
    if ( context.containsBean( id ) ) {
      return context.getBean( id );
    }
    return null;
  }

  @Override
  public boolean containsBean( String id ) {
    if ( context != null ) {
      return context.containsBean( id );
    }
    return false;
  }

  @Override
  public String[] getBeanNamesForType( @SuppressWarnings("rawtypes") Class clazz ) {
    return context.getBeanNamesForType( clazz );
  }

  protected ConfigurableApplicationContext getSpringBeanFactory( String config ) {
    try {
      final ClassLoader cl = this.getClass().getClassLoader();
      URL url = cl.getResource( config );
      if ( url != null ) {
        logger.debug( "Found spring file @ " + url ); //$NON-NLS-1$
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext( config ) {
          @Override
          protected void initBeanDefinitionReader(
              XmlBeanDefinitionReader beanDefinitionReader ) {

            beanDefinitionReader.setBeanClassLoader( cl );
          }

          @Override
          protected void prepareBeanFactory(
              ConfigurableListableBeanFactory clBeanFactory ) {
            super.prepareBeanFactory( clBeanFactory );
            clBeanFactory.setBeanClassLoader( cl );
          }

          /**
           * Critically important to override this and return the desired
           * CL
           **/
          @Override
          public ClassLoader getClassLoader() {
            return cl;
          }
        };
        return context;
      }
    } catch ( Exception e ) {
      logger.fatal( "Error loading " + CDC_SPRING_BEAN, e );
    }
    logger.fatal(
        "Spring definition file does not exist. There should be a <plugin_name>.spring.xml file on the classpath " );
    return null;

  }
}
