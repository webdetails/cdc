package pt.webdetails.cdc.ws;

import javax.sql.DataSource;
import mondrian.olap.*;
import mondrian.olap.CacheControl.CellRegion;
import mondrian.rolap.RolapConnectionProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.data.IDatasourceService;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;

/**
 *
 * @author diogo
 */
public class MondrianCacheCleanService {
    
    private final IMondrianCatalogService mondrianCatalogService = PentahoSystem.get(IMondrianCatalogService.class, IMondrianCatalogService.class.getSimpleName(), null);
    private static Log logger = LogFactory.getLog(MondrianCacheCleanService.class);
    private IPentahoSession userSession;
    
    
    /**
     * Clear cache from a given catalog
     * 
     * @param catalog Catalog to be cleaned
     * @return Message describing the clear action result
     */
    public Result clearCatalog(String catalog){
        try{
            Connection connection = getMdxConnection(catalog);
            
            if(connection == null){
                return new Result(Result.Status.ERROR, "Catalog "+catalog+" non available.");
            }
            CacheControl cacheControl = connection.getCacheControl(null);

            Cube[] cubes = connection.getSchema().getCubes();
            
            if(cubes.length == 0) {
                return new Result(Result.Status.ERROR,"Catalog "+catalog+" contains no cubes.");
            }

            for (int i = 0; i < cubes.length; i++) {
                logger.debug("flushing cube " + cubes[i].getName());
                cacheControl.flush(cacheControl.createMeasuresRegion(cubes[i]));
            } 
        
            return new Result(Result.Status.OK,"Catalog "+catalog+" cache cleaned.");
            
        } catch(Exception e){
            return new Result(Result.Status.ERROR,e.getLocalizedMessage());
        }
    }
    
    /**
     * Clear cache from a given cube in a certain catalog
     * 
     * @param catalog Catalog defining where cube is defined
     * @param cube Cube to be cleaned
     * @return Message describing the clear action result
     */
    public Result clearCube(String catalog, String cube) {
      try {
        Connection connection = getMdxConnection(catalog);
  
        if (connection == null) {
          return new Result(Result.Status.ERROR, "Catalog " + catalog + " non available.");
        }
  
        CacheControl cacheControl = connection.getCacheControl(null);
  
        Cube[] cubes = connection.getSchema().getCubes();
  
        if (cubes.length == 0) {
          return new Result(Result.Status.ERROR, "Catalog " + catalog + " contains no cubes.");
        }
  
        int i = 0;
        for (; i < cubes.length; i++) {
          if (cubes[i].getName().equals(cube)) {
            logger.debug("flushing cube " + cubes[i].getName());
            CellRegion cubeRegion = cacheControl.createMeasuresRegion(cubes[i]);
            cacheControl.flush(cubeRegion);
            break;
          }
        }
  
        if (i == cubes.length) {
          return new Result(Result.Status.ERROR, "Cube " + cube + " not found.");
        }
  
        return new Result(Result.Status.OK, "Cube " + cube + " cache cleaned.");
      } catch (Exception e) {
        return new Result(Result.Status.ERROR, e.getLocalizedMessage());
      }
    }
     
    
    /**
     * Clear cache defined in a certain cube
     * 
     * @param catalog Catalog defining where cube is defined
     * @param cube Cube defining where dimension is defined
     * @param dimension Dimension to be cleaned
     * @return Message describing the clear action result
     */
    public Result clearDimension(String catalog, String cube, String dimension){
        try{
            Connection connection = getMdxConnection(catalog);
            
            if(connection == null){
                return new Result(Result.Status.ERROR,"Catalog "+catalog+" non available.");
            }
            
            CacheControl cacheControl = connection.getCacheControl(null);

            Cube[] cubes = connection.getSchema().getCubes();
            
            if(cubes.length == 0) {
                return new Result(Result.Status.ERROR,"Catalog "+catalog+" contains no cubes.");
            }

            for (int i = 0; i < cubes.length; i++) {
                if(cubes[i].getName().equals(cube)){
                    CacheControl.CellRegion cubeRegion = cacheControl.createMeasuresRegion(cubes[i]);
                    Dimension[] dimensions = cubes[i].getDimensions();
                    
                    if(dimensions.length == 0) {
                        return new Result(Result.Status.ERROR,"Cube "+cube+" contains no dimensions.");
                    }
                    
                    for(int j = 0; j < dimensions.length; j++){
                        if(dimensions[j].getName().equals(dimension)){
                            Hierarchy[] hierarchies = dimensions[j].getHierarchies();
                            
                            if(hierarchies.length == 0) {
                                return new Result(Result.Status.ERROR,"Dimension "+dimension+" contains no hierarchy.");
                            }
                            
                            for(int k = 0; k < hierarchies.length; k++){
                                CacheControl.CellRegion hierarchyRegion = cacheControl.createMemberRegion(hierarchies[k].getAllMember(),true);
                                CacheControl.CellRegion region = cacheControl.createCrossjoinRegion(cubeRegion, hierarchyRegion);
                                
                                cacheControl.flush(region);
                            }
                            break;
                        }
                    }
                    break;
                }
            } 

            return new Result(Result.Status.OK,"Dimension "+dimension+" cache cleaned.");
            
        } catch(Exception e){
            return new Result(Result.Status.ERROR,e.getLocalizedMessage());
        }
    }
    
    
    private Connection getMdxConnection(String catalog) {
      
      if(catalog != null && catalog.startsWith("/")) 
      {
        catalog = StringUtils.substring(catalog, 1);
      }

      MondrianCatalog selectedCatalog = mondrianCatalogService.getCatalog(catalog, PentahoSessionHolder.getSession());
      if(selectedCatalog == null)
      {
        logger.error("Received catalog '" + catalog  + "' doesn't appear to be valid");
        return null;
      }
      selectedCatalog.getDataSourceInfo();
      logger.info("Found catalog " + selectedCatalog.toString());
      
      String connectStr = "provider=mondrian;dataSource=" + selectedCatalog.getEffectiveDataSource().getJndi() +
      "; Catalog=" + selectedCatalog.getDefinition();
      
      return getMdxConnectionFromConnectionString(connectStr);
    }
    
    
    private Connection getMdxConnectionFromConnectionString(String connectStr){
        Connection nativeConnection = null;
    	Util.PropertyList properties = Util.parseConnectString(connectStr);
        try {
            String dataSourceName = properties.get(RolapConnectionProperties.DataSource.name());

            if (dataSourceName != null) {
                IDatasourceService datasourceService = PentahoSystem.getObjectFactory().get(IDatasourceService.class, null);
                DataSource dataSourceImpl = datasourceService.getDataSource(dataSourceName);
                if (dataSourceImpl != null) {
                    properties.remove(RolapConnectionProperties.DataSource.name());
                    nativeConnection = DriverManager.getConnection(properties, null, dataSourceImpl);
                } else {
                    nativeConnection = DriverManager.getConnection(properties, null);
                }
            } else {
                nativeConnection = DriverManager.getConnection(properties, null);
            }

            if (nativeConnection == null) {
                logger.error("Invalid connection: " + connectStr);
            }
        } catch (Throwable t) {
            logger.error("Invalid connection: " + connectStr + " - " + t.toString());
        }

        return nativeConnection;
    }
}

 
