/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.webdetails.cpf;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.platform.engine.services.solution.BaseContentGenerator;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;

/**
 *
 * @author pdpi
 */
public class SimpleContentGenerator extends BaseContentGenerator {


  private static final long serialVersionUID = 1L;
    Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void createContent() {
        IParameterProvider pathParams = parameterProviders.get("path");
                //requestParams = parameterProviders.get("request");
        final IContentItem contentItem = outputHandler.getOutputContentItem("response", "content", "", instanceId, "text/html");

        try {
            final OutputStream out = contentItem.getOutputStream(null);
            final Class<?>[] params = {OutputStream.class};

            final String method = pathParams.getStringParameter("path", null).split("/")[1].toLowerCase();

            try {
                final Method mthd = this.getClass().getMethod(method, params);
                boolean exposed = mthd.isAnnotationPresent(Exposed.class);
                boolean accessible = exposed && mthd.getAnnotation(Exposed.class).accessLevel() == AccessLevel.PUBLIC;
                if (accessible) {
                    mthd.invoke(this, out);
                } else {
                    throw new IllegalAccessException("Method " + method + " has the wrong access level");
                }
            } catch (NoSuchMethodException e) {
                logger.warn("could't locate method: " + method);
            } catch (InvocationTargetException e) {
                logger.error(e.toString());

            } catch (IllegalAccessException e) {
                logger.warn(e.toString());

            } catch (IllegalArgumentException e) {

                logger.error(e.toString());
            }
        } catch (SecurityException e) {
            logger.warn(e.toString());
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    @Override
    public Log getLogger() {
        return logger;
    }
}
