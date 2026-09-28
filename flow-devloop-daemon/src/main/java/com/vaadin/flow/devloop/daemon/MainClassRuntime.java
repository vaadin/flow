/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An application with an entry point, launched as
 * {@code java -cp <classpath> <MainClass>}.
 * <p>
 * This is the dev loop's original and simplest shape - a Spring Boot
 * application, or anything else with a {@code public static void main} - and
 * the one every invariant elsewhere was written against: the JVM is a direct
 * child, its classpath is the one the daemon resolved, and its exit code is the
 * outcome.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class MainClassRuntime implements AppRuntime {

    /** The name this runtime answers to in {@code -Dvaadin.dev.runtime}. */
    static final String NAME = "main";

    private final Launch launch;
    private final Launch.Log log;

    /**
     * The discovered application class, so a restart does not rediscover it.
     */
    private volatile String mainClass;

    MainClassRuntime(Launch launch, Launch.Log log) {
        this.launch = launch;
        this.log = log;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Invocation invocation(Launch.Project project, List<String> jvmFlags,
            List<String> systemProperties) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(launch.appJvm().binary().toString());
        command.addAll(jvmFlags);
        command.addAll(systemProperties);
        command.add("-cp");
        command.add(project.appClasspath());
        command.add(mainClass(project.app()));
        return new Invocation(command, Map.of(), true);
    }

    @Override
    public boolean serving(String line) {
        return AppLog.serving(line);
    }

    /**
     * The class the app JVM is launched with, discovered once and remembered:
     * scanning an output directory is cheap, but a restart should not pay for
     * it twice.
     */
    private String mainClass(Reactor.Module app) throws IOException {
        String known = mainClass;
        if (known != null) {
            return known;
        }
        String found = MainClass.discover(app, log)
                .orElseThrow(() -> new IOException(
                        "no application class found under " + app.classesDir()
                                + ": build the module once, or name the class "
                                + "with -Dvaadin.dev.mainClass"));
        mainClass = found;
        return found;
    }
}
