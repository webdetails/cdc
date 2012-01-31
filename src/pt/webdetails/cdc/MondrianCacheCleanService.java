/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.webdetails.cdc;

import javax.sql.DataSource;
import mondrian.olap.*;
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
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalogHelper;

/**
 *
 * @author diogo
 */
public class MondrianCacheCleanService {
    
    private final IMondrianCatalogService mondrianCatalogService = MondrianCatalogHelper.getInstance();
    private static Log logger = LogFactory.getLog(MondrianCacheCleanService.class);
    private IPentahoSession userSession;
    
    
    public StatusMessage clearSchema(String catalog){
        Connection connection = getMdxConnection(catalog);
        CacheControl cacheControl = connection.getCacheControl(null);
        
        Cube[] cubes = connection.getSchema().getCubes();

        for (int i = 0; i < cubes.length; i++) {
            cacheControl.flush(cacheControl.createMeasuresRegion(cubes[i]));
        } 
        
        return new StatusMessage("Text","Test");
    }
    
    public StatusMessage clearCube(String catalog, String cube){

        int j = 0;
        Connection connection = getMdxConnection(catalog);
        CacheControl cacheControl = connection.getCacheControl(null);
        
        Cube[] cubes = connection.getSchema().getCubes();
        
        for (int i = 0; i < cubes.length; i++) {
            if(cubes[i].getName().equals(cube)){
                cacheControl.flush(cacheControl.createMeasuresRegion(cubes[i]));
                break;
            }
        } 
        
        return new StatusMessage("Text","Test");
    }
        
    public StatusMessage clearDimension(String catalog, String cube, String dimension){
        Connection connection = getMdxConnection(catalog);
        CacheControl cacheControl = connection.getCacheControl(null);
        
        Cube[] cubes = connection.getSchema().getCubes();
           
        for (int i = 0; i < cubes.length; i++) {
            if(cubes[i].getName().equals(cube)){
                Dimension[] dimensions = cubes[i].getDimensions();
                for(int j = 0; j < dimensions.length; j++){
                    if(dimensions[j].getName().equals(dimension)){
                        Hierarchy[] hierarchies = dimensions[j].getHierarchies();
                        for(int k = 0; k < hierarchies.length; k++){
                            cacheControl.flush(cacheControl.createMemberRegion(hierarchies[k].getAllMember(),true));
                        }
                        break;
                    }
                }
                break;
            }
        } 
        
        return new StatusMessage("Text","Test");
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

 
