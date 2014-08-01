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
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import org.json.JSONObject;
import org.pentaho.platform.api.engine.IParameterProvider;

import org.pentaho.platform.util.messages.LocaleHelper;

import pt.webdetails.cdc.core.HazelcastManager;
import pt.webdetails.cdc.ws.MondrianCacheCleanService;
import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.VersionChecker;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;
import pt.webdetails.cpf.olap.OlapUtils;

public class CdcContentGenerator extends SimpleContentGenerator {

  private static final long serialVersionUID = 1L;

  private static final String UI_PATH = "cdc/presentation/";

  private static Map<String, Method> exposedMethods = new HashMap<String, Method>();

  static {
    //to keep case-insensitive methods
    exposedMethods = getExposedMethods( CdcContentGenerator.class, true );
  }

  @Override
  protected Method getMethod( String methodName ) throws NoSuchMethodException {
    Method method = exposedMethods.get( StringUtils.lowerCase( methodName ) );
    if ( method == null ) {
      throw new NoSuchMethodException();
    }
    return method;
  }

  @Override
  public String getPluginName() {
    return "cdc";
  }

  //TODO: testing
  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void start( OutputStream out ) throws Exception {
    HazelcastManager.INSTANCE.init();
    writeOut( out, "OK?" );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void stop( OutputStream out ) throws Exception {
    HazelcastManager.INSTANCE.tearDown();
    writeOut( out, "OK?" );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void recoverMondrianCache( OutputStream out ) throws Exception {
    MondrianCacheCleanService.loadMondrianCatalogs();
    int cnt = HazelcastManager.INSTANCE.reloadMondrianCache();
    writeOut( out, "reload: " + cnt );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void reloadMondrianCache( OutputStream out ) throws Exception {
    int cnt = HazelcastManager.INSTANCE.reloadMondrianCache();
    writeOut( out, "reload: " + cnt );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void home( OutputStream out ) throws IOException {
    Map<String, Object> params = getRenderRequestParameters( "cdcHome.wcdf" );
    renderInCde( out, params );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void clusterInfo( OutputStream out ) throws IOException {
    Map<String, Object> params = getRenderRequestParameters( "cdcClusterInfo.wcdf" );
    renderInCde( out, params );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void cacheInfo( OutputStream out ) throws IOException {
    Map<String, Object> params = getRenderRequestParameters( "cdcCacheInfo.wcdf" );
    renderInCde( out, params );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void settings( OutputStream out ) throws IOException {
    Map<String, Object> params = getRenderRequestParameters( "cdcSettings.wcdf" );
    renderInCde( out, params );
  }

  @Exposed(accessLevel = AccessLevel.ADMIN)
  public void cacheClean( OutputStream out ) throws IOException {
    Map<String, Object> params = getRenderRequestParameters( "cdcCacheClean.wcdf" );
    renderInCde( out, params );
  }

  @Exposed(accessLevel = AccessLevel.PUBLIC)
  public void about( OutputStream out ) throws IOException {
    renderInCde( out, getRenderRequestParameters( "cdcAbout.wcdf" ) );
  }

  @Exposed(accessLevel = AccessLevel.PUBLIC)
  public void olapUtils( OutputStream out ) {

    logger.debug( "Current user locale is " + LocaleHelper.getLocale() );
    OlapUtils utils = new OlapUtils();
    IParameterProvider requestParams = getRequestParameters();
    String operation = requestParams.getStringParameter( "operation", "-" );
    JSONObject json = new JSONObject();
    try {

      if ( operation.equals( "GetOlapCubes" ) ) {
        int i = 33;
        json = utils.getOlapCubes();

      } else if ( operation.equals( "GetCubeStructure" ) ) {
        String catalog = requestParams.getStringParameter( "catalog", null );
        String cube = requestParams.getStringParameter( "cube", null );
        String jndi = requestParams.getStringParameter( "jndi", null );
        json = utils.getCubeStructure( catalog, cube, jndi );
      } else if ( operation.equals( "GetLevelMembersStructure" ) ) {
        String catalog = requestParams.getStringParameter( "catalog", null );
        String cube = requestParams.getStringParameter( "cube", null );
        String member = requestParams.getStringParameter( "member", null );
        String direction = requestParams.getStringParameter( "direction", null );
        json = utils.getLevelMembersStructure( catalog, cube, member, direction );
      } else {
        json.put( "result", "Unknown operation: \"" + operation + "\"" );
        json.put( "status", "false" );
      }

      out.write( json.toString().getBytes() );
    } catch ( IOException e ) {
      logger.error( e );
    } catch ( JSONException e ) {
      logger.error( e );
    }
  }

  @Exposed(accessLevel = AccessLevel.PUBLIC)
  public void checkVersion( OutputStream out ) throws IOException, JSONException {
    writeOut( out, getVersionChecker().checkVersion() );
  }

  @Exposed(accessLevel = AccessLevel.PUBLIC)
  public void getVersion( OutputStream out ) throws IOException, JSONException {
    writeOut( out, getVersionChecker().getVersion() );
  }

  /**
   * Set up parameters to render a dashboard from the presentation layer
   *
   * @param dashboardName
   * @return
   */
  private Map<String, Object> getRenderRequestParameters( String dashboardName ) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put( "solution", "system" );
    params.put( "path", UI_PATH );
    params.put( "file", dashboardName );
    params.put( "bypassCache", "true" );
    params.put( "absolute", "true" );
    params.put( "inferScheme", "true" );
    params.put( "root", getRoot() );

    //add request parameters
    ServletRequest request = getRequest();
    @SuppressWarnings("unchecked")//should always be String
        Enumeration<String> originalParams = request.getParameterNames();
    // Iterate and put the values there
    while ( originalParams.hasMoreElements() ) {
      String originalParam = originalParams.nextElement();
      params.put( originalParam, request.getParameter( originalParam ) );
    }

    return params;
  }

  /**
   * Display a CDE dashboard
   *
   * @param out
   * @param params
   * @throws IOException
   */
  private void renderInCde( OutputStream out, Map<String, Object> params ) throws IOException {
    InterPluginCall pluginCall = new InterPluginCall( InterPluginCall.CDE, "Render", params );
    pluginCall.setResponse( getResponse() );
    pluginCall.setOutputStream( out );
    pluginCall.run();
  }

  private String getRoot() {

    ServletRequest wrapper = getRequest();
    String root =  wrapper.getServerName() + ":" + wrapper.getServerPort();

    return root;
  }

  @Override
  protected String getDefaultPath( String path ) {
    if ( StringUtils.endsWith( path, "/" ) ) {
      return "home";
    } else {
      return "cdc/home";
    }
  }

  public VersionChecker getVersionChecker() {

    return new VersionChecker( CdcConfig.getConfig() ) {

      @Override
      protected String getVersionCheckUrl( VersionChecker.Branch branch ) {
        switch ( branch ) {
          case TRUNK:
            return "http://ci.pentaho.com/job/pentaho-cdc/lastSuccessfulBuild/artifact/cdc-pentaho/dist/marketplace.xml";
          case STABLE:
            return "http://ci.analytical-labs"
                + ".com/job/Webdetails-CDC-Release/lastSuccessfulBuild/artifact/dist/marketplace.xml";
          default:
            return null;
        }

      }

    };
  }
}
