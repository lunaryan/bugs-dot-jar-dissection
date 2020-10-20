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
package org.apache.accumulo.core.client.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.SortedMapIterator;
import org.apache.accumulo.core.security.Authorizations;

public class MockBatchScanner extends MockScannerBase implements BatchScanner {
  
  List<Range> ranges = null;
  
  public MockBatchScanner(MockTable mockTable, Authorizations authorizations) {
    super(mockTable, authorizations);
  }
  
  @Override
  public void setRanges(Collection<Range> ranges) {
    if (ranges == null || ranges.size() == 0) {
      throw new IllegalArgumentException("ranges must be non null and contain at least 1 range");
    }
    
    this.ranges = new ArrayList<Range>(ranges);
  }
  
  static class RangesFilter extends Filter {
    List<Range> ranges;
    
    RangesFilter(SortedKeyValueIterator<Key,Value> iterator, List<Range> ranges) {
      setSource(iterator);
      this.ranges = ranges;
    }
    
    @Override
    public boolean accept(Key k, Value v) {
      for (Range r : ranges) {
        if (r.contains(k))
          return true;
      }
      return false;
    }
  }
  
  @Override
  public Iterator<Entry<Key,Value>> iterator() {
    if (ranges == null) {
      throw new IllegalStateException("ranges not set");
    }

    SortedKeyValueIterator<Key,Value> i = new SortedMapIterator(table.table);
    try {
      i = new RangesFilter(createFilter(i), ranges);
      i.seek(new Range(), createColumnBSS(fetchedColumns), !fetchedColumns.isEmpty());
      return new IteratorAdapter(i);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void close() {}
  
}
