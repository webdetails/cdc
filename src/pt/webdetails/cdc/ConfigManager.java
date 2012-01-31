package pt.webdetails.cdc;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.xml.dom4j.XmlDom4JHelper;

public class ConfigManager {
  
  private static final String CDA_PLUGIN_XML_PATH = PentahoSystem.getApplicationContext().getSolutionPath("system/cda/plugin.xml");
  private static final String CDA_BEAN_ID = "cda.IQueryCache";
  
  private static Log logger = LogFactory.getLog(ConfigManager.class);
  
  public static void setCdaQueryCache(String className) throws DocumentException, IOException{
    Document doc = XmlDom4JHelper.getDocFromFile(CDA_PLUGIN_XML_PATH, null);
    
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
  
  public static String getCdaQueryCache() throws DocumentException, IOException {
    Document doc = XmlDom4JHelper.getDocFromFile(CDA_PLUGIN_XML_PATH, null);
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
  
  
  //OLD
  public static void testCdaConfigSwitch() throws DocumentException, IOException
  {
    Document doc = XmlDom4JHelper.getDocFromFile(CDA_PLUGIN_XML_PATH, null);
    
    //Node beanNode = doc.selectSingleNode("//plugin/bean[id=\"cda." + IQueryCache.class.getSimpleName() + "\"]");
    Element root = doc.getRootElement();
    Iterator<Element> it = ( Iterator<Element>) root.elementIterator("bean");
    Element elem = it.next();
    for(;it.hasNext(); elem = it.next()){
      if(elem.attributeValue("id").equals("cda.IQueryCache")){
        String oldName = elem.attributeValue("class");
        elem.attribute("class").setValue("pt.webdetails.cdc.cda.HazelcastQueryCache");
        break;
      }
    }
    FileWriter fw = null;
    try{
      fw = new FileWriter(CDA_PLUGIN_XML_PATH);
      XmlDom4JHelper.saveDomToWriter(doc, fw);
      fw.flush();
    }
    finally{
      if(fw != null) fw.close(); 
    }
    //bean[@id='cda.IQueryCache']
    
  }
  
}
