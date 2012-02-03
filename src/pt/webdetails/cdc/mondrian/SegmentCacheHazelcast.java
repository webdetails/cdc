/*
// $Id: //open/mondrian/src/main/mondrian/spi/SegmentCache.java#3 $
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2011 Pedro Alves and others
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
//
 */
package pt.webdetails.cdc.mondrian;

import java.util.List;
import java.util.concurrent.Future;

import mondrian.rolap.agg.SegmentBody;
import mondrian.rolap.agg.SegmentHeader;
import mondrian.rolap.agg.SegmentHeader.ConstrainedColumn;
import mondrian.spi.SegmentCache;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import mondrian.olap.Util;

/**
 * Implementation
 * 
 * 
 * @author pedro
 */
public class SegmentCacheHazelcast implements SegmentCache {

    private static final String MAP = "mondrian";
    
   private static IMap<SegmentHeader, SegmentBody> cache ;
    
    private static final synchronized IMap<SegmentHeader, SegmentBody> getCache(){
      if(cache == null){
        cache = Hazelcast.getMap(MAP);
      }
      return cache;
    }
    
    public static synchronized void invalidateCache(){
      cache = null;
    }
    
    /**
     * Executor for the tests. Thread-factory ensures that thread does not
     * prevent shutdown.
     */
    private static final ExecutorService executor =
            Util.getExecutorService(1, "mondrian.rolap.agg.MockSegmentCache$ExecutorThread");

    @Override
    public Future<SegmentBody> get(final SegmentHeader header) {
        return executor.submit(
                new Callable<SegmentBody>() {

                    @Override
                    public SegmentBody call() throws Exception {
                        return getCache().get(header);
                    }
                });
    }

    @Override
    public Future<Boolean> contains(final SegmentHeader header) {
        return executor.submit(
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        return getCache().containsKey(header);
                    }
                });
    }

    @Override
    public Future<List<SegmentHeader>> getSegmentHeaders() {
        return executor.submit(
                new Callable<List<SegmentHeader>>() {

                    public List<SegmentHeader> call() throws Exception {
                        return new ArrayList<SegmentHeader>(getCache().keySet());
                    }
                });

    }

    @Override
    public Future<Boolean> put(final SegmentHeader header, final SegmentBody body) {

        return executor.submit(
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                      getCache().put(header, body);
                        return true;
                    }
                });
    }

    @Override
    public Future<Boolean> remove(final SegmentHeader header) {

        return executor.submit(
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                      getCache().remove(header);
                        return true;
                    }
                });

    }

    @Override
    public Future<Boolean> flush(final ConstrainedColumn[] region) {
        return executor.submit(
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {

                        final Set<SegmentHeader> toEvict = new HashSet<SegmentHeader>();
                        for (SegmentHeader sh : getCache().keySet()) {

                            final List<ConstrainedColumn> cc2 = Arrays.asList(region);
                            for (ConstrainedColumn cc : region) {
                                if (cc2.contains(cc.getColumnExpression())) {
                                    // Must flush.
                                    toEvict.add(sh);
                                }
                            }
                        }
                        for (SegmentHeader sh : toEvict) {
                          getCache().remove(sh);
                        }
                        return true;
                    }
                });
    }

    @Override
    public void tearDown() {
      getCache().clear();
      invalidateCache();
    }
}
