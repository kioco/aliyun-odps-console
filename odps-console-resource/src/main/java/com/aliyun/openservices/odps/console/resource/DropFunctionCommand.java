/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.openservices.odps.console.resource;

import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.aliyun.odps.NoSuchObjectException;
import com.aliyun.odps.Odps;
import com.aliyun.odps.OdpsException;
import com.aliyun.odps.utils.StringUtils;
import com.aliyun.openservices.odps.console.ExecutionContext;
import com.aliyun.openservices.odps.console.ODPSConsoleException;
import com.aliyun.openservices.odps.console.commands.AbstractCommand;
import com.aliyun.openservices.odps.console.constants.ODPSConsoleConstants;
import com.aliyun.openservices.odps.console.utils.CommandParserUtils;



public class DropFunctionCommand extends AbstractCommand {

  public static final String[] HELP_TAGS = new String[]{"drop", "delete", "function"};

  public static void printUsage(PrintStream out) {
    out.println("Usage: drop function [if exists] <functionname>");
    out.println("       delete function [if exists] <functionname> [-p,-project <projectname>]");
  }

  private String projectName;
  private String functionName;
  boolean isCheckExistence; // if not exists, do nothing, just return ok.

  public DropFunctionCommand(String projectName, String resourceName, String commandText, ExecutionContext context) {
    this(false, projectName, resourceName, commandText, context);
  }

  public DropFunctionCommand(boolean isCheckExistence, String projectName, String resourceName,
      String commandText, ExecutionContext context) {
    super(commandText, context);
    this.projectName = projectName;
    this.functionName = resourceName;
    this.isCheckExistence = isCheckExistence;
  }

  static Options initOptions() {
    Options opts = new Options();
    Option project_name = new Option("p", "project", true, "project name");
    project_name.setRequired(false);
    opts.addOption(project_name);
    return opts;
  }
  
  @Override
  public void run() throws  ODPSConsoleException, OdpsException {

    Odps odps = getCurrentOdps();
    try {
      if (StringUtils.isNullOrEmpty(projectName)) {
        if (isCheckExistence && !odps.functions().exists(functionName)) {
          getWriter().writeError("OK");
          return;
        }
        odps.functions().delete(functionName);
      } else {
        if (isCheckExistence && !odps.functions().exists(projectName, functionName)) {
          getWriter().writeError("OK");
          return;
        }
        odps.functions().delete(projectName, functionName);
      }
      getWriter().writeError("OK");
    } catch (NoSuchObjectException e) {
      getWriter().writeError(e.getMessage());
    } catch (OdpsException e) {
      throw new ODPSConsoleException(e.getMessage());
    }
    
  }

  private static DropFunctionCommand parseExistsCommand(String[] args, String projectName, String commandString, ExecutionContext sessionContext) throws ODPSConsoleException {
    if (args.length == 5) {
      if (args[2].equalsIgnoreCase("IF") && args[3].equalsIgnoreCase("EXISTS")) {
        return new DropFunctionCommand(true, projectName, args[4], commandString, sessionContext);
      } else {
        throw new ODPSConsoleException(ODPSConsoleConstants.BAD_COMMAND);
      }
    } else {
      throw new ODPSConsoleException(ODPSConsoleConstants.BAD_COMMAND);
    }
  }

  public static DropFunctionCommand parse(String commandString, ExecutionContext sessionContext)
      throws ODPSConsoleException {

    String[] args = CommandParserUtils.getCommandTokens(commandString);
    if (args.length < 2) {
      return null;
    }

    // 检查是否符合DROP　FUNCTION FUNCTION_NAME命令
    if (args[0].equalsIgnoreCase("DROP") && args[1].equalsIgnoreCase("FUNCTION")) {
      if (args.length == 3) {
        return new DropFunctionCommand(null, args[2], commandString, sessionContext);
      } else {
        return parseExistsCommand(args, null, commandString, sessionContext);
      }
    } else if (args[0].equalsIgnoreCase("DELETE") && args[1].equalsIgnoreCase("FUNCTION")) {
      Options opts = initOptions();
      CommandLine cl=CommandParserUtils.getCommandLine(args, opts);

      String project = cl.getOptionValue("p");
      if (cl.getArgs().length == 3) {
        return new DropFunctionCommand(project, cl.getArgs()[2], commandString, sessionContext);
      } else {
        return parseExistsCommand(cl.getArgs(), project, commandString, sessionContext);
      }
    }
    return null;
  }

}
