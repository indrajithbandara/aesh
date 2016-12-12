/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.readline;

import org.aesh.readline.Readline;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.parser.Parser;
import org.aesh.tty.Connection;
import org.aesh.tty.Signal;
import org.aesh.util.LoggerUtil;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Process extends Thread implements Consumer<Signal> {

    private final Connection conn;
    private final Readline readline;
    private final CommandContainer container;
    private final String line;
    private final Console console;
    private final Settings settings;
    private volatile boolean running;

    private static final String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;
    private static final Logger LOGGER = LoggerUtil.getLogger(Process.class.getName());

    public Process(Connection conn, Console console, Readline readline,
                   CommandContainer container, Settings settings, String line) {
        this.conn = conn;
        this.console = console;
        this.readline = readline;
        this.container = container;
        this.settings = settings;
        this.line = line;
    }

    @Override
    public void accept(Signal signal) {
        switch (signal) {
            case INT:
                if (running) {
                    // Ctrl-C interrupt : we use Thread interrupts to signal the command to stop
                    LOGGER.info("got interrupted in Task");
                    interrupt();
                }
        }
    }

    @Override
    public void run() {
        // Subscribe to events, in particular Ctrl-C
        conn.setSignalHandler(this);
        running = true;
        try {
            runCommand(container, line);
            //command.execute(conn, args);
            //command.execute(null);
        }
        catch (InterruptedException e) {
            // Ctlr-C interrupt
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            running = false;
            conn.setSignalHandler(null);
            conn.setStdinHandler(null);

            if (console.running()) {
                LOGGER.info("trying to read again.");
                // Readline again
                console.read(conn, readline);
            }
            else {
                conn.close();
                LOGGER.info("we're exiting...");
            }
        }
    }

    private CommandContainerResult runCommand(CommandContainer container, String aeshLine) throws InterruptedException, OptionValidatorException, CommandException, CommandLineParserException, CommandValidatorException {
        return container.executeCommand(Parser.findAllWords(aeshLine), settings.invocationProviders(), settings.aeshContext(),
                settings.commandInvocationServices().getCommandInvocationProvider(
                        commandInvocationProvider).enhanceCommandInvocation(
                        new AeshCommandInvocation(console, new ShellImpl(conn, readline))));
    }
}