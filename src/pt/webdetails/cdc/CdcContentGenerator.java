/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import pt.webdetails.cpf.InterPluginComms;
import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;

/**
 *
 * @author pdpi
 */
public class CdcContentGenerator extends SimpleContentGenerator {

  private static final long serialVersionUID = 1L;
  
  public static final String ENCODING = "utf-8";
  
  
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void edit(OutputStream out) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdc/presentation/");
        params.put("file", "cdcHome.wcdf");
        params.put("absolute", "true");
        params.put("root", "http://localhost:8080");
        out.write(InterPluginComms.callPlugin(InterPluginComms.Plugin.CDE, "Render", params).getBytes(ENCODING));
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void home(OutputStream out) throws IOException {
    	Map<String, Object> params = getLink("cdcHome.wcdf");
    	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void clusterinfo(OutputStream out) throws IOException {
    	Map<String, Object> params = getLink("cdcClusterInfo.wcdf");
    	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void cacheinfo(OutputStream out) throws IOException {
    	Map<String, Object> params = getLink("cdcCacheInfo.wcdf");
    	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void settings(OutputStream out) throws IOException {
    	Map<String, Object> params = getLink("cdcSettings.wcdf");
    	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC) 
    public void cacheclean(OutputStream out) throws IOException {
      	Map<String, Object> params = getLink("cdcCacheClean.wcdf");
      	run(out, params);
    }

    private Map<String, Object> getLink(String dashboardName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdc/presentation/");
        params.put("file", dashboardName);
        params.put("bypassCache", "true");
        params.put("absolute", "true");
        params.put("root", "localhost:8080");
	
	return params;
    }


    private void run(OutputStream out, Map<String, Object> params) throws IOException {
        out.write(InterPluginComms.callPlugin(InterPluginComms.Plugin.CDE, "Render", params).getBytes(ENCODING));
    }




}
