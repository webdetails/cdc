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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cdc.hazelcast.operations.DistributedMapConfig;

import pt.webdetails.cpf.utils.CharsetHelper;

import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigXmlGenerator;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Member;

public class CoreHazelcastConfigHelper {

  static Log logger = LogFactory.getLog( CoreHazelcastConfigHelper.class );

  public enum MapConfigOption {

    maxSizePolicy,
    maxSize,
    evictionPercentage,
    evictionPolicy,
    timeToLive,

    enabled;

    public static MapConfigOption parse( String value ) {
      try {
        return valueOf( value );
      } catch ( IllegalArgumentException e ) {
        return null;
      }
    }

  }

  public static final String[] MAX_SIZE_POLICIES = new String[] {
      MaxSizeConfig.POLICY_CLUSTER_WIDE_MAP_SIZE,
      MaxSizeConfig.POLICY_MAP_SIZE_PER_JVM,
      MaxSizeConfig.POLICY_PARTITIONS_WIDE_MAP_SIZE,
      MaxSizeConfig.POLICY_USED_HEAP_PERCENTAGE,
      MaxSizeConfig.POLICY_USED_HEAP_SIZE
  };

  static {
    Arrays.sort( MAX_SIZE_POLICIES );
  }

  public static final String[] EVICTION_POLICIES = new String[] {
      "LRU", "LFU", "NONE"
  };

  static {
    Arrays.sort( EVICTION_POLICIES );
  }

  /**
   * Sets MapConfig in Member
   *
   * @param mapConfig the map configuration to send
   * @param member
   * @return
   * @throws InterruptedException
   */
  public static Boolean spreadMapConfig( MapConfig mapConfig, Member member ) {
    DistributedTask<Boolean> distribMapConfig =
        new DistributedTask<Boolean>( new DistributedMapConfig( mapConfig ), member );
    ExecutorService execService = HazelcastManager.INSTANCE.getHazelcast().getExecutorService();
    execService.execute( distribMapConfig );
    try {
      return distribMapConfig.get();
    } catch ( Exception e ) {
      logger.error( "Error attempting to send MapConfig " + mapConfig.getName() + " to " + member.toString() );
      return false;
    }
  }

  public static Collection<Boolean> spreadMapConfigs() {
    Collection<Boolean> result = null;
    for ( MapConfig mapConfig : HazelcastManager.INSTANCE.getHazelcast().getConfig().getMapConfigs().values() ) {
      if ( result == null ) {
        result = spreadMapConfig( mapConfig );
      } else {
        result.addAll( spreadMapConfig( mapConfig ) );
      }
    }
    return result;
  }

  public static void spreadMapConfigs( Member member ) {
    for ( MapConfig mapConfig : HazelcastManager.INSTANCE.getHazelcast().getConfig().getMapConfigs().values() ) {
      boolean ok = false;
      try {
        ok = spreadMapConfig( mapConfig, member );
      } catch ( Exception e ) {
        logger.error( "Could not send map configuration " + mapConfig.getName() + " to " + member, e );
      }
      logger.info( "sending map " + mapConfig.getName() + " to " + member + "..." + ( ok ? "OK" : "FAILED" ) );
    }
  }

  public static Collection<Boolean> spreadMapConfig( MapConfig mapConfig ) {

    ArrayList<Boolean> boolist = new ArrayList<Boolean>();
    for ( Member member : HazelcastManager.INSTANCE.getHazelcast().getCluster().getMembers() ) {
      if ( !member.localMember() ) { //skip this
        boolist.add( CoreHazelcastConfigHelper.spreadMapConfig( mapConfig, member ) );
      }
    }
    return boolist;
  }


  public static Config cloneConfig( Config config ) {
    ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator( false );
    String configXml = xmlGenerator.generate( config );
    return new InMemoryXmlConfig( configXml );
  }

  /**
   * clones via xml serialization
   */
  public static Config clone( Config config ) {
    ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator( true );
    XmlConfigBuilder builder = null;
    try {
      String xmlConfig = xmlGenerator.generate( config );
      InputStream input = new ByteArrayInputStream( xmlConfig.getBytes( CharsetHelper.getEncoding() ) );
      builder = new XmlConfigBuilder( input );
    } catch ( UnsupportedEncodingException e ) {
      // TODO Auto-generated catch block
      logger.error( e );
    }
    return builder.build();
  }

  public static boolean saveConfig( Config config, String fileName ) {
    File configFile = new File( fileName );

    if ( !configFile.exists() ) {
      logger.info( fileName + " does not exist, creating." );
      try {
        configFile.createNewFile();
      } catch ( IOException e ) {
        logger.error( "Error attempting to create file " + fileName );
        return false;
      }
    }

    FileWriter fileWriter = null;
    try {
      ConfigXmlGenerator xmlGenerator = new ConfigXmlGenerator( true );
      fileWriter = new FileWriter( configFile );
      fileWriter.write( xmlGenerator.generate( config ) );
      CoreHazelcastConfigHelper.spreadMapConfigs();
      return true;
    } catch ( FileNotFoundException e ) {
      String msg = "File not found: " + configFile.getAbsolutePath();
      logger.error( msg );
      return false;
    } catch ( IOException e ) {
      String msg = "Error writing file " + configFile.getAbsolutePath();
      logger.error( msg, e );
      return false;
    } finally {
      IOUtils.closeQuietly( fileWriter );
    }
  }

}
