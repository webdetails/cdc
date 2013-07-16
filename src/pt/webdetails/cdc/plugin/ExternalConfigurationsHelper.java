/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.plugin;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import pt.webdetails.cpf.repository.PentahoRepositoryAccess;

/**
 * Helper class to handle CDA configuration file.
 */
public class ExternalConfigurationsHelper {
  
  public static final String CDA_HAZELCAST_ADAPTER = CdcConfig.getConfig().getCdaHazelcastAdapterClass();
  public static final String CDA_DEFAULT_CACHE_ADAPTER = CdcConfig.getConfig().getCdaDefaultAdapterClass();
  
  private static final String CDA_PLUGIN_XML_PATH = PentahoRepositoryAccess.getRepository().getSolutionPath(CdcConfig.getConfig().getCdaConfigLocation());
  private static final String CDA_BEAN_ID =  CdcConfig.getConfig().getCdaCacheBeanId();
  
  private static Log logger = LogFactory.getLog(ExternalConfigurationsHelper.class);
  
  public static boolean isCdaHazelcastEnabled() throws DocumentException, IOException{
    return StringUtils.equals(getCdaQueryCache(), CDA_HAZELCAST_ADAPTER);
  }
  
  public static void setCdaHazelcastEnabled(boolean enabled) throws DocumentException, IOException {
    if (enabled && !StringUtils.equals(getCdaQueryCache(), CDA_HAZELCAST_ADAPTER)) {
      setCdaQueryCache(CDA_HAZELCAST_ADAPTER);
    } else if (!enabled && !StringUtils.equals(getCdaQueryCache(), CDA_DEFAULT_CACHE_ADAPTER)) {
      setCdaQueryCache(CDA_DEFAULT_CACHE_ADAPTER);
    }
  }
  
  private static void setCdaQueryCache(String className) throws DocumentException, IOException{
    Document doc = PentahoRepositoryAccess.getRepository().getResourceAsDocument(CdcConfig.getConfig().getCdaConfigLocation());// XmlDom4JHelper.getDocFromFile(CDA_PLUGIN_XML_PATH, null);

    
    Element elem = getCdaCacheBeanElement(doc); 
    String oldName = elem.attributeValue("class");
    logger.debug("Changing CDA query cache from "+ oldName + " to " + className);
    elem.attribute("class").setValue(className);    
    logger.debug("attempting to write CDA's plugin.xml...");
    FileWriter fw = null;
    try{
      fw = new FileWriter(CDA_PLUGIN_XML_PATH);
      XmlDom4JHelper.saveDomToWriter(doc, fw);
      fw.flush();
      logger.info("CDA plugin.xml overwritten! Plug-in should be restarted.");
    }
    finally{
      if(fw != null) fw.close(); 
    }
  }
  
  private static String getCdaQueryCache() throws DocumentException, IOException {
    Document doc = PentahoRepositoryAccess.getRepository().getResourceAsDocument(CdcConfig.getConfig().getCdaConfigLocation());//XmlDom4JHelper.getDocFromFile(CDA_PLUGIN_XML_PATH, null);
    Element elem = getCdaCacheBeanElement(doc); 
    return elem.attributeValue("class");
  }

    
  private static Element getCdaCacheBeanElement(Document doc) {
    
    Element root = doc.getRootElement();
    @SuppressWarnings("unchecked")
    Iterator<Element> it = ( Iterator<Element>) root.elementIterator("bean");
    Element elem = it.next();
    for(;it.hasNext(); elem = it.next()){
      if(elem.attributeValue("id").equals(CDA_BEAN_ID)){
        return elem;
      }
    }
    return null;
    
  }
  
}
