/*!
* Copyright 2002 - 2014 Webdetails, a Pentaho company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

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
