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
package org.apache.accumulo.server.monitor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.util.Daemon;
import org.apache.accumulo.server.conf.ServerConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SocketNode;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Hijack log4j and capture log events for display.
 * 
 */
public class LogService extends org.apache.log4j.AppenderSkeleton {
  
  private static final Logger log = Logger.getLogger(LogService.class);
  
  /**
   * Read logging events forward to us over the net.
   * 
   */
  static class SocketServer implements Runnable {
    private ServerSocket server = null;
    
    public SocketServer(int port) {
      try {
        server = new ServerSocket(port);
      } catch (IOException io) {
        throw new RuntimeException(io);
      }
    }
    
    public void run() {
      try {
        while (true) {
          log.debug("Waiting for log message senders");
          Socket socket = server.accept();
          log.debug("Got a new connection");
          Thread t = new Daemon(new SocketNode(socket, LogManager.getLoggerRepository()));
          t.start();
        }
      } catch (IOException io) {
        log.error(io, io);
      }
    }
  }
  
  static void startLogListener() {
    try {
      new Daemon(new SocketServer(ServerConfiguration.getSystemConfiguration().getPort(Property.MONITOR_LOG4J_PORT))).start();
    } catch (Throwable t) {
      log.info("Unable to listen to cluster-wide ports", t);
    }
  }
  
  static private LogService instance = null;
  
  synchronized public static LogService getInstance() {
    if (instance == null)
      return new LogService();
    return instance;
  }
  
  private static final int MAX_LOGS = 50;
  
  private LinkedHashMap<String,DedupedLogEvent> events = new LinkedHashMap<String,DedupedLogEvent>(MAX_LOGS + 1, (float) .75, true) {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    @SuppressWarnings("rawtypes")
    protected boolean removeEldestEntry(Map.Entry eldest) {
      return size() > MAX_LOGS;
    }
  };
  
  public LogService() {
    synchronized (LogService.class) {
      instance = this;
    }
  }
  
  @Override
  synchronized protected void append(LoggingEvent ev) {
    Object application = ev.getMDC("application");
    if (application == null || application.toString().isEmpty())
      return;
    
    DedupedLogEvent dev = new DedupedLogEvent(ev);
    
    // if event is present, increase the count
    if (events.containsKey(dev.toString())) {
      DedupedLogEvent oldDev = events.remove(dev.toString());
      dev.setCount(oldDev.getCount() + 1);
    }
    events.put(dev.toString(), dev);
  }
  
  @Override
  public void close() {
    events = null;
  }
  
  @Override
  public synchronized void doAppend(LoggingEvent event) {
    super.doAppend(event);
  }
  
  @Override
  public boolean requiresLayout() {
    return false;
  }
  
  synchronized public List<DedupedLogEvent> getEvents() {
    return new ArrayList<DedupedLogEvent>(events.values());
  }
  
  synchronized public void clear() {
    events.clear();
  }
}