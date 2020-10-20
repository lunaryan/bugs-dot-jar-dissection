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
package org.apache.accumulo.core.util.shell.commands;

import org.apache.accumulo.core.util.shell.Shell;
import org.apache.accumulo.core.util.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class NoTableCommand extends Command {
  private Option tableOpt;
  
  @Override
  public int execute(String fullCommand, CommandLine cl, Shell shellState) throws Exception {
    shellState.setTableName("");
    
    return 0;
  }
  
  @Override
  public String description() {
    return "returns to a tableless shell state";
  }
  
  public String usage() {
    return getName();
  }
  
  @Override
  public int numArgs() {
    return 0;
  }
  
  public Options getOptions() {
    Options o = new Options();
    tableOpt = new Option(Shell.tableOption, "tableName", true, "Returns to a no table state");
    o.addOption(tableOpt);
    return o;
  }
  
}