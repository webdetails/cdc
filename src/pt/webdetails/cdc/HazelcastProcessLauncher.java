package pt.webdetails.cdc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSystem;


public class HazelcastProcessLauncher 
{
  private static String classToRun = com.hazelcast.examples.StartServer.class.getName();
  private static Log logger = LogFactory.getLog(HazelcastProcessLauncher.class);
  private static final String[] cachedPlugins = {
    "cda"
  };
  
  private static String[] getHazelcastClasspaths(){
    ArrayList<String> paths = new ArrayList<String>();
    
    String webInfPath = PentahoSystem.getApplicationContext().getApplicationPath("WEB-INF");
    File webInfDir = new File(webInfPath);
    if(webInfDir.exists() && webInfDir.isDirectory()){
      paths.add(webInfPath + "/classes");
      paths.add(webInfPath + "/lib/*");
    }
    
    for(String plugin: cachedPlugins){
      paths.add( PentahoSystem.getApplicationContext().getSolutionPath("system/" + plugin + "/lib/*"));
    }

    return paths.toArray(new String[paths.size()]);
  }
  
  public static void launchProcess(Long mem){
    //TODO: jvm options
    String[] paths = getHazelcastClasspaths();
    String classPathArg = StringUtils.join(paths, File.pathSeparatorChar);
    String java = System.getProperty("java.home") + "/bin/java";
    
    Runtime runtime = Runtime.getRuntime();
    try {
      //run with whole classpath
      runtime.exec(new String[]{java,"-cp", classPathArg, classToRun}, null,
          new File(PentahoSystem.getApplicationContext().getSolutionPath(CdcContentGenerator.PLUGIN_PATH)));

      logger.debug("process launched");
      
    } catch (IOException e) {
      logger.error(e);
    }
    
  }
 
  
}
