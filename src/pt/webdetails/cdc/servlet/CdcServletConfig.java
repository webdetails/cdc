package pt.webdetails.cdc.servlet;

import javax.servlet.ServletConfig;

import org.apache.commons.lang.StringUtils;

import pt.webdetails.cdc.core.ICdcConfig;

// TODO: option names need to be more similar to CdcConfig 
public class CdcServletConfig implements ICdcConfig {

  private ServletConfig servletConfig;

  public CdcServletConfig(ServletConfig config) {
    this.servletConfig = config;
  }

  public String getHazelcastConfigFile() {
    String configFile = servletConfig.getInitParameter("hazelcast.config");
    if(StringUtils.isEmpty(configFile)) {
      configFile = "hazelcast.xml";
    }
    return configFile;
  }

  public boolean enableShutdownThread() {
    return false;
  }

  public boolean isLiteMode() {
    String isLiteMode = servletConfig.getInitParameter("hazelcast.lite.member");
    if(!StringUtils.isEmpty(isLiteMode)){
      return Boolean.parseBoolean(isLiteMode);
    }
    else {
      return false;
    }
  }

  public boolean isDebugMode() {
    String debugMode = servletConfig.getInitParameter("webdetails.cdc.debug");
    if(!StringUtils.isEmpty(debugMode)){
      return Boolean.parseBoolean(debugMode);
    }
    else {
      return false;
    }
  }

  public boolean isForceConfig() {
    return false;
  }

  public boolean isMondrianCdcEnabled() {
    // just assuming it is
    return true;
  }

  public boolean isSyncCacheOnStart() {
    return false;
  }

  public String getVmMemory() {
    // disabled, depends on master
    return "512m";
  }

  public boolean isMaster() {
    // will always default to slave for now, it doesn't do any master stuff anyway
    return false;
  }

  public boolean isSyncConfig() {
    return false;
  }

  public boolean isLaunchInnerProc() {
    return false;
  }

  public boolean isRegisterMondrian() {//TODO:
    return true;
  }

}
