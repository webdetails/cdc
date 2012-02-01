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
  public static final String PLUGIN_NAME = "cdc";
  public static final String PLUGIN_PATH = "system/" + PLUGIN_NAME + "/";
  
  private static final String CDE_PLUGIN = "petaho-cdf-dd";
  
    @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void edit(OutputStream out) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdc/presentation/");
        params.put("file", "cdcHome.wcdf");
        params.put("absolute", "true");
        params.put("root", "localhost:8080");
        out.write(InterPluginComms.callPlugin(CDE_PLUGIN, "Render", params).getBytes(ENCODING));
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
        out.write(InterPluginComms.callPlugin(CDE_PLUGIN, "Render", params).getBytes(ENCODING));
    }




}
