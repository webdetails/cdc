/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdc.mondrian;

import java.util.List;

import mondrian.spi.SegmentBody;
import mondrian.spi.SegmentHeader;
import mondrian.spi.SegmentCache;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;

import java.util.ArrayList;

import pt.webdetails.cdc.HazelcastManager;


/**
 * SegmentCache implementation for mondrian-3.4+ based on a hazelcast distributed map.
 */
public class SegmentCacheHazelcast implements SegmentCache {

  private static final String MAP = "mondrian";

  private static final IMap<SegmentHeader, SegmentBody> getCache() {
    return HazelcastManager.INSTANCE.getHazelcast().getMap(MAP);
  }

  private boolean syncListeners = true;

  public SegmentCacheHazelcast() {
    this(false);
  }

  public SegmentCacheHazelcast(boolean syncListeners) {
    this.syncListeners = syncListeners;
  }

  @Override
  public SegmentBody get(SegmentHeader key) {
    key.getDescription();//this call affects serialization
    return getCache().get(key);
  }

  @Override
  public boolean put(SegmentHeader key, SegmentBody value) {
    key.getDescription();//affects serialization
    getCache().put(key, value);    
    return true;
  }

  public boolean contains(SegmentHeader key) {
    key.getDescription();//affects serialization
    return getCache().containsKey(key);
  }
  
  @Override
  public boolean remove(SegmentHeader key) {
    key.getDescription();
    return getCache().remove(key) != null;
  }
  
  @Override
  public List<SegmentHeader> getSegmentHeaders() {
    return new ArrayList<SegmentHeader>(getCache().keySet());
  }

  @Override
  public void tearDown() {
//    launcher.stop();
  }
  
  @Override
  public void addListener(SegmentCacheListener listener) {
    if(syncListeners) {
      listenersToSync.add(listener);
      //syncWithListener(listener);
    }
    getCache().addEntryListener(new SegmentCacheListenerWrapper(listener), false);
  }
  
  @Override
  public void removeListener(SegmentCacheListener listener) {
    getCache().removeEntryListener(new SegmentCacheListenerWrapper(listener));
  }
  

  @Override
  public boolean supportsRichIndex() {
    //Stores full key, not just ID
    return true;
  }

  private List<SegmentCacheListener> listenersToSync = new ArrayList<SegmentCacheListener>();
  public synchronized List<SegmentCacheListener> getListenersToSync() {
    return listenersToSync;
  }
  public synchronized void resetListeners() {
    listenersToSync = new ArrayList<SegmentCacheListener>();;
  }

  /**
   * Notifies the listener of what already is in cache.<br/>
   * It does so by sending an external add event for each header in cache.<br/>
   * All relevant RolapStars must have been initialized inside mondrian for
   * this to work.
   * @param listener a mondrian listener
   * @return number of entries sent
   */
  public int syncWithListener(SegmentCacheListener listener) {
    int count = 0;
    for(final SegmentHeader header : getSegmentHeaders()) {
      listener.handle(
          new SegmentCacheListener.SegmentCacheEvent(){

            public EventType getEventType() {
              return EventType.ENTRY_CREATED;
            }

            public SegmentHeader getSource() {
              return header;
            }

            public boolean isLocal() {
              return false;
            }
            
          });
      count++;
    }
    return count;
  }

  /**
   * Wraps a mondrian SegmentCacheListener in a hazelcast EntryListener
   */
  static class SegmentCacheListenerWrapper implements EntryListener<SegmentHeader, SegmentBody> {

    private SegmentCacheListener listener;
    
    public SegmentCacheListener getInnerListener() {
      return listener;
    }
    
    public SegmentCacheListenerWrapper(SegmentCacheListener segmentCacheListener){
      this.listener = segmentCacheListener;
    }
    
    @Override
    public void entryAdded(final EntryEvent<SegmentHeader, SegmentBody> event) {
      listener.handle(getSegmentCacheEvent(event));
    }

    @Override
    public void entryRemoved(final EntryEvent<SegmentHeader, SegmentBody> event) {
      listener.handle(getSegmentCacheEvent(event));
    }

    @Override
    public void entryUpdated(final EntryEvent<SegmentHeader, SegmentBody> event) {
      listener.handle(getSegmentCacheEvent(event));
    }

    @Override
    public void entryEvicted(final EntryEvent<SegmentHeader, SegmentBody> event) {
      listener.handle(getSegmentCacheEvent(event));
    }
    
    private SegmentCacheListener.SegmentCacheEvent getSegmentCacheEvent(final EntryEvent<SegmentHeader, SegmentBody> event)
    { 
      return new SegmentCacheListener.SegmentCacheEvent(){
       
        public boolean isLocal() {//for this to work we have to make sure cache cleaning made directly through cdc doesn't bypass mondrian api 
          return event.getMember().localMember();
        }
        
        public SegmentHeader getSource() {
            return event.getKey();
        }
        
        public EventType getEventType() {
          switch( event.getEventType()){
            case ADDED:
            case UPDATED:
              return SegmentCacheListener.SegmentCacheEvent.EventType.ENTRY_CREATED;  
            case REMOVED:
            case EVICTED:
              return SegmentCacheListener.SegmentCacheEvent.EventType.ENTRY_DELETED;
          }
          //we shouldn't even get here
          return null;
        }
      };
    }
    
    @Override
    public boolean equals(Object other){
      if(other == null) return false;
      if( other instanceof SegmentCacheListenerWrapper){
        return this.listener.equals(((SegmentCacheListenerWrapper)other).listener);
      }
      else return false;
    }
    
  }
  
}
