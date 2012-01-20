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
        params.put("file", "cdcExample.wcdf");
        params.put("absolute", "true");
        params.put("root", "localhost:8080");
        out.write(InterPluginComms.callPlugin("pentaho-cdf-dd", "Render", params).getBytes("utf-8"));
    }


}
