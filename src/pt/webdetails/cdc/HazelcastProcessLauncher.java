/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;


public class HazelcastProcessLauncher 
{
  public static final String INNER_PROC_VAR = "pt.webdetails.cdc.isInnerProc"; 
  private static String HAZELCAST_SERVER_CLASS = pt.webdetails.cdc.hazelcast.StartServer.class.getName();
//  private static String HAZELCAST_DEBUG_CLASS = pt.webdetails.cdc.hazelcast.CfgTestApp.class.getName(); 
  private static Log logger = LogFactory.getLog(HazelcastProcessLauncher.class);
  private static final String[] classpathPlugins = {
    "cda", "cdc"
  };
  
  private static final String[] JAVA_OPTIONS = {
    "-Djava.net.preferIPv4Stack=" + getPreferIPv4Stack(), 
    "-Dsun.lang.ClassLoader.allowArraySyntax=true", 
    "-Dhazelcast.serializer.shared=true",
    "-Dhazelcast.logging.type=log4j",
    "-D" + INNER_PROC_VAR + "=true",
  };
  
  private static class ProcessOutputLogger implements Runnable{

    private BufferedReader reader;
    private Log log;
    
    public ProcessOutputLogger(InputStream is, Log log){
      this.log = log;
      this.reader = new BufferedReader(new InputStreamReader(is));
    }
    
    @Override
    public void run() {
      try{
        String line=null;
        while ( (line = reader.readLine()) != null){
            log.debug(line);    
        }
      } 
      catch (IOException ioe) {
        log.error(ioe);
      }
      finally{
        IOUtils.closeQuietly(reader);
      }
    }

  }
  
  private static class ProcessExitListener implements Runnable{//Callable<Integer>{

    private Process proc;
    
    public ProcessExitListener(Process proc){
      this.proc = proc;
    }
    @Override
    public void run() {
      try{
          int ret = proc.waitFor();
          logger.info("Process exit: " + ret);

      } catch(Exception e){
        logger.error(e);
      }
    }
    
  }
  
  public static Process launchProcess(){
    String[] paths = getHazelcastClasspaths();
    
    String classPathArg = StringUtils.join(paths, File.pathSeparatorChar);
    String java = joinPath(System.getProperty("java.home"), "bin", "java");
    
    Config config = null;
    XmlConfigBuilder configBuilder;

    try {
      configBuilder = new XmlConfigBuilder(getHazelcastStandaloneConfigFile());
      config = configBuilder.build();
    } catch (FileNotFoundException e1) {
      logger.error("launchProcess: " + getHazelcastStandaloneConfigFile() + " not found! Creating from current config.");
      config = HazelcastConfigHelper.cloneConfig(Hazelcast.getConfig());
      HazelcastConfigHelper.saveConfig(config, getHazelcastStandaloneConfigFile());
    }

    try {
      //run with whole classpath
      ArrayList<String> cmds = new ArrayList<String>();
      cmds.add(java);
      for(String opt: JAVA_OPTIONS){
        cmds.add(opt);
      }
      
      //memory
      cmds.add("-Xmx" + CdcConfig.getConfig().getVmMemory()); 
      
      cmds.add(getHazelcastConfigOption());
      cmds.add("-cp");
      cmds.add(classPathArg);
      cmds.add(HAZELCAST_SERVER_CLASS);
      
      String[] cmdsArray = cmds.toArray(new String[cmds.size()]);

      logger.debug("launching process: " + StringUtils.join(cmdsArray, " "));
      
      ProcessBuilder pBuild = new ProcessBuilder(cmds);
      pBuild.directory(new File(PentahoSystem.getApplicationContext().getSolutionPath(joinPath("system", "cdc"))));
      Process proc = pBuild.start();
      new Thread( new ProcessOutputLogger(proc.getErrorStream(), logger)).start();
      new Thread( new ProcessOutputLogger(proc.getInputStream(), logger)).start();
      new Thread( new ProcessExitListener(proc)).start();
      
      return proc;
    } catch (Exception e) {
      logger.error(e);
      return null;
    }
    
  }
  
  private static String getPreferIPv4Stack() {
    String prop = System.getProperty("java.net.preferIPv4Stack");
    return prop == null ? "false" : prop;
  }

  private static String getHazelcastConfigOption(){
    return "-Dhazelcast.config=" + getHazelcastStandaloneConfigFile();
  }
  
  private static String[] getHazelcastClasspaths(){
    ArrayList<String> paths = new ArrayList<String>();
    
    String webInfPath = PentahoSystem.getApplicationContext().getApplicationPath("WEB-INF");
    File webInfDir = new File(webInfPath);
    if(webInfDir.exists() && webInfDir.isDirectory()){
      paths.add(joinPath(webInfPath,"classes"));
      paths.add(joinPath(webInfPath, "lib", "*"));
    }
    
    for(String plugin: classpathPlugins){
      paths.add( joinPath(PentahoSystem.getApplicationContext().getSolutionPath("system"), plugin,"lib","*"));
    }

    return paths.toArray(new String[paths.size()]);
  }
  
  private static String joinPath(String... path){
    return StringUtils.join(path,File.separatorChar);
  }
  
  private static String getHazelcastStandaloneConfigFile(){
    String cfgPath = CdcConfig.getHazelcastStandaloneConfigFile();
    if(File.separatorChar != '/'){
      cfgPath = cfgPath.replace('/', File.separatorChar);
    }
    return cfgPath;
  }
  
}
