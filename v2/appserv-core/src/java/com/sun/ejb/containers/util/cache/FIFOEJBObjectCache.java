/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.ejb.containers.util.cache;

import com.sun.appserv.util.cache.Cache;
import com.sun.appserv.util.cache.CacheListener;
import com.sun.appserv.util.cache.Constants;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

import java.util.logging.*;
import com.sun.logging.*;

/**
 * A FIFO EJB(Local)Object cache that maintains reference count
 *
 * @author Mahesh Kannan
 */
public class FIFOEJBObjectCache
    extends LruCache
    implements EJBObjectCache
{
    protected int maxCacheSize;
    protected String name;
    protected EJBObjectCacheListener listener;
    
    protected Object refCountLock = new Object();
    protected int totalRefCount = 0;
    protected static boolean _printRefCount = false;
    
    
    private static final Logger _logger =
        LogDomains.getLogger(LogDomains.EJB_LOGGER);
    
    static {
        
        try {
            Properties props = System.getProperties();
            _printRefCount = (Boolean.valueOf(props.getProperty
                ("cache.printrefcount"))).booleanValue();
        } catch (Exception ex) {
            _logger.log(Level.FINE, "Cache PrintRefCount property ex", ex);
        }
    }
    
    /**
     * default constructor
     */
    public FIFOEJBObjectCache(String name) {
        this.name = name;
    }
    
    /**
     * constructor with specified timeout
     */
    public FIFOEJBObjectCache(String name, long timeout) {
        super(timeout);
        this.name = name;
    }
    
    public void init(int maxEntries, int numberOfVictimsToSelect, long timeout,
            float loadFactor, Properties props)
    {
        super.init(maxEntries, loadFactor, props);
        super.timeout = timeout;
        this.maxCacheSize = maxEntries;
        _logger.log(Level.FINE, name + ": FIFOEJBObject cache created....");
    }
    
    public void setEJBObjectCacheListener(EJBObjectCacheListener listener) {
        this.listener = listener;
    }
    
    public Object get(Object key) {
        int hashCode = hash(key);
        
        return internalGet(hashCode, key, false);
    }
    
    
    public Object get(Object key, boolean incrementRefCount) {
        int hashCode = hash(key);
        
        return internalGet(hashCode, key, incrementRefCount);
    }
    
    public Object put(Object key, Object value) {
        int hashCode = hash(key);
        
        return internalPut(hashCode, key, value, -1, false);
    }
    
    public Object put(Object key, Object value, boolean incrementRefCount) {
        int hashCode = hash(key);
        
        return internalPut(hashCode, key, value, -1, incrementRefCount);
    }
    
    
    public Object remove(Object key) {
        return internalRemove(key, true);
    }
    
    public Object remove(Object key, boolean decrementRefCount) {
        return internalRemove(key, decrementRefCount);
    }
    
    protected boolean isThresholdReached() {
        return listSize > maxCacheSize;
    }
    
    protected void itemAccessed(CacheItem item) { }
    
    protected void itemRemoved(CacheItem item) {
        LruCacheItem l = (LruCacheItem) item;
        
        // remove the item from the LRU list
        synchronized (this) {
            // if the item is already trimmed from the LRU list, nothing to do.
            if (l.isTrimmed) {
                return;
            }
            
            LruCacheItem prev = l.lPrev;
            LruCacheItem next = l.lNext;
            
            l.isTrimmed = true;
            
            // patch up the neighbors and make sure head/tail are correct
            if (prev != null)
                prev.lNext = next;
            else
                head = next;
            
            if (next != null)
                next.lPrev = prev;
            else
                tail = prev;
            
            l.lNext = l.lPrev = null;
            
            listSize--;
        }
    }
    
    protected Object internalGet(int hashCode, Object key, 
                                 boolean incrementRefCount) {
        
        int index = getIndex(hashCode);
        Object value = null;
        CacheItem item = null;
        
        synchronized (bucketLocks[index]) {
            item = buckets[index];
            
            for (; item != null; item = item.next) {
                if ( (hashCode == item.hashCode) && eq(key, item.key) ) {
                    break;
                }
            }
            
            // update the stats in line
            if (item != null) {
                value = item.getValue();
                if (incrementRefCount) {
                    EJBObjectCacheItem eoItem = (EJBObjectCacheItem) item;
                    eoItem.refCount++;
                    if (_printRefCount) {
                        incrementReferenceCount();
                    }
                    if (! eoItem.isTrimmed) {
                        itemRemoved(eoItem);
                    }
                }
            }
        }
        
        if (item != null)
            incrementHitCount();
        else
            incrementMissCount();
        
        return value;
    }
    
    protected Object internalPut(int hashCode, Object key, Object value, 
                                 int size, boolean incrementRefCount)
    {
        
        int index = getIndex(hashCode);
        
        CacheItem item, oldItem = null, overflow = null;
        EJBObjectCacheItem newItem = null;
        Object oldValue = null;
        int oldSize = 0;
        
        // lookup the item
        synchronized (bucketLocks[index]) {
            for (item = buckets[index]; item != null; item = item.next) {
                if ((hashCode == item.hashCode) && eq(key, item.key)) {
                    oldItem = item;
                    break;
                }
            }
            
            // if there was no item in the cache, insert the given item
            if (oldItem == null) {
                newItem = (EJBObjectCacheItem) 
                    createItem(hashCode, key, value, size);
                newItem.isTrimmed = incrementRefCount;
                
                // add the item at the head of the bucket list
                newItem.next = buckets[index];
                buckets[index] = newItem;
                
                if (incrementRefCount) {
                    newItem.refCount++;
                    if (_printRefCount) {
                        incrementReferenceCount();
                    }
                } else {
                    overflow = itemAdded(newItem);
                }
            } else {
                oldValue = oldItem.getValue();
                if (incrementRefCount) {
                    EJBObjectCacheItem oldEJBO = (EJBObjectCacheItem) oldItem;
                    oldEJBO.refCount++;
                    if (_printRefCount) {
                        incrementReferenceCount();
                    }
                }
            }
        }
        
        if (newItem != null) {
            incrementEntryCount();
            // make sure we are are not crossing the threshold
            if ((overflow != null) && (listener != null)) {
                listener.handleOverflow(overflow.key);
            }
        }
        
        return oldValue;
    }
    
    
    public void print() {
        System.out.println("EJBObjectCache:: size: " + getEntryCount() + 
                           "; listSize: " + listSize);
        for (LruCacheItem run = head; run!=null; run=run.lNext) {
            System.out.print("("+run.key+", "+run.value+") ");
        }
        System.out.println();
    }
    
    protected Object internalRemove(Object key, boolean decrementRefCount) {
        
        int hashCode = hash(key);
        int index = getIndex(hashCode);
        
        CacheItem prev = null, item = null;
        
        synchronized (bucketLocks[index]) {
            for (item = buckets[index]; item != null; item = item.next) {
                if (hashCode == item.hashCode && key.equals(item.key)) {
                    
                    EJBObjectCacheItem eoItem = (EJBObjectCacheItem) item;
                    if (decrementRefCount) {
                        if (eoItem.refCount > 0) {
                            eoItem.refCount--;
                            if (_printRefCount) {
                                decrementReferenceCount();
                            }
                        }
                    }
                    
                    if (eoItem.refCount > 0) {
                        return null;
                    }
                    
                    if (prev == null) {
                        buckets[index] = item.next;
                    } else  {
                        prev.next = item.next;
                    }
                    item.next = null;
                    
                    itemRemoved(item);
                    
                    break;
                    
                }
                prev = item;
            }
        }
        
        if (item != null) {
            decrementEntryCount();
            incrementRemovalCount();
            incrementHitCount();
            return item.value;
        } else {
            incrementMissCount();
            return null;
        }
        
    }
    
    /*
      protected void trimItem(CacheItem item) {
    }
     */
    
    protected CacheItem createItem(int hashCode, Object key, Object value, 
                                   int size) {
        return new EJBObjectCacheItem(hashCode, key, value, size);
    }
    
    protected static class EJBObjectCacheItem
    extends LruCacheItem {
        protected int refCount;
        
        protected EJBObjectCacheItem(int hashCode, Object key, Object value, 
                                     int size) {
            super(hashCode, key, value, size);
        }
    }
    
    public Map getStats() {
        Map map = new HashMap();
        StringBuffer sbuf = new StringBuffer();
        
        sbuf.append("(totalRef=").append(totalRefCount).append("; ");
        
        sbuf.append("listSize=").append(listSize)
        .append("; curSize/totSize=").append(getEntryCount())
        .append("/").append(maxEntries)
        .append("; trim=").append(trimCount)
        .append("; remove=").append(removalCount)
        .append("; hit/miss=").append(hitCount).append("/").append(missCount)
        .append(")");
        map.put("["+name+"]", sbuf.toString());
        return map;
    }
    
    public void trimExpiredEntries(int maxCount) {
        
        int count = 0;
        LruCacheItem item, lastItem = null;
        long currentTime = System.currentTimeMillis();
        
        synchronized (this) {
            // traverse LRU list till we reach a valid item; remove them at once
            for (item = tail; item != null && count < maxCount;
                 item = item.lPrev) {
                
                if ((timeout != NO_TIMEOUT) &&
                    (item.lastAccessed + timeout) <= currentTime) {
                    item.isTrimmed = true;
                    lastItem = item;
                    
                    count++;
                } else {
                    break;
                }
            }
            
            // if there was at least one invalid item then item != tail.
            if (item != tail) {
                lastItem.lPrev = null;
                
                if (item != null)
                    item.lNext = null;
                else
                    head = null;
                
                lastItem = tail; // record the old tail
                tail = item;
            }
            listSize -= count;
            trimCount += count;
        }
        
        if (count > 0) {
            
            ArrayList localVictims = new ArrayList(count);
            // trim the items from the BaseCache from the old tail backwards
            for (item = lastItem; item != null; item = item.lPrev) {
                localVictims.add(item.key);
            }
            
            if (listener != null) {
                listener.handleBatchOverflow(localVictims);
            }
        }
    }
    
    protected void incrementReferenceCount() {
        synchronized (refCountLock) {
            totalRefCount++;
        }
    }
    
    protected void decrementReferenceCount() {
        synchronized (refCountLock) {
            totalRefCount--;
        }
    }
    
    protected void decrementReferenceCount(int count) {
        synchronized (refCountLock) {
            totalRefCount -= count;
        }
    }
    
    
    static void unitTest_1()
    throws Exception {
        
        FIFOEJBObjectCache cache = new FIFOEJBObjectCache("UnitTestCache");
        cache.init(512, 0, 0, (float)1.0, null);
        
        int maxCount = 14;
        ArrayList keys = new ArrayList();
        for (int i=0; i<maxCount; i++) {
            keys.add("K_"+i);
        }
        
        for (int i=0; i<maxCount; i++) {
            String key = (String) keys.get(i);
            System.out.println("****  put(" + key + ", " + key + ", i" + 
                               ((i%2) == 0) + ")");
            cache.put(key, key, ((i%2)==0));
        }
        
        System.out.println("***  Only odd numbered keys must be printed  ***");
        cache.print();
        System.out.println("************************************************");
        
        for (int i=0; i<maxCount; i++) {
            String key = (String) keys.get(i);
            cache.get(key, ((i%2)==1));
        }
        
        System.out.println("****  NONE SHOULD BE PRINTED ****");
        cache.print();
        System.out.println("************************************************");
        
        cache.put("K__15", "K__15", true);
        cache.put("K__16", "K__15", true);
        cache.get("K__16", true);   //K__16 has refCount == 2
        cache.put("K__17", "K__17");//K__17 has refCount == 0
        
        System.out.println("****  Only K__17 must be printed ****");
        cache.print();
        System.out.println("************************************************");
        
        for (int i=0; i<maxCount; i++) {
            String key = (String) keys.get(i);
            if (cache.remove(key) == null) {
                throw new RuntimeException("Remove must have returned null!!");
            }
        }
        
        Object k15 = cache.remove("K__15");
        Object k16_1 = cache.remove("K__16");
        Object k16_2 = cache.remove("K__16");
        Object k17 = cache.remove("K__17");
        
        if (k15 == null) {
            System.out.println("** FAILED for K_15");
        }
        
        if (k16_1 != null) {
            System.out.println("** FAILED for K_16_1");
        }
        
        if (k16_2 == null) {
            System.out.println("** FAILED for K_16_2");
        }
        
        if (k17 == null) {
            System.out.println("** FAILED for K_17");
        }
        
        // Now the list id completely empty, add some more items
        for (int i=0; i<maxCount; i+=2) {
            String key = (String) keys.get(i);
            cache.put(key, key, (i%4)==0);
        }
        cache.print();
        
        
        //Make the FIFO list empty
        for (int i=0; i<maxCount; i+=2) {
            String key = (String) keys.get(i);
            cache.get(key, true);
        }
        cache.print();
        
        // Now the FIFO list id completely empty, add some more items
        for (int i=1; i<maxCount; i+=2) {
            String key = (String) keys.get(i);
            cache.put(key, key, (i%9)==0);
        }
        cache.print();
    }
    
    public static void main(String[] args)
        throws Exception
    {
        unitTest_1();
    }
    
}
