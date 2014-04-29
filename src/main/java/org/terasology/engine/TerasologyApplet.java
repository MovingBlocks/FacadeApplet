/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.engine;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.modes.StateMainMenu;
import org.terasology.engine.paths.PathManager;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.engine.subsystem.lwjgl.LwjglAudio;
import org.terasology.engine.subsystem.lwjgl.LwjglGraphics;
import org.terasology.engine.subsystem.lwjgl.LwjglInput;
import org.terasology.engine.subsystem.lwjgl.LwjglTimer;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.google.common.collect.Lists;

/**
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 * @author Rasmus 'Cervator' Praestholm <cervator@gmail.com>
 */
public final class TerasologyApplet extends Applet {

    private static final long serialVersionUID = -5223097421609941428L;
    
    private static Logger logger;
    private TerasologyEngine engine;
    private Thread gameThread;

    @Override
    public void init() {
        super.init();
        
        setLayout(new BorderLayout());

        // fill the applet with a scrollable text area
        final JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        validate();
        
        attachRootLogAppender(textArea);
        
        try {
            PathManager.getInstance().useDefaultHomePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start applet - could not obtain home path.", e);
        }
        logger = LoggerFactory.getLogger(TerasologyApplet.class);
        obtainMods();
        startGame();
    }

    private void attachRootLogAppender(final JTextArea textArea) {
        Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) rootLogger;
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        AppenderBase<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {

            @Override
            protected void append(ILoggingEvent eventObject) {
                textArea.append(eventObject.getFormattedMessage() + System.lineSeparator());
            }
        };
        appender.setContext(context);
        appender.start();
        log.addAppender(appender);
        log.info("Applet log appender attached");
    }

    private void obtainMods() {
        String[] mods = getParameter("modules").split(",");

        for (String modRaw : mods) {
            try {
                String mod = modRaw.trim();
                URL url = new URL(getCodeBase(), "mods/" + mod);
                
                logger.info("Downloading " + url);
                
                try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                     SeekableByteChannel writeChannel = Files.newByteChannel(PathManager.getInstance().getHomeModPath().resolve(mod),
                             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(1 << 16);
                    while (rbc.read(buffer) != -1) {
                        buffer.flip();
                        writeChannel.write(buffer);
                        buffer.compact();
                    }
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        writeChannel.write(buffer);
                    }
                } catch (IOException e) {
                    logger.error("Unable to obtain module '{}'", mod, e);
                }
            } catch (MalformedURLException e) {
                logger.error("Unable to obtain module '{}'", modRaw, e);
            }


        }
    }

    private void startGame() {
        gameThread = new Thread() {
            @Override
            public void run() {
                try {
                    Collection<EngineSubsystem> subsystemList;
                    subsystemList = Lists.<EngineSubsystem>newArrayList(new LwjglGraphics(), new LwjglTimer(), new LwjglAudio(), new LwjglInput());
                    TerasologyEngine engine = new TerasologyEngine(subsystemList);
                    engine.init();
                    engine.run(new StateMainMenu());
                    engine.dispose();
                } catch (Exception e) {
                    logger.error(e.toString(), e);
                }
            }
        };

        gameThread.start();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void destroy() {
        if (engine != null) {
            engine.shutdown();
        }
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.error("Failed to cleanly shut down engine", e);
            }
        }

        super.destroy();
    }
}
