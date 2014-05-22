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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletResponse;

import com.hazelcast.config.MapConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import pt.webdetails.cdc.core.CoreHazelcastConfigHelper.MapConfigOption;
import pt.webdetails.cdc.core.HazelcastConfigHelper;
import pt.webdetails.cdc.core.HazelcastManager;
import pt.webdetails.cdc.plugin.CdcConfig;
import pt.webdetails.cdc.plugin.ExternalConfigurationsHelper;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;
import pt.webdetails.cpf.utils.PluginIOUtils;

import java.io.IOException;
import java.util.Arrays;

@Path( "cdc/api/services/HazelcastConfigurationService" )
public class HazelcastConfigurationService {

  static Log log = LogFactory.getLog( HazelcastConfigurationService.class );

  @GET
  @Path( "/setMapOption" )
  @Produces( "application/json" )
  public void setMapOption( @Context HttpServletResponse response,
      @QueryParam( "map" ) @DefaultValue( "" ) String map,
      @QueryParam( "name" ) @DefaultValue( "" ) String name,
      @QueryParam( "value" ) @DefaultValue( "" ) String value ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    MapConfigOption option = MapConfigOption.parse( name );
    CacheMap cacheMap = CacheMap.parse( map );

    if ( option == null ) {
      PluginIOUtils
          .writeOutAndFlush( response.getOutputStream(), Result.getError( "No such option: " + name ).toString() );
    }
    if ( cacheMap == null ) {
      PluginIOUtils
          .writeOutAndFlush( response.getOutputStream(), Result.getError( "No such map: " + name ).toString() );
    }
    if ( value == null ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getError( "Must supply value" ).toString() );
    }

    MapConfig mapConfig = getMapConfig( cacheMap );
    switch ( option ) {
      case enabled:
        Boolean enabled = parseBooleanStrict( value );
        if ( enabled != null ) {
          switch ( cacheMap ) {
            case Cda:
              try {
                ExternalConfigurationsHelper.setCdaHazelcastEnabled( enabled );
                PluginIOUtils.writeOutAndFlush( response.getOutputStream(), new Result( Result.Status.OK,
                    "Configuration changed, please restart Pentaho server after finishing changes" ).toString() );
              } catch ( Exception e ) {
                PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
              }
              break;
            case Mondrian:
              try {
                CdcConfig.getConfig().setMondrianCdcEnabled( enabled );
                PluginIOUtils.writeOutAndFlush( response.getOutputStream(), new Result( Result.Status.OK,
                    "Configuration changed, please restart Pentaho server after finishing changes" ).toString() );
              } catch ( Exception e ) {
                PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
              }
          }
        } else {
          PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
              Result.getError( "enabled must be either 'true' or 'false'." ).toString() );
        }
        return;
      case maxSizePolicy:
        if ( Arrays.binarySearch( HazelcastConfigHelper.MAX_SIZE_POLICIES, value ) >= 0 ) {
          mapConfig.getMaxSizeConfig().setMaxSizePolicy( value );
        } else {
          PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
              Result.getError( "Unrecognized size policy." ).toString() );
          return;
        }

        break;
      case evictionPercentage:
        try {
          int evictionPercentage = Integer.parseInt( value );
          if ( evictionPercentage <= 0 || evictionPercentage > 100 ) {
            PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
                Result.getError( "Invalid domain for percentage." ).toString() );
            return;
          }
          mapConfig.setEvictionPercentage( evictionPercentage );
        } catch ( NumberFormatException nfe ) {
          PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( nfe ).toString() );
          return;
        }
        break;
      case evictionPolicy:
        if ( Arrays.binarySearch( HazelcastConfigHelper.EVICTION_POLICIES, value ) >= 0 ) {
          mapConfig.setEvictionPolicy( value );
        } else {
          PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
              Result.getError( "Unrecognized eviction policy" ).toString() );
          return;
        }

        mapConfig.setEvictionPolicy( value );
        break;
      case maxSize:
        try {
          int maxSize = Integer.parseInt( value );
          mapConfig.getMaxSizeConfig().setSize( maxSize );
        } catch ( NumberFormatException nfe ) {
          PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( nfe ).toString() );
          return;
        }

        break;
      case timeToLive:
        try {
          int timeToLiveSeconds = Integer.parseInt( value );
          mapConfig.setTimeToLiveSeconds( timeToLiveSeconds );
        } catch ( NumberFormatException nfe ) {
          PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( nfe ).toString() );
          return;
        }
        break;
      default://shouldn't reach
        PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
            Result.getError( "Unrecognized option " + name ).toString() );
        return;
    }
    PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
        Result.getOK( "Option " + name + " changed." ).toString() );
  }

  /**
   * Defaults to null instead of false for unparseable values.
   *
   * @param value
   * @return
   */
  private static Boolean parseBooleanStrict( String value ) {
    if ( !StringUtils.isEmpty( value ) ) {
      value = value.trim().toLowerCase();
      if ( value.equals( "true" ) ) {
        return true;
      } else if ( value.equals( "false" ) ) {
        return false;
      }
    }
    return null;
  }

  @GET
  @Produces( "application/xml" )
  @Path( "/getMapOption" )
  public void getMapOption( @Context HttpServletResponse response,
      @QueryParam( "map" ) @DefaultValue( "" ) String map,
      @QueryParam( "name" ) @DefaultValue( "" ) String name ) throws IOException {

    MapConfigOption option = MapConfigOption.parse( name );

    if ( option == null ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
          new Result( Result.Status.ERROR, "No such option: " + name ).toString() );
      return;
    }

    CacheMap cacheMap = CacheMap.parse( map );
    if ( cacheMap == null ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getError( "No such map: " + map ).toString() );
      return;
    }

    MapConfig mapConfig = getMapConfig( cacheMap );

    Object result = null;
    switch ( option ) {
      case enabled:
        switch ( cacheMap ) {
          case Cda:
            try {
              result = ExternalConfigurationsHelper.isCdaHazelcastEnabled();
            } catch ( Exception e ) {
              PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
                  new Result( Result.Status.ERROR, e.getLocalizedMessage() ).toString() );
              return;
            }
            break;
          case Mondrian:
            result = CdcConfig.getConfig().isMondrianCdcEnabled();
            break;
        }
        break;
      case maxSizePolicy:
        result = mapConfig.getMaxSizeConfig().getMaxSizePolicy();
        break;
      case evictionPercentage:
        result = mapConfig.getEvictionPercentage();
        break;
      case evictionPolicy:
        result = mapConfig.getEvictionPolicy();
        break;
      case maxSize:
        result = mapConfig.getMaxSizeConfig().getSize();
        break;
      case timeToLive:
        result = mapConfig.getTimeToLiveSeconds();
    }
    PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( result ).toString() );

  }

  @GET
  @Path( "/getMaxSizePolicies" )
  public static void getMaxSizePolicies( @Context HttpServletResponse response ) throws IOException {
    JSONArray results = new JSONArray();
    for ( String value : HazelcastConfigHelper.MAX_SIZE_POLICIES ) {
      results.put( value );
    }
    PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( results ).toString() );
  }

  @GET
  @Path( "/getEvictionPolicies" )
  public static void getEvictionPolicies( @Context HttpServletResponse response ) throws IOException {
    JSONArray results = new JSONArray();
    for ( String value : HazelcastConfigHelper.EVICTION_POLICIES ) {
      results.put( value );
    }
    PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getOK( results ).toString() );
  }

  @GET
  @Path( "/saveConfig" )
  public void saveConfig( @Context HttpServletResponse response ) throws IOException {
    SecurityAssertions.assertIsAdmin();

    if ( HazelcastConfigHelper.saveConfig() ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
          new Result( Result.Status.OK, "Configuration saved and propagated." ).toString() );
    } else {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
          new Result( Result.Status.ERROR, "Error saving file." ).toString() );
    }
  }

  @GET
  @Path( "/loadConfig" )
  public void loadConfig( @Context HttpServletResponse response ) throws IOException {
    try {
      HazelcastManager.INSTANCE.init( CdcConfig.getConfig().getHazelcastConfigFile(), true );
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(),
          new Result( Result.Status.OK, "Configuration read from file." ).toString() );
    } catch ( Exception e ) {
      PluginIOUtils.writeOutAndFlush( response.getOutputStream(), Result.getFromException( e ).toString() );
    }
  }

  private static MapConfig getMapConfig( CacheMap cacheMap ) {
    if ( HazelcastManager.INSTANCE.isRunning() ) {
      return HazelcastManager.INSTANCE.getHazelcast().getConfig().getMapConfig( cacheMap.getName() );
    } else {
      log.warn( "Hazelcast must be enabled for map config to be available." );
      return new MapConfig( "Bogus" );
    }
  }


}
