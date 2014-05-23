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

package pt.webdetails.cdc.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dom4j.Element;

import pt.webdetails.cdc.core.ICdcConfig;
import pt.webdetails.cpf.PluginSettings;

public class GenericCdcConfig extends PluginSettings implements ICdcConfig {
  public static final String HAZELCAST_FILE = "hazelcast.xml";
  private static final String HAZELCAST_STANDALONE_FILE = "hazelcast-standalone.xml";
  private static Log logger = LogFactory.getLog( GenericCdcConfig.class );


  private static GenericCdcConfig instance;

  protected GenericCdcConfig() {
    super( CdcEnvironment.getInstance().getContentAccessFactory().getPluginSystemWriter( "" ) );
  }

  public static GenericCdcConfig getConfig() {
    if ( instance == null ) {
      instance = new GenericCdcConfig();
    }
    return instance;
  }

  public String getPluginName() {
    return "cdc";
  }
  /* ************
   * Config Items
   * start */

  public String getHazelcastConfigFile() {
    String cfg = getStringSetting( "hazelcastConfigFile", StringUtils.EMPTY );
    if ( StringUtils.isEmpty( cfg ) ) {
      return getPathFromSystemReader( HAZELCAST_FILE );
    } else {
      return cfg;
    }
  }

  public static String getHazelcastStandaloneConfigFile() {
    return getPathFromSystemReader( HAZELCAST_STANDALONE_FILE );
  }

  //TODO: what was this?
  public boolean enableShutdownThread() {
    return getBooleanSetting( "enableShutdownThread", false );
  }

  public boolean isLiteMode() {
    return getBooleanSetting( "liteMode", true );
  }

  public boolean isDebugMode() {
    return getBooleanSetting( "debugMode", false );
  }

  public boolean isForceConfig() {
    return getBooleanSetting( "forceConfig", false );
  }

  public String getCdaConfigLocation() {
    return getStringSetting( "cdaConfig/location", "system/cda/plugin.xml" );
  }

  public String getCdaCacheBeanId() {
    return getStringSetting( "cdaConfig/beanID", "cda.IQueryCache" );
  }

  public boolean isMondrianCdcEnabled() {
    return getBooleanSetting( "mondrianConfig/enabled", false );
  }

  public void setMondrianCdcEnabled( boolean enabled ) {
    if ( !writeSetting( "mondrianConfig/enabled", "" + enabled ) ) {
      logger.error( "Could not write property mondrianConfig/enabled" );
    }
  }

  @Override
  public boolean isSyncCacheOnStart() {
    return getBooleanSetting( "mondrianConfig/syncCacheOnStart", false );
  }

  public String getCdaHazelcastAdapterClass() {
    return getStringSetting( "cdaConfig/adapterClasses/hazelcast", "pt.webdetails.cda.cache.HazelcastQueryCache" );
  }

  public String getCdaDefaultAdapterClass() {
    return getStringSetting( "cdaConfig/adapterClasses/default", StringUtils.EMPTY );
  }

  public String getVmMemory() {
    return getStringSetting( "vmMemory", "512m" );
  }

  public boolean isMaster() {
    return getBooleanSetting( "master", true );
  }

  public boolean isAsyncInit() {
    return getBooleanSetting( "asyncInit", false );
  }

  public List<String> getLocales() {
    List<Element> localesXml = getSettingsXmlSection( "locales/locale" );

    List<String> localesAsStr = new ArrayList<String>( localesXml.size() );

    for ( Element e : localesXml ) {
      localesAsStr.add( e.getText() );
    }

    return localesAsStr;

  }

  public boolean isSyncConfig() {
    return true;
  }

  public boolean isLaunchInnerProc() {
    return false;
  }

  public boolean isRegisterMondrian() {
    return isMondrianCdcEnabled();
  }
  /* end *
   * config items
   * ************/

  public static String getPathFromSystemReader( String path ) {
    String str = CdcEnvironment.getInstance().getContentAccessFactory().getPluginSystemReader( path ).toString();
    if ( str.contains( "SystemPluginResourceAccess:" ) ) {
      return str.replace( "SystemPluginResourceAccess:", "" );
    } else {
      return str;
    }
  }

}
