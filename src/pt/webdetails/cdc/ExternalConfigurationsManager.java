package pt.webdetails.cdc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import mondrian.olap.MondrianProperties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.eigenbase.util.property.StringProperty;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.xml.dom4j.XmlDom4JHelper;


public class ExternalConfigurationsManager {
  
  public static final String CDA_HAZELCAST_ADAPTER = "pt.webdetails.cdc.cda.HazelcastQueryCache";
  public static final String CDA_DEFAULT_CACHE_ADAPTER = "pt.webdetails.cda.cache.EHCacheQueryCache";
  
  public static final String MONDRIAN_HAZELCAST_ADAPTER = "pt.webdetails.cdc.mondrian.SegmentCacheHazelcast";
  
  private static final String CDA_PLUGIN_XML_PATH = PentahoSystem.getApplicationContext().getSolutionPath("system/cda/plugin.xml");
  private static final String CDA_BEAN_ID = "cda.IQueryCache";
  private static final String MONDRIAN_PROPERTIES_LOCATION = PentahoSystem.getApplicationContext().getSolutionPath("system/mondrian/mondrian.properties");
  
  private static Log logger = LogFactory.getLog(ExternalConfigurationsManager.class);
  
  
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
  
  public static boolean isMondrianHazelcastEnabled(){
    return StringUtils.equals(MondrianProperties.instance().SegmentCache.get(), MONDRIAN_HAZELCAST_ADAPTER);
  }
  
  public static void setMondrianHazelcastEnabled(boolean enabled) throws FileNotFoundException, IOException
  {
    StringProperty mondrianCache = MondrianProperties.instance().SegmentCache;
    String mondrianCacheClassName = mondrianCache.get(); 
    
    String toChange = null;
    if(enabled && !StringUtils.equals(mondrianCacheClassName, MONDRIAN_HAZELCAST_ADAPTER)){
      toChange = MONDRIAN_HAZELCAST_ADAPTER;
    }
    else if(!enabled && StringUtils.equals(mondrianCacheClassName, MONDRIAN_HAZELCAST_ADAPTER)){
     toChange = StringUtils.EMPTY;//will make mondrian fallback to default
    }
    if(toChange != null){
      mondrianCache.set(toChange);
      //save
      FileOutputStream mondrianPropertiesOutStream = null;
      //FileInputStream 
      
      try{
        mondrianPropertiesOutStream =new FileOutputStream( new File(MONDRIAN_PROPERTIES_LOCATION));
        MondrianProperties.instance().store( mondrianPropertiesOutStream, "Changed via CDC!");
        mondrianPropertiesOutStream.flush();
        mondrianPropertiesOutStream.close();
        //sync
        MondrianProperties.instance().populate();
      } finally{
        IOUtils.closeQuietly(mondrianPropertiesOutStream);
        
      }
    }
  }
  
  private static void setCdaQueryCache(String className) throws DocumentException, IOException{
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
  
  private static String getCdaQueryCache() throws DocumentException, IOException {
    Document doc = XmlDom4JHelper.getDocFromFile(CDA_PLUGIN_XML_PATH, null);
    Element elem = getCdaCacheBeanElement(doc); 
    return elem.attributeValue("class");
  }
  
//  private static void setMondrianSegmentCache(String cacheClassName){
//    MondrianProperties.instance().SegmentCache.set(cacheClassName);
//  }
//  
//  private static String getMondrianSegmentCache(){
//    return MondrianProperties.instance().SegmentCache.get();
//        
//  // Other relevant properties (timeouts, in ms)        
//  //        mondrian.rolap.SegmentCacheLookupTimeout=500000
//  //        mondrian.rolap.SegmentCacheReadTimeout=50000
//  //        mondrian.rolap.SegmentCacheScanTimeout=50000
//  //        mondrian.rolap.SegmentCacheWriteTimeout=50000
//  }
    
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
