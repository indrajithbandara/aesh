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
package org.aesh.readline;

import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.io.PipelineResource;
import org.aesh.terminal.Connection;
import org.aesh.utils.Config;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessManager {

    private Connection conn;
    private Console console;
    private Queue<Execution<? extends CommandInvocation>> executionQueue;

    public ProcessManager(Console console) {
        this.console = console;
        executionQueue = new ConcurrentLinkedQueue<>();
    }

    public void execute(Executor<? extends CommandInvocation> executor, Connection conn) {
        this.conn = conn;
        executionQueue.addAll(executor.getExecutions());
        executeNext();
    }

    private Execution<? extends CommandInvocation> next() {
        return executionQueue.poll();
    }

    private Execution<? extends CommandInvocation> peek() {
        return executionQueue.peek();
    }

    public boolean hasNext() {
        return !executionQueue.isEmpty();
    }

    public void processFinished(Process process) {

        boolean haveOperatorInput = false;
        //first check any operators
        if(process.execution().getCommandInvocation().getConfiguration() != null) {
            //we have redirection out
           if(process.execution().getCommandInvocation().getConfiguration().getOutputRedirection() != null) {
               try {
                   if(process.execution().getCommandInvocation().getConfiguration().getPipedData() != null) {
                       haveOperatorInput = true;
                   }
                   process.execution().getCommandInvocation().getConfiguration().getOutputRedirection().close();
               } catch (IOException e) {
                   conn.write("Aesh: " + e.getLocalizedMessage() + ": No such file or directory" + Config.getLineSeparator());
               }
           }
        }

        if(hasNext()) {
            //if we have any operator input, we should see if we could inject it
            if(haveOperatorInput) {
                peek().updateInjectedArgumentWithPipelinedData(new PipelineResource(
                        process.execution().getCommandInvocation().getConfiguration().getPipedData()));
            }
            executeNext();
        }
        else if(console.running())
            console.read();
        else
            conn.close();
    }

    public void executeNext() {
        if(hasNext())
            new Process(this, conn, next()).start();
    }
}
