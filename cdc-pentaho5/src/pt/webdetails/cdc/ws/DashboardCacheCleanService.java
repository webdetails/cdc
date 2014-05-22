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

package pt.webdetails.cdc.ws;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import pt.webdetails.cdc.plugin.CdcUtil;
import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.PentahoBasePluginEnvironment;
import pt.webdetails.cpf.PluginEnvironment;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;
import pt.webdetails.cpf.plugin.CorePlugin;
import pt.webdetails.cpf.plugincall.api.IPluginCall;
import pt.webdetails.cpf.plugincall.base.CallParameters;
import pt.webdetails.cpf.repository.api.IBasicFile;
import pt.webdetails.cpf.repository.api.IBasicFileFilter;
import pt.webdetails.cpf.repository.api.IUserContentAccess;
import pt.webdetails.cpf.utils.PluginIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Path("cdc/api/services/DashboardCacheCleanService")
public class DashboardCacheCleanService {

  private static Log logger = LogFactory.getLog( DashboardCacheCleanService.class );

  /**
   * Find all .cdfde files in solution
   *
   * @return All .cdfde files in solution.
   */
  @GET
  @Path("/getDashboardsList")
  public static void getDashboardsList( @Context HttpServletResponse response ) throws IOException {

    try {
      IUserContentAccess userContentAccess =
          PentahoBasePluginEnvironment.env().getContentAccessFactory().getUserContentAccess( "/" );
      List<IBasicFile> files = userContentAccess.listFiles( "", getDashboardFilter(), -1, true, false );
      List<String> filesNames = getFilesNames( files );
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
          Result.getOK( filesNames.toArray( new String[files.size()] ) ).toString() );
    } catch ( Exception e ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
    }

  }

  private static IBasicFileFilter getDashboardFilter() {
    return new IBasicFileFilter() {
      @Override
      public boolean accept( IBasicFile iBasicFile ) {
        return StringUtils.endsWith( iBasicFile.getName(), ".cdfde" );
      }
    };
  }

  private static List<String> getFilesNames( List<IBasicFile> files ) {
    List<String> filesNames = new ArrayList<String>();
    for ( IBasicFile file : files ) {
      filesNames.add( file.getFullPath() );
    }
    return filesNames;
  }

  /**
   * Clear all CDA data sources referenced by a given dashboard.
   *
   * @param dashboard The full path (from solution) of the dashboard
   * @return Number of entries cleared from cache.
   */

  @GET
  @Path("/clearDashboard")
  public void clearDashboard( @Context HttpServletResponse response,
      @QueryParam("dashboard") @DefaultValue("") String dashboard ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    IPluginCall pluginCall =
        PluginEnvironment.env().getPluginCall( CorePlugin.CDE.getId(), "datasources", "listCdaSources" );
    CallParameters callParameters = new CallParameters();
    callParameters.put( "dashboard", dashboard );

    try {
      JSONArray results = new JSONArray( pluginCall.call( callParameters.getParameters() ) );
      IPluginCall cacheMonitorCall =
          PluginEnvironment.env().getPluginCall( CorePlugin.CDA.getId(), "CdaCacheMonitor", "removeAllInterPlugin" );
      CallParameters cacheMonitorParameters = new CallParameters();
      for ( int i = 0; i < results.length(); i++ ) {
        JSONObject dataSource = (JSONObject) results.get( i );
        cacheMonitorParameters.put( "cdaSettingsId", dataSource.getString( "cdaSettingsId" ) );
        if ( dataSource.has( "dataAccessId" ) ) {
          cacheMonitorParameters.put( "dataAccessId", dataSource.getString( "dataAccessId" ) );
        } else {
          cacheMonitorParameters.put( "dataAccessId", null );
        }

        JSONObject itemsCleared = new JSONObject( cacheMonitorCall.call( cacheMonitorParameters.getParameters() ) );
        if ( StringUtils.equalsIgnoreCase( itemsCleared.getString( "status" ), "ok" ) ) {
          int numCleared = itemsCleared.getInt( "result" );
          dataSource.put( "cleared", numCleared );
        }
      }
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( results ).toString() );
    } catch ( JSONException e ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
    } catch ( Exception e ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
    }

  }

}
