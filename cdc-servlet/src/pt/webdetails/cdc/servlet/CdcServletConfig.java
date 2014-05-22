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

package pt.webdetails.cdc.servlet;

import javax.servlet.ServletConfig;

import org.apache.commons.lang.StringUtils;

import pt.webdetails.cdc.core.ICdcConfig;

// TODO: option names need to be more similar to CdcConfig 
public class CdcServletConfig implements ICdcConfig {

  private ServletConfig servletConfig;

  public CdcServletConfig( ServletConfig config ) {
    this.servletConfig = config;
  }

  public String getHazelcastConfigFile() {
    String configFile = servletConfig.getInitParameter( "hazelcast.config" );
    if ( StringUtils.isEmpty( configFile ) ) {
      configFile = "hazelcast.xml";
    }
    return configFile;
  }

  public boolean enableShutdownThread() {
    return false;
  }

  public boolean isLiteMode() {
    String isLiteMode = servletConfig.getInitParameter( "hazelcast.lite.member" );
    if ( !StringUtils.isEmpty( isLiteMode ) ) {
      return Boolean.parseBoolean( isLiteMode );
    } else {
      return false;
    }
  }

  public boolean isDebugMode() {
    String debugMode = servletConfig.getInitParameter( "webdetails.cdc.debug" );
    if ( !StringUtils.isEmpty( debugMode ) ) {
      return Boolean.parseBoolean( debugMode );
    } else {
      return false;
    }
  }

  public boolean isForceConfig() {
    return false;
  }

  public boolean isMondrianCdcEnabled() {
    // just assuming it is
    return true;
  }

  public boolean isSyncCacheOnStart() {
    return false;
  }

  public String getVmMemory() {
    // disabled, depends on master
    return "512m";
  }

  public boolean isMaster() {
    // will always default to slave for now, it doesn't do any master stuff anyway
    return false;
  }

  public boolean isSyncConfig() {
    return false;
  }

  public boolean isLaunchInnerProc() {
    return false;
  }

  public boolean isRegisterMondrian() { //TODO:
    return true;
  }

}
