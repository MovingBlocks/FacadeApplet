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
import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.modes.StateMainMenu;
import org.terasology.engine.paths.PathManager;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.engine.subsystem.lwjgl.LwjglAudio;
import org.terasology.engine.subsystem.lwjgl.LwjglGraphics;
import org.terasology.engine.subsystem.lwjgl.LwjglInput;
import org.terasology.engine.subsystem.lwjgl.LwjglTimer;

import com.google.common.collect.Lists;

/**
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 * @author Rasmus 'Cervator' Praestholm <cervator@gmail.com>
 */
@SuppressWarnings("serial")
public final class TerasologyApplet extends Applet {
    private static Logger logger;
    private TerasologyEngine engine;
    private Thread gameThread;

    @Override
    public void init() {
        super.init();
        try {
            PathManager.getInstance().useDefaultHomePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start applet - could not obtain home path.", e);
        }
        logger = LoggerFactory.getLogger(TerasologyApplet.class);

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
            gameThread.join();
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.error("Failed to cleanly shut down engine", e);
            }
        }

        super.destroy();
    }
}
