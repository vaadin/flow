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
package com.vaadin.flow.devloop.agent;

import java.lang.instrument.Instrumentation;

/**
 * Captures the {@link Instrumentation} handle at JVM startup and publishes it,
 * so the in-app dev-loop connector can perform atomic class redefinitions
 * in-process.
 * <p>
 * The daemon jar carries {@code Premain-Class}/{@code Agent-Class} pointing
 * here alongside its own {@code Main-Class}, so one artifact is both the
 * runnable daemon and the javaagent the application JVM loads. Only this class
 * is ever loaded into the application JVM.
 * <p>
 * The handle is published two ways, so the connector finds it regardless of
 * which class loader ends up owning the application classes: as a static field
 * here (which works when the agent jar is visible to the application loader,
 * the normal case since agent jars are appended to the system class path), and
 * as a value in the system properties table, which is a
 * {@code Hashtable<Object, Object>} and can therefore carry an arbitrary object
 * - that path needs no class visibility at all.
 * <p>
 * Depends on nothing but {@code java.instrument}: it is loaded before the
 * application, into a JVM whose class path it must not influence.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class DevLoopAgent {

    /**
     * The system properties key the {@link Instrumentation} handle is published
     * under.
     */
    public static final String PROPERTY = "devloop.instrumentation";

    private static volatile Instrumentation instrumentation;

    private DevLoopAgent() {
    }

    /**
     * Entry point when the agent is loaded at JVM startup with
     * {@code -javaagent:}.
     *
     * @param args
     *            the agent arguments, unused
     * @param inst
     *            the instrumentation handle to publish
     */
    public static void premain(String args, Instrumentation inst) {
        install(inst);
    }

    /**
     * Entry point when the agent is attached to a running JVM.
     *
     * @param args
     *            the agent arguments, unused
     * @param inst
     *            the instrumentation handle to publish
     */
    public static void agentmain(String args, Instrumentation inst) {
        install(inst);
    }

    private static void install(Instrumentation inst) {
        instrumentation = inst;
        System.getProperties().put(PROPERTY, inst);
        // System.out rather than a logger: this runs in premain, before the
        // application's logging framework exists, and the output belongs to the
        // app log the daemon reads anyway.
        System.out.println("[devloop-agent] Instrumentation captured; "
                + "redefineClasses supported="
                + inst.isRedefineClassesSupported());
    }

    /**
     * The captured instrumentation handle, or {@code null} when this class was
     * loaded without the agent having been installed.
     *
     * @return the instrumentation handle, or {@code null}
     */
    public static Instrumentation get() {
        return instrumentation;
    }
}
