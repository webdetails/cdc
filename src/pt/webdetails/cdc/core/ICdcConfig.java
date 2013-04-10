/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.core;


public interface ICdcConfig {

  public static final class CacheMaps {
    public static final String MONDRIAN_MAP = "mondrian";
    public static final String CDA_MAP = "cdaCache";
    public static final String CDA_STATS_MAP = "cdaCacheStats";
  }

  /**
   * hazelcast.xml location
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
   * Send map configuration to joining members.
   * <br/>{@link #isMaster()} must be true to enable this.
   */
  public boolean isSyncConfig();

  /**
   * Launch the spawned node as a new JVM process
   * <br/>{@link #isMaster()} must be true to enable this.
   */
  public boolean isLaunchInnerProc();

  /**
   * If should attempt to inject Hazelcast mondrian cache
   */
  public boolean isRegisterMondrian();


}