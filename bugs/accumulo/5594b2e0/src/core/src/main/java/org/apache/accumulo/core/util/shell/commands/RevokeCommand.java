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

import java.util.Map;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.util.BadArgumentException;
import org.apache.accumulo.core.util.shell.Shell;
import org.apache.accumulo.core.util.shell.Token;
import org.apache.accumulo.core.util.shell.Shell.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

public class RevokeCommand extends Command {
  private Option systemOpt, tableOpt, userOpt;
  
  @Override
  public int execute(String fullCommand, CommandLine cl, Shell shellState) throws AccumuloException, AccumuloSecurityException {
    String user = cl.hasOption(userOpt.getOpt()) ? cl.getOptionValue(userOpt.getOpt()) : shellState.getConnector().whoami();
    
    String permission[] = cl.getArgs()[0].split("\\.", 2);
    if (cl.hasOption(systemOpt.getOpt()) && permission[0].equalsIgnoreCase("System")) {
      try {
        shellState.getConnector().securityOperations().revokeSystemPermission(user, SystemPermission.valueOf(permission[1]));
        Shell.log.debug("Revoked from " + user + " the " + permission[1] + " permission");
      } catch (IllegalArgumentException e) {
        throw new BadArgumentException("No such system permission", fullCommand, fullCommand.indexOf(cl.getArgs()[0]));
      }
    } else if (cl.hasOption(tableOpt.getOpt()) && permission[0].equalsIgnoreCase("Table")) {
      String tableName = cl.getOptionValue(tableOpt.getOpt());
      try {
        shellState.getConnector().securityOperations().revokeTablePermission(user, tableName, TablePermission.valueOf(permission[1]));
        Shell.log.debug("Revoked from " + user + " the " + permission[1] + " permission on table " + tableName);
      } catch (IllegalArgumentException e) {
        throw new BadArgumentException("No such table permission", fullCommand, fullCommand.indexOf(cl.getArgs()[0]));
      }
    } else {
      throw new BadArgumentException("Unrecognized permission", fullCommand, fullCommand.indexOf(cl.getArgs()[0]));
    }
    return 0;
  }
  
  @Override
  public String description() {
    return "revokes system or table permissions from a user";
  }
  
  @Override
  public String usage() {
    return getName() + " <permission>";
  }
  
  @Override
  public void registerCompletion(Token root, Map<Command.CompletionSet,Set<String>> completionSet) {
    Token cmd = new Token(getName());
    cmd.addSubcommand(new Token(TablePermission.printableValues()));
    cmd.addSubcommand(new Token(SystemPermission.printableValues()));
    root.addSubcommand(cmd);
  }
  
  @Override
  public Options getOptions() {
    Options o = new Options();
    OptionGroup group = new OptionGroup();
    
    tableOpt = new Option(Shell.tableOption, "table", true, "revoke a table permission on this table");
    systemOpt = new Option("s", "system", false, "revoke a system permission");
    
    tableOpt.setArgName("table");
    
    group.addOption(systemOpt);
    group.addOption(tableOpt);
    group.setRequired(true);
    
    o.addOptionGroup(group);
    
    userOpt = new Option(Shell.userOption, "user", true, "user to operate on");
    userOpt.setArgName("username");
    userOpt.setRequired(true);
    o.addOption(userOpt);
    
    return o;
  }
  
  @Override
  public int numArgs() {
    return 1;
  }
}