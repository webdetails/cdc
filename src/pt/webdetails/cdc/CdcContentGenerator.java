/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void edit(OutputStream out) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdc/presentation/");
        params.put("file", "cdcHome.wcdf");
        params.put("absolute", "true");
        params.put("root", "localhost:8080");
        out.write(InterPluginComms.callPlugin("pentaho-cdf-dd", "Render", params).getBytes("utf-8"));
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
    public void cdasettings(OutputStream out) throws IOException {
	Map<String, Object> params = getLink("cdcCDASettings.wcdf");
	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void mondriansettings(OutputStream out) throws IOException {
	Map<String, Object> params = getLink("cdcMondrianSettings.wcdf");
	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void cdacacheclean(OutputStream out) throws IOException {
	Map<String, Object> params = getLink("cdcCDACacheClean.wcdf");
	run(out, params);
    }

    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void mondriancacheclean(OutputStream out) throws IOException {
	Map<String, Object> params = getLink("cdcMondrianCacheClean.wcdf");
	run(out, params);
    }


    private Map<String, Object> getLink(String dashboardName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdc/presentation/");
        params.put("file", dashboardName);
        params.put("absolute", "true");
        params.put("root", "localhost:8080");
	
	return params;
    }


    private void run(OutputStream out, Map<String, Object> params) throws IOException {
        out.write(InterPluginComms.callPlugin("pentaho-cdf-dd", "Render", params).getBytes("utf-8"));
    }




}
