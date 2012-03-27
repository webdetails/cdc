/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cdc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.security.SecurityHelper;


import pt.webdetails.cpf.InterPluginComms;
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

  
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void home(OutputStream out) throws IOException {
        if (!validateAccess())
            return;
        
        Map<String, Object> params = getLink("cdcHome.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void clusterinfo(OutputStream out) throws IOException {
        if (!validateAccess())
            return;
        
        Map<String, Object> params = getLink("cdcClusterInfo.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void cacheinfo(OutputStream out) throws IOException {        
        if (!validateAccess())
            return;
                
        Map<String, Object> params = getLink("cdcCacheInfo.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void settings(OutputStream out) throws IOException {
        if (!validateAccess())
            return;
                
        Map<String, Object> params = getLink("cdcSettings.wcdf");
        run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC) 
    public void cacheclean(OutputStream out) throws IOException {
        if (!validateAccess())
            return;
                        
        Map<String, Object> params = getLink("cdcCacheClean.wcdf");
        run(out, params);
    }

    
    private boolean validateAccess() {
        IPentahoSession userSession = PentahoSessionHolder.getSession();
        if (!SecurityHelper.isPentahoAdministrator(userSession)) {
            final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            logger.warn("Access denied for user: " + userSession.getName() + ". Not an admin");
            return false;
        }
        
        return true;                
    }
    
    private Map<String, Object> getLink(String dashboardName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdc/presentation/");
        params.put("file", dashboardName);
        params.put("bypassCache", "true");
        params.put("absolute", "true");
        params.put("root", getRoot());

        ServletRequestWrapper wrapper = (ServletRequestWrapper) parameterProviders.get("path").getParameter("httprequest");
        Enumeration originalParams = wrapper.getParameterNames();
        // Iterate and put the values there
        while(originalParams.hasMoreElements()) {
            String originalParam = (String) originalParams.nextElement();
            params.put(originalParam,wrapper.getParameter(originalParam));
        }
        
        
        return params;
    }

    private void run(OutputStream out, Map<String, Object> params) throws IOException {
        out.write(InterPluginComms.callPlugin(InterPluginComms.Plugin.CDE, "Render", params).getBytes(ENCODING));
    }

    private String getRoot() {

        IParameterProvider pathParams = parameterProviders.get("path");
        ServletRequestWrapper wrapper = (ServletRequestWrapper) pathParams.getParameter("httprequest");
        String root = wrapper.getServerName() + ":" + wrapper.getServerPort();
        return root;
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void olaputils(OutputStream out) {
        OlapUtils utils = new OlapUtils();
        IParameterProvider requestParams = parameterProviders.get("request");
        try {
            out.write(utils.process(requestParams).toString().getBytes("utf-8"));
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
