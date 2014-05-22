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
