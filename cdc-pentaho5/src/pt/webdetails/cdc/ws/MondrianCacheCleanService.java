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

import mondrian.olap.CacheControl;
import mondrian.olap.CacheControl.CellRegion;
import mondrian.olap.Connection;
import mondrian.olap.Cube;
import mondrian.olap.Dimension;
import mondrian.olap.DriverManager;
import mondrian.olap.Hierarchy;
import mondrian.olap.InvalidArgumentException;
import mondrian.olap.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import mondrian.olap.Id;
import mondrian.olap.Member;
import mondrian.olap.SchemaReader;

@Path( "cdc/api/services/MondrianCacheCleanService" )
public class MondrianCacheCleanService {

  private static Log logger = LogFactory.getLog( MondrianCacheCleanService.class );
  private final IMondrianCatalogService mondrianCatalogService = getMondrianCatalogService();

  public static void loadMondrianCatalogs() {
    IMondrianCatalogService mondrianCatalogService = getMondrianCatalogService();

    for ( MondrianCatalog catalog : mondrianCatalogService.listCatalogs( PentahoSessionHolder.getSession(), true ) ) {
      try {
        String connectStr = catalog.getDataSourceInfo() + "; Catalog= " + catalog.getDefinition();
        Connection conn = getConnectionFromString( connectStr );
        if ( conn == null ) {
          logger.warn( "Couldn't get connection for " + connectStr );
        }
      } catch ( Exception e ) {
        // not fatal for cache sync, no need to throw
        logger.error( "Error while creating connection", e );
      }
    }
  }

  /**
   * @param connectStr
   * @return
   */
  private static Connection getConnectionFromString( String connectStr ) {
    Util.PropertyList properties = Util.parseConnectString( connectStr );
    logger.debug( "loading connection: " + connectStr );
    Connection conn = DriverManager.getConnection( properties, null );
    return conn;
  }

  private static final IMondrianCatalogService getMondrianCatalogService() {
    return PentahoSystem.get( IMondrianCatalogService.class, IMondrianCatalogService.class.getSimpleName(), null );
  }

  /**
   * Clear cache from a given catalog
   *
   * @param catalog Catalog to be cleaned
   * @return Message describing the clear action result
   */
  @GET
  @Path( "/clearCatalog" )
  @Produces( "application/json" )
  public String clearCatalog( @Context HttpServletResponse response,
                              @QueryParam( "catalog" ) @DefaultValue( "" ) String catalog ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    try {
      if ( StringUtils.isEmpty( catalog ) ) {
        return Result.getError( "No catalog given." ).toString();
      }
      flushCubes( catalog, null );
      return Result.getOK( "Catalog " + catalog + " cache cleaned." ).toString();

    } catch ( Exception e ) {
      logger.error( e );
      return Result.getError( e.getMessage() ).toString();
    }
  }

  /**
   * Clear cache from a given cube in a certain catalog
   *
   * @param catalog Catalog defining where cube is defined
   * @param cube    Cube to be cleaned
   * @return Message describing the clear action result
   */
  @GET
  @Path( "/clearCube" )
  @Produces( "application/json" )
  public String clearCube( @Context HttpServletResponse response,
                           @QueryParam( "catalog" ) @DefaultValue( "" ) String catalog,
                           @QueryParam( "cube" ) @DefaultValue( "" ) String cube ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    try {
      if ( StringUtils.isEmpty( catalog ) ) {
        return Result.getError( "No catalog given." ).toString();
      }
      if ( StringUtils.isEmpty( cube ) ) {
        return Result.getError( "No cube given." ).toString();
      }
      flushCubes( catalog, cube );
      return Result.getOK( "Cube " + cube + " cache cleaned." ).toString();
    } catch ( Exception e ) {
      logger.error( e );
      return Result.getError( e.getMessage() ).toString();
    }
  }

  private void flushCubes( String catalog, String cube ) {
    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace( '+', ' ' );
    if ( cube != null ) {
      cube = cube.replace( '+', ' ' );
    }

    Connection connection = getMdxConnection( catalog );

    if ( connection == null ) {
      throw new InvalidArgumentException( "Catalog " + catalog + " non available." );
    }

    CacheControl cacheControl = connection.getCacheControl( null );

    Cube[] cubes = connection.getSchema().getCubes();

    if ( cubes.length == 0 ) {
      throw new InvalidArgumentException( "Catalog " + catalog + " contains no cubes." );
    }

    int i = 0;
    for (; i < cubes.length; i++ ) {
      if ( cube == null || cubes[ i ].getName().equals( cube ) ) {
        logger.debug( "flushing cube " + cubes[ i ].getName() );
        CellRegion cubeRegion = cacheControl.createMeasuresRegion( cubes[ i ] );

        cacheControl.flush( cubeRegion );

        if ( cube != null ) {
          break;
        }
      }
    }
    if ( cube != null && i == cubes.length ) {
      throw new InvalidArgumentException( "Cube " + cube + " not found." );
    }

  }

  /**
   * Clear cache defined in a certain cube
   *
   * @param catalog   Catalog defining where cube is defined
   * @param cube      Cube defining where dimension is defined
   * @param dimension Dimension to be cleaned
   * @return Message describing the clear action result
   */
  @GET
  @Path( "/clearDimension" )
  @Produces( "application/json" )
  public String clearDimension( @Context HttpServletResponse response,
                                @QueryParam( "catalog" ) @DefaultValue( "" ) String catalog,
                                @QueryParam( "cube" ) @DefaultValue( "" ) String cube,
                                @QueryParam( "dimension" ) @DefaultValue( "" ) String dimension ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    if ( StringUtils.isEmpty( catalog ) ) {
      return Result.getError( "No catalog given." ).toString();
    }
    if ( StringUtils.isEmpty( cube ) ) {
      return Result.getError( "No cube given." ).toString();
    }
    if ( StringUtils.isEmpty( dimension ) ) {
      return Result.getError( "No dimension given." ).toString();
    }

    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace( '+', ' ' );
    cube = cube.replace( '+', ' ' );
    dimension = dimension.replace( '+', ' ' );

    try {
      flushHierarchies( catalog, cube, dimension, null );
      return Result.getOK( "Dimension " + dimension + " cleaned from cache." ).toString();
    } catch ( Exception e ) {
      logger.error( e );
      return Result.getError( e.getMessage() ).toString();
    }
  }

  /**
   * Clear cache defined in a certain cube
   *
   * @param catalog   Catalog defining where cube is defined
   * @param cube      Cube defining where dimension is defined
   * @param dimension Dimension where hierarchy is defined
   * @param hierarchy Hierarchy to be cleaned
   * @return Message describing the clear action result
   */
  @GET
  @Path( "/clearHierarchy" )
  @Produces( "application/json" )
  public String clearHierarchy( @Context HttpServletResponse response,
                                @QueryParam( "catalog" ) @DefaultValue( "" ) String catalog,
                                @QueryParam( "cube" ) @DefaultValue( "" ) String cube,
                                @QueryParam( "dimension" ) @DefaultValue( "" ) String dimension,
                                @QueryParam( "hierarchy" ) @DefaultValue( "" ) String hierarchy ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    if ( StringUtils.isEmpty( catalog ) ) {
      return Result.getError( "No catalog given." ).toString();
    }
    if ( StringUtils.isEmpty( cube ) ) {
      return Result.getError( "No cube given." ).toString();
    }
    if ( StringUtils.isEmpty( dimension ) ) {
      return Result.getError( "No dimension given." ).toString();
    }
    if ( StringUtils.isEmpty( hierarchy ) ) {
      return Result.getError( "No hierarchy given." ).toString();
    }

    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace( '+', ' ' );
    cube = cube.replace( '+', ' ' );
    dimension = dimension.replace( '+', ' ' );
    hierarchy = hierarchy.replace( '+', ' ' ).replace( "[", "" ).replace( "]", "" );

    try {
      flushHierarchies( catalog, cube, dimension, hierarchy );
      return Result.getOK( "Hierarchy " + hierarchy + " cleaned" ).toString();
    } catch ( Exception e ) {
      return Result.getError( e.getMessage() ).toString();
    }
  }

  /**
   * This method will clean the Mondrian Cache for a CellRegion which is defined by a Crossjoin between all
   * Measures and
   * the two given members.
   *
   * @param response
   * @param catalog
   * @param cube
   * @param member1  Syntax: [Dimension.Hierarchy].[Level0].[Level1]...[LevelN] (e.g. [Time.YMD].[2014].[1])
   * @param member2  Syntax: [Dimension.Hierarchy].[Level0].[Level1]...[LevelN] (e.g. [Time.YMD].[2014].[1])
   * @return
   * @throws IOException
   */
  @GET
  @Path( "/clearCellRegion" )
  @Produces( "application/json" )
  public String clearCellRegion( @Context HttpServletResponse response,
                                 @QueryParam( "catalog" ) @DefaultValue( "" ) String catalog,
                                 @QueryParam( "cube" ) @DefaultValue( "" ) String cube,
                                 @QueryParam( "member1" ) @DefaultValue( "" ) String member1,
                                 @QueryParam( "member2" ) @DefaultValue( "" ) String member2 ) throws IOException {

    SecurityAssertions.assertIsAdmin();

    if ( StringUtils.isEmpty( catalog ) ) {
      return Result.getError( "No catalog given." ).toString();
    }
    if ( StringUtils.isEmpty( cube ) ) {
      return Result.getError( "No cube given." ).toString();
    }
    if ( StringUtils.isEmpty( member1 ) ) {
      return Result.getError( "No value given for Member 1." ).toString();
    }
    if ( StringUtils.isEmpty( member2 ) ) {
      return Result.getError( "No value given for Member 2." ).toString();
    }

    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace( '+', ' ' );
    cube = cube.replace( '+', ' ' );

    //Split Members by ] : [Time.YMD].[2014].[1] => {"[Time.YMD", "[2014", "[1]"}
    String[] split1 = member1.split( "]." );
    String[] split2 = member2.split( "]." );

    //Remove all brackets
    for ( int i = 0; i < split1.length; i++ ) {
      split1[ i ] = split1[ i ].replace( "[", "" ).replace( "]", "" );
    }

    for ( int i = 0; i < split2.length; i++ ) {
      split2[ i ] = split2[ i ].replace( "[", "" ).replace( "]", "" );
    }

    //Create Segment Lists from given Members
    List<Id.Segment> member1Segments = Id.Segment.toList( split1 );
    List<Id.Segment> member2Segments = Id.Segment.toList( split2 );

    try {
      cleanCellRegion( member1Segments, member2Segments, catalog, cube );
      return Result.getOK( "Cell Region flushed successfully" ).toString();
    } catch ( Exception e ) {
      return Result.getError( e.getMessage() ).toString();
    }
  }

  private void flushHierarchies( String catalog, String cube, String dimension, String hierarchy ) {

    Connection connection = getMdxConnection( catalog );

    if ( connection == null ) {
      throw new InvalidArgumentException( "Catalog " + catalog + " non available." );
    }

    CacheControl cacheControl = connection.getCacheControl( null );

    Cube[] cubes = connection.getSchema().getCubes();

    if ( cubes.length == 0 ) {
      throw new InvalidArgumentException( "Catalog " + catalog + " contains no cubes." );
    }

    for ( int i = 0; i < cubes.length; i++ ) {
      if ( cube == null || cubes[ i ].getName().equals( cube ) ) {
        CellRegion cubeRegion = cacheControl.createMeasuresRegion( cubes[ i ] );
        Dimension[] dimensions = cubes[ i ].getDimensions();

        if ( dimensions.length == 0 ) {
          throw new InvalidArgumentException( "Cube " + cube + " contains no dimensions." );
        }

        for ( int j = 0; j < dimensions.length; j++ ) {
          if ( dimension == null || dimensions[ j ].getName().equals( dimension ) ) {
            Hierarchy[] hierarchies = dimensions[ j ].getHierarchies();

            if ( hierarchies.length == 0 ) {
              throw new InvalidArgumentException( "Dimension " + dimension + " contains no hierarchies." );
            }

            for ( int k = 0; k < hierarchies.length; k++ ) {
              if ( hierarchy == null || hierarchy.equals( hierarchies[ k ].getName() ) ) {
                CellRegion hierarchyRegion
                  = cacheControl.createMemberRegion( hierarchies[ k ].getAllMember(), true );
                CellRegion region = cacheControl.createCrossjoinRegion( cubeRegion, hierarchyRegion );

                cacheControl.flush( region );
                if ( hierarchy != null ) {
                  break;
                }
              }
            }
            if ( dimension != null ) {
              break;
            }
          }
        }
        if ( cube != null ) {
          break;
        }
      }
    }
  }

  private Connection getMdxConnection( String catalog ) {

    if ( catalog != null && catalog.startsWith( "/" ) ) {
      catalog = StringUtils.substring( catalog, 1 );
    }

    MondrianCatalog selectedCatalog = mondrianCatalogService.getCatalog( catalog, PentahoSessionHolder.getSession() );

    if ( selectedCatalog == null ) {
      logger.error( "Received catalog '" + catalog + "' doesn't appear to be valid" );
      return null;
    }

    String connectStr = selectedCatalog.getDataSourceInfo() + "; Catalog=" + selectedCatalog.getDefinition();
    logger.info( "Found catalog " + selectedCatalog.toString() );

    Connection conn = null;
    try {
      conn = getConnectionFromString( connectStr );
      if ( conn == null ) {
        logger.warn( "Couldn't get connection for " + connectStr );
      }
    } catch ( Exception e ) {
      logger.error( "Error while creating connection", e );
    }
    return conn;
  }

  private void cleanCellRegion( List<Id.Segment> member1, List<Id.Segment> member2, String catalog, String cubeParam ) {

    Connection connection = getMdxConnection( catalog );
    CacheControl cacheControl = connection.getCacheControl( new PrintWriter( System.out ) );

    Cube[] cubes = connection.getSchema().getCubes();

    if ( cubes.length == 0 ) {
      throw new InvalidArgumentException( "Catalog " + catalog + " contains no cubes." );
    }

    Cube selectedCube = null;

    for ( Cube cube : cubes ) {
      if ( cube.getName().equals( cubeParam ) ) {
        selectedCube = cube;
      }
    }

    if ( selectedCube == null ) {
      throw new InvalidArgumentException( "Cube " + cubeParam + " is not contained in " + catalog );
    }

    SchemaReader schemaReader = selectedCube.getSchemaReader( null ).withLocus();

    Member memberByUniqueName1 = schemaReader.getMemberByUniqueName( member1, true );
    Member memberByUniqueName2 = schemaReader.getMemberByUniqueName( member2, true );

    //Check if provided Members exists
    if ( memberByUniqueName1 == null ) {
      throw new InvalidArgumentException( "Member 1 not found." );
    } else if ( memberByUniqueName2 == null ) {
      throw new InvalidArgumentException( "Member 2 not found." );
    }

    //We will wipe out all Measures for the given Dimension Members
    CacheControl.CellRegion measuresRegion = cacheControl.createMeasuresRegion( selectedCube );

    CacheControl.CellRegion region1 = cacheControl.createMemberRegion( memberByUniqueName1, true );
    CacheControl.CellRegion region2 = cacheControl.createMemberRegion( memberByUniqueName2, true );
    CacheControl.CellRegion regionCrossjoin = cacheControl.createCrossjoinRegion( measuresRegion, region1, region2 );

    cacheControl.flush( regionCrossjoin );
  }
}
