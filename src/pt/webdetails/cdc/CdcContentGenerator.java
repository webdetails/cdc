/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cdc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.pentaho.platform.api.engine.IParameterProvider;

import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;
import pt.webdetails.cpf.olap.OlapUtils;

/**
 *
 * @author pdpi
 */
public class CdcContentGenerator extends SimpleContentGenerator {

    private static final long serialVersionUID = 1L;
    public static final String ENCODING = "utf-8";

    private static final String UI_PATH = "cdc/presentation/";
  
    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void home(OutputStream out) throws IOException {
        Map<String, Object> params = getRequestParameters("cdcHome.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void clusterinfo(OutputStream out) throws IOException {
        Map<String, Object> params = getRequestParameters("cdcClusterInfo.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void cacheinfo(OutputStream out) throws IOException {        
        Map<String, Object> params = getRequestParameters("cdcCacheInfo.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN)
    public void settings(OutputStream out) throws IOException {
        Map<String, Object> params = getRequestParameters("cdcSettings.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.ADMIN) 
    public void cacheclean(OutputStream out) throws IOException {
        Map<String, Object> params = getRequestParameters("cdcCacheClean.wcdf");
        run(out, params);
    }
    

    private Map<String, Object> getRequestParameters(String dashboardName) {
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

    //delegate response to cde
    private void run(OutputStream out, Map<String, Object> params) throws IOException {
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

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void olaputils(OutputStream out) {
        OlapUtils utils = new OlapUtils();
        IParameterProvider requestParams = parameterProviders.get("request");
        try {
            out.write(utils.process(requestParams).toString().getBytes(ENCODING));
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
