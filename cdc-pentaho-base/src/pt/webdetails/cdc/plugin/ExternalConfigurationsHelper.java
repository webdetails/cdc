/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2002 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


package pt.webdetails.cdc.plugin;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import org.dom4j.io.SAXReader;
import pt.webdetails.cpf.repository.api.IRWAccess;
import pt.webdetails.cpf.repository.api.IReadAccess;

/**
 * Helper class to handle CDA configuration file.
 */
public class ExternalConfigurationsHelper {

  public static final String CDA_HAZELCAST_ADAPTER = CdcConfig.getConfig().getCdaHazelcastAdapterClass();
  public static final String CDA_DEFAULT_CACHE_ADAPTER = CdcConfig.getConfig().getCdaDefaultAdapterClass();

  private static final String CDA_BEAN_ID = CdcConfig.getConfig().getCdaCacheBeanId();
  private static final String CDA_CONFIG_FILE = CdcConfig.getConfig().getCdaConfigFile();

  private static Log logger = LogFactory.getLog( ExternalConfigurationsHelper.class );

  public static boolean isCdaHazelcastEnabled() throws DocumentException, IOException {
    return StringUtils.equals( getCdaQueryCache(), CDA_HAZELCAST_ADAPTER );
  }

  public static void setCdaHazelcastEnabled( boolean enabled ) throws DocumentException, IOException {
    if ( enabled && !StringUtils.equals( getCdaQueryCache(), CDA_HAZELCAST_ADAPTER ) ) {
      setCdaQueryCache( CDA_HAZELCAST_ADAPTER );
    } else if ( !enabled && !StringUtils.equals( getCdaQueryCache(), CDA_DEFAULT_CACHE_ADAPTER ) ) {
      setCdaQueryCache( CDA_DEFAULT_CACHE_ADAPTER );
    }
  }

  private static void setCdaQueryCache( String className ) throws DocumentException, IOException {

    Document doc = getDocument( getCdaWriteAccess().getFileInputStream( CDA_CONFIG_FILE ) );
    Element elem = getCdaCacheBeanElement( doc );
    String oldName = elem.attributeValue( "class" );
    logger.debug( "Changing CDA query cache from " + oldName + " to " + className );
    elem.attribute( "class" ).setValue( className );
    logger.debug( "attempting to write CDA's " + CDA_CONFIG_FILE + "..." );
    IRWAccess rw = getCdaWriteAccess();
    rw.saveFile( CDA_CONFIG_FILE, new ByteArrayInputStream( doc.asXML().getBytes() ) );
    logger.info( "CDA " + CDA_CONFIG_FILE + " overwritten! Plug-in should be restarted." );


  }

  private static String getCdaQueryCache() throws DocumentException, IOException {
    Document doc = getDocument( getCdaReadAccess().getFileInputStream( CDA_CONFIG_FILE ) );
    Element elem = getCdaCacheBeanElement( doc );
    return elem.attributeValue( "class" );
  }


  private static Element getCdaCacheBeanElement( Document doc ) {

    Element root = doc.getRootElement();
    @SuppressWarnings("unchecked")
    Iterator<Element> it = (Iterator<Element>) root.elementIterator( "bean" );
    Element elem = it.next();
    for (; it.hasNext(); elem = it.next() ) {
      if ( elem.attributeValue( "id" ).equals( CDA_BEAN_ID ) ) {
        return elem;
      }
    }
    return null;

  }

  private static Document getDocument( InputStream is ) throws IOException, DocumentException {
    SAXReader reader;
    Document doc = null;
    reader = new SAXReader();
    try {
      doc = reader.read( is );
    } finally {
      is.close();
    }

    return doc;
  }

  private static IReadAccess getCdaReadAccess() {
    return CdcEnvironment.getInstance().getOtherPluginSystemReader( "cda", "" );
  }

  private static IRWAccess getCdaWriteAccess() {
    return CdcEnvironment.getInstance().getOtherPluginSystemWriter( "cda", "" );
  }

}
