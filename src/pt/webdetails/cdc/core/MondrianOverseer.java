package pt.webdetails.cdc.core;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hazelcast.core.MultiTask;

import pt.webdetails.cdc.hazelcast.operations.DistributedMapReinsertion;
import pt.webdetails.cdc.mondrian.SegmentCacheHazelcast;

import mondrian.spi.SegmentCache;
import mondrian.spi.SegmentCache.SegmentCacheInjector;
import mondrian.spi.SegmentCache.SegmentCacheListener;

public class MondrianOverseer {

  private static Log logger = LogFactory.getLog(MondrianOverseer.class);

  private SegmentCacheHazelcast mondrianCache;
  private ICdcConfig cdcConfig;

//  public MondrianOverseer(ICdcConfig config) {
//    cdcConfig = config;
//  }

  public void initMondrianCache(ICdcConfig config) {
    cdcConfig = config;
    registerMondrianCacheSpi(config.isDebugMode());
  }

  /**
   * Registers {@link SegmentCache} as mondrian cache
   * @param addDebugger add {@link MondrianLoggingListener} 
   */
  public void registerMondrianCacheSpi(boolean addDebugger) {
    
    logger.info("registering with mondrian");

    boolean cacheAlreadyIn = false;

    for(SegmentCache cache : SegmentCacheInjector.getCaches()) {
      if (cache instanceof SegmentCacheHazelcast) {
        cacheAlreadyIn = true;
        mondrianCache = (SegmentCacheHazelcast) cache;
        break;
      }
      else if (cdcConfig.isDebugMode() 
               && cache.getClass().getName().equals(SegmentCacheHazelcast.class.getName()))
      {
        // happens with an install plugin, its HazelcastManager will also be from a defunct classloader
        logger.fatal("Found orphan " + SegmentCacheHazelcast.class.getName() + " in mondrian caches, please restart server or Mondrian will not work properly");
        throw new RuntimeException("stale " + SegmentCacheHazelcast.class.getName());
      }
    }

    if (!cacheAlreadyIn) {
      mondrianCache = new SegmentCacheHazelcast(true);
    }
    if (cdcConfig.isDebugMode()) {
      logger.debug("adding mondrian listener");
      SegmentCacheListener listener = new MondrianLoggingListener(logger);
      mondrianCache.removeListener(listener);
      mondrianCache.addListener(listener);
    }

    if (!cacheAlreadyIn) {
      SegmentCacheInjector.addCache(mondrianCache);
    }
    else {
      logger.warn("CDC cache already in mondrian.");
    }
  }

  // TODO: testing
  // this will make every hz instance owining mondrian cache entries to reinsert them
  public int reloadMondrianCache() {
    ExecutorService execService = HazelcastManager.INSTANCE.getHazelcast().getExecutorService();

    MultiTask<Integer> reloadMondrianCache =
            new MultiTask<Integer>(
                new DistributedMapReinsertion(ICdcConfig.CacheMaps.MONDRIAN_MAP),
                HazelcastManager.INSTANCE.getHazelcast().getCluster().getMembers());
    execService.execute(reloadMondrianCache);
    //TODO: add timeout
    try {
      Collection<Integer> counts = reloadMondrianCache.get();
      int total = 0;
      for(int count : counts) {
        total += count;
      }
      return total;
    } catch (Exception e) {
      logger.error(e);
      return -1;
    }
  }
  /* ***********
   * LISTENERS *
   */

  /**
   * Mondrian listener that just logs cache additions and removals.
   */
  static class MondrianLoggingListener implements SegmentCacheListener {

    private Log logger;

    public MondrianLoggingListener(Log log) {
      this.logger = log;
    }
    @Override
    public void handle(SegmentCacheEvent event) {
      switch(event.getEventType()){
        case ENTRY_CREATED:
          logger.debug("(" + (event.isLocal()? "local" : "remote") + ") " + "Mondrian cache entry ADDED: " + event.getSource());
          break;
        case ENTRY_DELETED:
          logger.debug("(" + (event.isLocal()? "local" : "remote") + ") " + "Mondrian cache entry REMOVED: " + event.getSource());
          break;
      }
    }
        
    @Override 
    public boolean equals(Object other) {
      return other instanceof MondrianLoggingListener;
    }

  };
}
