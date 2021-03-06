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

import org.aesh.command.CommandDefinition;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.graphics.Graphics;
import org.aesh.graphics.GraphicsConfiguration;

import org.aesh.command.CommandException;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.terminal.Key;
import org.aesh.readline.terminal.formatting.Color;
import org.aesh.readline.terminal.formatting.TerminalColor;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshGraphicsExample {

    public static void main(String[] args) throws CommandLineParserException, IOException {
        SettingsBuilder builder = SettingsBuilder.builder().logging(true);
        builder.enableMan(true);

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(new GraphicsCommand())
                .create();

        Settings settings = builder
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt("[aesh@rules]$ "));

        console.start();
    }

    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "gfx", description = "")
    public static class GraphicsCommand implements Command {

        private CommandInvocation invocation;
        private Graphics g;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            invocation = commandInvocation;
            invocation.getShell().enableAlternateBuffer();
            doGfx();

            return CommandResult.SUCCESS;
        }

        public void waitForInput() {
            try {
                while(!invocation.input().equals(Key.q)) {

                }
            }
            catch (InterruptedException ignored) { }
            if(g != null)
                g.clearAndShowCursor();
            invocation.getShell().enableMainBuffer();
        }

        private void doGfx() {
            try {
                GraphicsConfiguration gc = null; //new AeshGraphicsConfiguration(invocation.getShell());
                g = gc.getGraphics();

                g.setColor(new TerminalColor(Color.BLUE, Color.DEFAULT));
                g.drawRect(20, 10, 20, 4);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.RED, Color.DEFAULT));
                g.drawRect(50, 5, 10, 8);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.DEFAULT, Color.YELLOW));
                g.fillRect(0, 15, 16, 10);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.CYAN, Color.DEFAULT));
                g.drawString("ÆSH", 100, 25);
                g.drawString("RULES", 100, 26);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.WHITE, Color.WHITE));
                g.drawRect(80, 23, 40, 5);
                Thread.sleep(500);
                g.flush();


                g.setColor(new TerminalColor(Color.BLUE, Color.DEFAULT));
                g.drawCircle(100, 10, 5);
                g.flush();
                Thread.sleep(500);

                g.setColor(new TerminalColor(Color.DEFAULT, Color.DEFAULT));
                g.drawLine(0, 0, 50, 20);
                g.flush();
                Thread.sleep(1500);

                g.setColor(new TerminalColor(Color.DEFAULT, Color.RED));
                int j =0;
                for(int i=0; i<100; i++) {
                    g.clear();
                    g.fillRect(i, 15+j, 20, 8);
                    g.flush();
                    Thread.sleep(50);
                    if(i > 10 && i < 20 || (i > 30 && i < 40))
                        j++;
                    if(i < 10 || (i > 20 && i < 30) || (i > 40 && i < 50))
                        j--;
                }

                waitForInput();
            }
            catch (InterruptedException ignored) { }
        }

    }

}
