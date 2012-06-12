/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cdc;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.pentaho.platform.api.engine.IParameterProvider;

import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.VersionChecker;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;
import pt.webdetails.cpf.olap.OlapUtils;

/**
 *
 * @author pdpi
 */
public class CdcContentGenerator extends SimpleContentGenerator {

    private static final long serialVersionUID = 1L;

    private static final String UI_PATH = "cdc/presentation/";
    
    private static Map<String, Method> exposedMethods = new HashMap<String, Method>();
    static{
      //to keep case-insensitive methods
      exposedMethods = getExposedMethods(CdcContentGenerator.class, true);
    }
    
    @Override
    protected Method getMethod(String methodName) throws NoSuchMethodException {
      Method method = exposedMethods.get(StringUtils.lowerCase(methodName) );
      if(method == null) {
        throw new NoSuchMethodException();
      }
      return method;
    }
    
    @Override
    public String getPluginName(){
      return "cdc";
    }
  
    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void home(OutputStream out) throws IOException {
        Map<String, Object> params = getRenderRequestParameters("cdcHome.wcdf");
        renderInCde(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void clusterInfo(OutputStream out) throws IOException {
        Map<String, Object> params = getRenderRequestParameters("cdcClusterInfo.wcdf");
        renderInCde(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void cacheInfo(OutputStream out) throws IOException {        
        Map<String, Object> params = getRenderRequestParameters("cdcCacheInfo.wcdf");
        renderInCde(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void settings(OutputStream out) throws IOException {
        Map<String, Object> params = getRenderRequestParameters("cdcSettings.wcdf");
        renderInCde(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN) 
    public void cacheClean(OutputStream out) throws IOException {
        Map<String, Object> params = getRenderRequestParameters("cdcCacheClean.wcdf");
        renderInCde(out, params);
    }
    
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void about(OutputStream out) throws IOException {
      renderInCde(out, getRenderRequestParameters("cdcAbout.wcdf"));
    }
    
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void olapUtils(OutputStream out) {
        OlapUtils utils = new OlapUtils();
        IParameterProvider requestParams = getRequestParameters();
        try {
            out.write(utils.process(requestParams).toString().getBytes(ENCODING));
        } catch (IOException e) {
            logger.error(e);
        }
    }
    
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void checkVersion(OutputStream out) throws IOException, JSONException {
      writeOut(out, getVersionChecker().checkVersion());
    }
    
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void getVersion(OutputStream out) throws IOException, JSONException {
      writeOut(out, getVersionChecker().getVersion());
    }
    
    /**
     * Set up parameters to render a dashboard from the presentation layer
     * @param dashboardName
     * @return
     */
    private Map<String, Object> getRenderRequestParameters(String dashboardName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", UI_PATH);
        params.put("file", dashboardName);
        params.put("bypassCache", "true");
        params.put("absolute", "true");
        params.put("root", getRoot());

        //add request parameters
        ServletRequest request = getRequest();
        @SuppressWarnings("unchecked")//should always be String
        Enumeration<String> originalParams =  request.getParameterNames();
        // Iterate and put the values there
        while(originalParams.hasMoreElements()) {
            String originalParam = originalParams.nextElement();
            params.put(originalParam, request.getParameter(originalParam));
        }
        
        return params;
    }

    /**
     * Display a CDE dashboard
     * @param out
     * @param params
     * @throws IOException
     */
    private void renderInCde(OutputStream out, Map<String, Object> params) throws IOException {
      InterPluginCall pluginCall = new InterPluginCall(InterPluginCall.CDE, "Render", params);
      pluginCall.setResponse(getResponse());
      pluginCall.setOutputStream(out);
      pluginCall.run();
    }

    private String getRoot() {
        ServletRequest request = getRequest();
        String root = request.getServerName() + ":" + request.getServerPort();
        return root;
    }
    
    @Override
    protected String getDefaultPath(String path){
      if(StringUtils.endsWith(path, "/")){
        return "home";
      }
      else {
        return "cdc/home";
      }
    }

    public VersionChecker getVersionChecker() {
      
      return new VersionChecker(CdcConfig.getConfig()){

        @Override
        protected String getVersionCheckUrl(VersionChecker.Branch branch) {
          switch(branch){
            case TRUNK:
              return "http://ci.analytical-labs.com/job/Webdetails-CDC/lastSuccessfulBuild/artifact/dist/marketplace.xml";
            case STABLE:
              return "http://ci.analytical-labs.com/job/Webdetails-CDC-Release/lastSuccessfulBuild/artifact/dist/marketplace.xml";
            default:
              return null;
          }
          
        }
        
      };
    }
}
