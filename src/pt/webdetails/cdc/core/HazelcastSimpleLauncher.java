package pt.webdetails.cdc.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Launches a non-lite Hazelcast instance.
 */
public class HazelcastSimpleLauncher implements IHazelcastLauncher {

  private static Log logger = LogFactory.getLog(HazelcastSimpleLauncher.class);

  HazelcastInstance hazelcast;
  Config hzConfig;

  public HazelcastSimpleLauncher(Config cfg) {
    hzConfig = HazelcastConfigHelper.clone(cfg);
    hzConfig.setLiteMember(false);
  }

  @Override
  public void start() {
    if (hazelcast == null) {
      logger.debug("starting..");
      hazelcast = Hazelcast.newHazelcastInstance(hzConfig);
      logger.debug("started");
    }
  }

  @Override
  public void stop() {
    if (hazelcast != null) {
      logger.debug("shutting down");
      hazelcast.getLifecycleService().shutdown();
      hazelcast = null;
    }
  }

  @Override
  public boolean isRunning() {
    boolean isRunning = hazelcast != null && hazelcast.getLifecycleService().isRunning();
    logger.debug(isRunning ? "running" : "-not running-");
    return isRunning;
  }

  
}
