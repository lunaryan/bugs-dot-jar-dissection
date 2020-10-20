/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.AuthInfo;
import org.apache.accumulo.core.util.ArgumentChecker;

public class TabletServerBatchReader extends ScannerOptions implements BatchScanner {
  
  private String table;
  private int numThreads;
  private ExecutorService queryThreadPool;
  
  private Instance instance;
  private ArrayList<Range> ranges;
  
  private AuthInfo credentials;
  private Authorizations authorizations = Constants.NO_AUTHS;
  
  private static int nextBatchReaderInstance = 1;
  
  private static synchronized int getNextBatchReaderInstance() {
    return nextBatchReaderInstance++;
  }
  
  private int batchReaderInstance = getNextBatchReaderInstance();
  
  private class BatchReaderThreadFactory implements ThreadFactory {
    
    private ThreadFactory dtf = Executors.defaultThreadFactory();
    private int threadNum = 1;
    
    public Thread newThread(Runnable r) {
      Thread thread = dtf.newThread(r);
      thread.setName("batch scanner " + batchReaderInstance + "-" + threadNum++);
      thread.setDaemon(true);
      return thread;
    }
    
  }
  
  public TabletServerBatchReader(Instance instance, AuthInfo credentials, String table, Authorizations authorizations, int numQueryThreads) {
    ArgumentChecker.notNull(instance, credentials, table, authorizations);
    this.instance = instance;
    this.credentials = credentials;
    this.authorizations = authorizations;
    this.table = table;
    this.numThreads = numQueryThreads;
    
    queryThreadPool = new ThreadPoolExecutor(numQueryThreads, numQueryThreads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
        new BatchReaderThreadFactory());
    
    ranges = null;
  }
  
  public void close() {
    queryThreadPool.shutdownNow();
  }
  
  @Override
  public void setRanges(Collection<Range> ranges) {
    if (ranges == null || ranges.size() == 0) {
      throw new IllegalArgumentException("ranges must be non null and contain at least 1 range");
    }
    
    if (queryThreadPool.isShutdown()) {
      throw new IllegalStateException("batch reader closed");
    }
    
    this.ranges = new ArrayList<Range>(ranges);
    
  }
  
  @Override
  public Iterator<Entry<Key,Value>> iterator() {
    if (ranges == null) {
      throw new IllegalStateException("ranges not set");
    }
    
    if (queryThreadPool.isShutdown()) {
      throw new IllegalStateException("batch reader closed");
    }
    
    return new TabletServerBatchReaderIterator(instance, credentials, table, authorizations, ranges, numThreads, queryThreadPool, this);
  }
}
