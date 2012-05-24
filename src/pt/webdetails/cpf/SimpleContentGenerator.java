/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cpf;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONException;
import org.pentaho.platform.api.engine.IParameterProvider;

import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.engine.services.solution.BaseContentGenerator;
import org.springframework.security.GrantedAuthorityImpl;

import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;

/**
 *
 * @author pdpi
 */
public abstract class SimpleContentGenerator extends BaseContentGenerator {

    private static final long serialVersionUID = 1L;
    protected Log logger = LogFactory.getLog(this.getClass());
   
    public static final String ENCODING = PluginSettings.ENCODING;

    @Override
    public void createContent() {
        IParameterProvider pathParams = parameterProviders.get("path");
        
        try {
          
            final OutputStream out = getResponseOutputStream("text/html");
            final Class<?>[] params = {OutputStream.class};

            String[] pathSections = StringUtils.split(pathParams.getStringParameter("path", null), "/");

            if (pathSections != null && pathSections.length > 0) {

                final String method = StringUtils.lowerCase(pathSections[0]);

                try {
                  final Method mthd = this.getClass().getMethod(method, params);
                  invokeMethod(out, method, mthd);
                  
                } catch (NoSuchMethodException e) {
                    logger.warn("could't locate method: " + method);
                } catch (InvocationTargetException e) {
                    logger.error(e.toString());

                } catch (IllegalAccessException e) {
                    logger.warn(e.toString());

                } catch (IllegalArgumentException e) {
                    logger.error(e.toString());
                }
            } else {
                logger.error("No method supplied.");
            }
        } catch (SecurityException e) {
            logger.warn(e.toString());
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }
    
    protected OutputStream getResponseOutputStream(final String mimeType) throws IOException {
      ServletResponse resp = getResponse();
      resp.setContentType(mimeType);
      return resp.getOutputStream();
      //TODO: deprecated, will be eliminated in sugar
      //return getResponse().getOutputStream();
      //return outputHandler.getOutputContentItem(IOutputHandler.RESPONSE, IOutputHandler.CONTENT, "", instanceId, mimeType).getOutputStream(null);
    }

    protected ServletRequest getRequest(){
      return (ServletRequest) parameterProviders.get("path").getParameter("httprequest");
    }
    
    protected ServletResponse getResponse(){
      return (ServletResponse) parameterProviders.get("path").getParameter("httpresponse");
    }
    
    protected IParameterProvider getRequestParameters(){
      return parameterProviders.get("request");
    }
    protected IParameterProvider getPathParameters(){
      return parameterProviders.get("path");
    }

    private boolean invokeMethod(final OutputStream out, final String methodName, final Method method) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
      
      Exposed exposed = method.getAnnotation(Exposed.class);
      if (exposed != null) {
        
        AccessLevel accessLevel = exposed.accessLevel();
        if(accessLevel != null) {
          
          boolean accessible = false;
          switch (accessLevel) {
            case ADMIN:
              accessible = SecurityHelper.isPentahoAdministrator(PentahoSessionHolder.getSession());
              break;
            case ROLE:
              String role = exposed.role();
              if (!StringUtils.isEmpty(role)) {
                accessible = SecurityHelper.isGranted(PentahoSessionHolder.getSession(), new GrantedAuthorityImpl(role));
              }
              break;
            case PUBLIC:
              accessible = true;
              break;
            default:
              logger.error("Unsupported AccessLevel " + accessLevel);
          }
          
          if (accessible) {
            method.invoke(this, out);
            return true;
          }
        }
        
      }
      logger.error("Method " + methodName + " not exposed or user does not have required permissions.");
      final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse");
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
    
    protected void writeOut(OutputStream out, String contents) throws IOException {
      StringBuffer buf = new StringBuffer(contents);
      IOUtils.write(buf, out);
    }
    
    protected void writeOut(OutputStream out, JsonSerializable contents) throws IOException, JSONException {
      StringBuffer buf = new StringBuffer(contents.toJSON().toString());
      IOUtils.write(buf, out);
    }

    @Override
    public Log getLogger() {
        return logger;
    }
    
    public abstract VersionChecker getVersionChecker();
}
