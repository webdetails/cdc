/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSystem;


public class HazelcastProcessLauncher 
{
  private static String HAZELCAST_SERVER_CLASS = com.hazelcast.examples.StartServer.class.getName();
  private static String HAZELCAST_DEBUG_CLASS = com.hazelcast.examples.TestApp.class.getName(); 
  private static Log logger = LogFactory.getLog(HazelcastProcessLauncher.class);
  private static final String[] cachedPlugins = {
    "cda", "cdc"
  };
  
  private static final String[] JAVA_OPTIONS = {
    "-Djava.net.preferIPv4Stack=true",
    "-Dsun.lang.ClassLoader.allowArraySyntax=true", 
    "-Dhazelcast.serializer.shared=true"
  };
  
  public static void launchProcess(Long mem){
    String[] paths = getHazelcastClasspaths();
    String classPathArg = StringUtils.join(paths, File.pathSeparatorChar);
    String java = System.getProperty("java.home") + "/bin/java";
    Runtime runtime = Runtime.getRuntime();
    try {
      //run with whole classpath
      ArrayList<String> cmds = new ArrayList<String>();
      cmds.add(java);
      for(String opt: JAVA_OPTIONS){
        cmds.add(opt);
      }
      cmds.add(getHazelcastConfigOption());
      cmds.add("-cp");
      cmds.add(classPathArg);
      cmds.add(HAZELCAST_SERVER_CLASS);
      
      runtime.exec(cmds.toArray(new String[cmds.size()]), null, // new String[]{java,"-cp", classPathArg, HAZELCAST_SERVER_CLASS}, null,
          new File(PentahoSystem.getApplicationContext().getSolutionPath(CdcConfig.PLUGIN_SOLUTION_PATH)));

      logger.debug("process launched");
      
    } catch (IOException e) {
      logger.error(e);
    }
    
  }
  
  public static String createLauncherFile(boolean isDebugVersion){
    String[] paths = getHazelcastClasspaths();
    String classPathArg = StringUtils.join(paths, File.pathSeparatorChar);

    
    String osName = System.getProperty("os.name").toLowerCase();
    boolean isWindows = osName.contains("windows"); 

    String fileName = "launch-hazelcast" + (isDebugVersion? "-debug" : "") + (isWindows ? ".bat" : ".sh");

    StringBuilder contents = new StringBuilder();
    final String endLine = System.getProperty("line.separator");
    
    String opts = StringUtils.join(JAVA_OPTIONS, ' ');
    
    contents.append(isWindows ? "@echo off" : "#!/bin/sh")
            .append(endLine);
    
    contents.append(System.getProperty("java.home"))
            .append(File.separatorChar)
            .append("bin")
            .append(File.separatorChar)
            //TODO: java opts
            .append("java ")
            .append(opts)
            .append(' ')
            .append(getHazelcastConfigOption())
            .append(" -cp ")
            .append(classPathArg)
            .append(' ')
            .append(isDebugVersion? HAZELCAST_DEBUG_CLASS : HAZELCAST_SERVER_CLASS)
            .append(endLine);
    
    File shFile = new File(PentahoSystem.getApplicationContext().getSolutionPath(CdcConfig.PLUGIN_SOLUTION_PATH+fileName));
    
    FileOutputStream fileOut = null;
    try {
      if(shFile.exists()){
        shFile.delete();
      }
      shFile.createNewFile();
      
      fileOut = new FileOutputStream(shFile);
      IOUtils.write(contents.toString(), fileOut);
      fileOut.flush();
      
      if(!isWindows){
        shFile.setExecutable(true);
      }
      return shFile.getCanonicalPath();
    } catch (IOException e) {
      logger.error(e);
    }
    finally{
      IOUtils.closeQuietly(fileOut);
    }
    return null;
  }
  
  
  private static String getHazelcastConfigOption(){
    return "-Dhazelcast.config=" + CdcConfig.getHazelcastStandaloneConfigFile();
  }
  
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
 
  
}
