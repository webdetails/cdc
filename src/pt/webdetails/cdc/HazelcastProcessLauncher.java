package pt.webdetails.cdc;

import java.io.File;

import org.pentaho.platform.engine.core.system.PentahoSystem;

public class HazelcastProcessLauncher {

  
  private static String getHazelcastClasspaths(){
    //TODO: this should get only needed jars, for now get everything
//    PentahoSystem.getApplicationContext().
    //TODO: 1) get server location (WEB-INF)
    String webInfPath = "/home/tgf/pentaho/target-dist/server";
    
//    File webInf = new File("/home/tgf/pentaho/target-dist/server");
    
    //TODO: 1.1) classes folder
    
    //TODO: 1.2) lib folder
    
    //TODO: 2) get CDA lib folder
    
    //TODO: 3) get CDC lib folder (?)
    
    return null;
  }
  
  
}
