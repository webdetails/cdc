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


package pt.webdetails.cdc.core;

import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigXmlGenerator;
import pt.webdetails.cdc.plugin.CdcConfig;
import pt.webdetails.cdc.plugin.CdcEnvironment;
import pt.webdetails.cpf.repository.api.IRWAccess;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class HazelcastConfigHelper extends CoreHazelcastConfigHelper {

  public static boolean saveConfig() {
    return saveConfig( HazelcastManager.INSTANCE.getHazelcast().getConfig() );
  }

  public static boolean saveConfig( Config config ) {
    return saveConfig( config, CdcConfig.getConfig().getHazelcastConfigFile() );
  }

  public static boolean saveConfig( Config config, InputStream file ) {
    IRWAccess rwAccess = CdcEnvironment.getInstance().getPluginSystemWriter( "" );
    if ( file != null ) {
      ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator( true );
      rwAccess.saveFile( CdcConfig.HAZELCAST_FILE,
          new ByteArrayInputStream( xmlGenerator.generate( config ).getBytes() ) );
      CoreHazelcastConfigHelper.spreadMapConfigs();
      return true;
    }
    logger.error( "Couldn't save config, null1" );
    return false;

  }


}
