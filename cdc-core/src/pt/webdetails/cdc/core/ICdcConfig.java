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


import java.io.IOException;
import java.io.InputStream;

public interface ICdcConfig {

  public static final class CacheMaps {
    public static final String MONDRIAN_MAP = "mondrian";
    public static final String CDA_MAP = "cdaCache";
    public static final String CDA_STATS_MAP = "cdaCacheStats";
  }

  /**
   * hazelcast.xml file
   */
  public String getHazelcastConfigFile();

  /**
   * Should cdc hazelcast instance be a lite member
   */
  public boolean isLiteMode();

  /**
   * Debug mode logs entry events
   */
  public boolean isDebugMode();

  /**
   * Force hazelcast to run with provided configuration.
   */
  public boolean isForceConfig();

  /**
   * If should be used as mondrian cache
   */
  public boolean isMondrianCdcEnabled();

  /**
   * Notify mondrian of cache contents when starting up
   */
  public boolean isSyncCacheOnStart();

  /**
   * If {@link #isLaunchInnerProc()}, the -Xmx option
   */
  public String getVmMemory();

  /**
   * If should attempt to take over configuration and spawn a member node when none detected.
   */
  public boolean isMaster();

  /**
   * Send map configuration to joining members. <br/>{@link #isMaster()} must be true to enable this.
   */
  public boolean isSyncConfig();

  /**
   * Launch the spawned node as a new JVM process <br/>{@link #isMaster()} must be true to enable this.
   */
  public boolean isLaunchInnerProc();

  /**
   * If should attempt to inject Hazelcast mondrian cache
   */
  public boolean isRegisterMondrian();


}
