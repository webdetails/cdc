/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.ws;

import javax.sql.DataSource;
import mondrian.olap.*;
import mondrian.olap.CacheControl.CellRegion;
import mondrian.rolap.RolapConnectionProperties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.data.IDatasourceService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;

import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.SecurityAssertions;

/**
 * 
 * @author diogo
 */
public class MondrianCacheCleanService {

  private final IMondrianCatalogService mondrianCatalogService = getMondrianCatalogService();
  private static Log logger = LogFactory.getLog(MondrianCacheCleanService.class);

  /**
   * Clear cache from a given catalog
   * 
   * @param catalog
   *          Catalog to be cleaned
   * @return Message describing the clear action result
   */

  public String clearCatalog(String catalog) {
    
    SecurityAssertions.assertIsAdmin();

    try {
      if(StringUtils.isEmpty(catalog)) return Result.getError("No catalog given.").toString();
      flushCubes(catalog,null);
      return Result.getOK("Catalog " + catalog + " cache cleaned.").toString();

    } catch (Exception e) {
      return Result.getError(e.getMessage()).toString();
    }
  }

  /**
   * Clear cache from a given cube in a certain catalog
   * 
   * @param catalog
   *          Catalog defining where cube is defined
   * @param cube
   *          Cube to be cleaned
   * @return Message describing the clear action result
   */

  public String clearCube(String catalog, String cube) {
    
    SecurityAssertions.assertIsAdmin();
    
    try {
      if(StringUtils.isEmpty(catalog)) return Result.getError("No catalog given.").toString();
      if(StringUtils.isEmpty(cube)) return Result.getError("No cube given.").toString();
    
      flushCubes(catalog,cube);
      return Result.getOK("Cube " + cube + " cache cleaned.").toString();
    } catch (Exception e) {
      return Result.getError(e.getMessage()).toString();
    }
  }
  
  private void flushCubes(String catalog, String cube){
    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace('+', ' ');
    if(cube!=null) cube = cube.replace('+', ' ');

      
    Connection connection = getMdxConnection(catalog);

    
    if (connection == null) {
      throw new InvalidArgumentException("Catalog " + catalog + " non available.");
    }

    CacheControl cacheControl = connection.getCacheControl(null);

    Cube[] cubes = connection.getSchema().getCubes();

    if (cubes.length == 0) {
      throw new InvalidArgumentException("Catalog " + catalog + " contains no cubes.");
    }

    
    int i = 0;
    for (; i < cubes.length; i++) {
      if (cube == null || cubes[i].getName().equals(cube)) {
        logger.debug("flushing cube " + cubes[i].getName());
        CellRegion cubeRegion = cacheControl.createMeasuresRegion(cubes[i]);
        
        cacheControl.flush(cubeRegion);
        
        if(cube != null) break;
      }
    }
    if (cube != null && i == cubes.length) {
      throw new InvalidArgumentException("Cube " + cube + " not found.");
    }

  }

  /**
   * Clear cache defined in a certain cube
   * 
   * @param catalog
   *          Catalog defining where cube is defined
   * @param cube
   *          Cube defining where dimension is defined
   * @param dimension
   *          Dimension to be cleaned
   * @return Message describing the clear action result
   */

  public String clearDimension(String catalog, String cube, String dimension) {
    
    SecurityAssertions.assertIsAdmin();
    
    if(StringUtils.isEmpty(catalog)) return Result.getError("No catalog given.").toString();
    if(StringUtils.isEmpty(cube)) return Result.getError("No cube given.").toString();
    if(StringUtils.isEmpty(dimension)) return Result.getError("No dimension given.").toString();
    
    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace('+', ' ');
    cube = cube.replace('+', ' ');
    dimension = dimension.replace('+', ' ');
    
    
    try{
      flushHierarchies(catalog, cube, dimension, null);
      return Result.getOK("Dimension " + dimension + " cleaned from cache.").toString();
     } catch (Exception e) {
       return Result.getError(e.getMessage()).toString();
     }
  }
  
  /**
   * Clear cache defined in a certain cube
   * 
   * @param catalog Catalog defining where cube is defined
   * @param cube Cube defining where dimension is defined
   * @param dimension Dimension where hierarchy is defined
   * @param hierarchy Hierarchy to be cleaned
   *          
   * @return Message describing the clear action result
   */
  public String clearHierarchy(String catalog, String cube, String dimension, String hierarchy){
    
    SecurityAssertions.assertIsAdmin();
    
    if(StringUtils.isEmpty(catalog)) return Result.getError("No catalog given.").toString();
    if(StringUtils.isEmpty(cube)) return Result.getError("No cube given.").toString();
    if(StringUtils.isEmpty(dimension)) return Result.getError("No dimension given.").toString();
    if(StringUtils.isEmpty(hierarchy)) return Result.getError("No hierarchy given.").toString();
    
    
    //Ensure escaped '+' are transformed back to spaces
    catalog = catalog.replace('+', ' ');
    cube = cube.replace('+', ' ');
    dimension = dimension.replace('+', ' ');
    hierarchy = hierarchy.replace('+', ' ').replace("[", "").replace("]", "");
    
    try{
     flushHierarchies(catalog, cube, dimension, hierarchy);
     return Result.getOK("Hierarchy " + hierarchy + " cleaned").toString();
    } catch (Exception e) {
      return Result.getError(e.getMessage()).toString();
    }
  }

  public static void loadMondrianCatalogs() {
    IMondrianCatalogService mondrianCatalogService = getMondrianCatalogService();
//    mondrianCatalogService.
//    int errors = 0;
    for (MondrianCatalog catalog :
         mondrianCatalogService.listCatalogs(PentahoSessionHolder.getSession(), true))
    {
      try {
        String connectStr = catalog.getDataSourceInfo() +
            "; Catalog= " + catalog.getDefinition();
        Connection conn = getConnectionFromString(connectStr);
        if (conn == null) {
          logger.warn("Couldn't get connection for " + connectStr);
        }
      } catch (Exception e) {
        // not fatal for cache sync, no need to throw
        logger.error("Error while creating connection", e);
//        errors++;
      }
    }
//    if (errors > 0) {
//      return Result.getError("errorCount:" + errors).toString();
//    }
//    return Result.getOK("loaded").toString();
  }

  /**
   * @param connectStr
   * @return
   */
  private static Connection getConnectionFromString(String connectStr) {
    Util.PropertyList properties = Util.parseConnectString(connectStr);
    logger.debug("loading connection: " + connectStr);
    Connection conn = DriverManager.getConnection(properties, null);
    return conn;
  }

  private static final IMondrianCatalogService getMondrianCatalogService() {
    return PentahoSystem.get(IMondrianCatalogService.class, IMondrianCatalogService.class.getSimpleName(), null);
  }

  private void flushHierarchies(String catalog, String cube, String dimension, String hierarchy) {

      Connection connection = getMdxConnection(catalog);

      if (connection == null) {
        throw new InvalidArgumentException("Catalog " + catalog + " non available.");
      }

      CacheControl cacheControl = connection.getCacheControl(null);

      Cube[] cubes = connection.getSchema().getCubes();

      if (cubes.length == 0) {
        throw new InvalidArgumentException("Catalog " + catalog + " contains no cubes.");
      }

      for (int i = 0; i < cubes.length; i++) {
        if (cube == null || cubes[i].getName().equals(cube)) {
          CacheControl.CellRegion cubeRegion = cacheControl.createMeasuresRegion(cubes[i]);
          Dimension[] dimensions = cubes[i].getDimensions();

          if (dimensions.length == 0) {
            throw new InvalidArgumentException("Cube " + cube + " contains no dimensions.");
          }

          for (int j = 0; j < dimensions.length; j++) {
            if (dimension == null || dimensions[j].getName().equals(dimension)) {
              Hierarchy[] hierarchies = dimensions[j].getHierarchies();

              if (hierarchies.length == 0) {
                throw new InvalidArgumentException("Dimension " + dimension + " contains no hierarchies.");
              }

              for (int k = 0; k < hierarchies.length; k++) {
                if (hierarchy == null || hierarchy.equals(hierarchies[k].getName())) {
                  CacheControl.CellRegion hierarchyRegion = cacheControl.createMemberRegion(hierarchies[k].getAllMember(), true);
                  CacheControl.CellRegion region = cacheControl.createCrossjoinRegion(cubeRegion, hierarchyRegion);

                  cacheControl.flush(region);
                  if (hierarchy != null) break;
                }
              }
              if (dimension != null) break;
            }
          }
          if (cube != null) break;
        }
      }
  }

  private Connection getMdxConnection(String catalog) {

    if (catalog != null && catalog.startsWith("/")) {
      catalog = StringUtils.substring(catalog, 1);
    }

    MondrianCatalog selectedCatalog = mondrianCatalogService.getCatalog(catalog, PentahoSessionHolder.getSession());
        
    if (selectedCatalog == null) {
      logger.error("Received catalog '" + catalog + "' doesn't appear to be valid");
      return null;
    }

    String connectStr = selectedCatalog.getDataSourceInfo() + "; Catalog=" + selectedCatalog.getDefinition();
    logger.info("Found catalog " + selectedCatalog.toString());

    Connection conn = null;
    try {
      conn = getConnectionFromString(connectStr);
      if (conn == null) {
        logger.warn("Couldn't get connection for " + connectStr);
      }
    } catch (Exception e) {
      logger.error("Error while creating connection", e);
    }
    return conn;
//    return getMdxConnectionFromConnectionString(connectStr);
  }

//  private Connection getMdxConnectionFromConnectionString(String connectStr) {
//    Connection nativeConnection = null;
//    Util.PropertyList properties = Util.parseConnectString(connectStr);
//    try {
//      String dataSourceName = properties.get(RolapConnectionProperties.DataSource.name());
//
//      if (dataSourceName != null) {
//        IDatasourceService datasourceService = PentahoSystem.getObjectFactory().get(IDatasourceService.class, null);
//        DataSource dataSourceImpl = datasourceService.getDataSource(dataSourceName);
//        if (dataSourceImpl != null) {
//          properties.remove(RolapConnectionProperties.DataSource.name());
//          nativeConnection = DriverManager.getConnection(properties, null, dataSourceImpl);
//        } else {
//          nativeConnection = DriverManager.getConnection(properties, null);
//        }
//      } else {
//        nativeConnection = DriverManager.getConnection(properties, null);
//      }
//
//      if (nativeConnection == null) {
//        logger.error("Invalid connection: " + connectStr);
//      }
//    } catch (Throwable t) {
//      logger.error("Invalid connection: " + connectStr + " - " + t.toString());
//    }
//
//    return nativeConnection;
//  }
//  
////duplicate some cache segment <numNewEntries> times  
//  public String fillMondrianCache(int numNewEntries)
//  {
//    SegmentHeader header = null;
//    SegmentBody body = null;
//    
//    IMap<SegmentHeader, SegmentBody> cache = Hazelcast.getMap("mondrian");
//    if(cache.size() == 0){
//      return Result.getError("No elements in cache").toString();
//    }
//    
//    //fetch first
//    for(SegmentHeader key : cache.keySet()){
//      body = cache.get(key);
//      header = key;
//      break;
//    }
//    
//    String schema = header.schemaName
//    for(int i=0; i< numNewEntries; i++){
//      
//      String schemaSuffix = "" + (header.schemaName + header.hashCode()).hashCode() + System.currentTimeMillis();
//      header = new SegmentHeader(schema + schemaSuffix, header.schemaChecksum, header.cubeName, header.measureName, header.getConstrainedColumns(),
//          header.compoundPredicates, header.rolapStarFactTableName, header.constrainedColsBitKey, header.getExcludedRegions());
//      cache.put(header, body);
//      
//    }
//    
//    return Result.getOK(numNewEntries).toString();
//    
//  }
  
}
