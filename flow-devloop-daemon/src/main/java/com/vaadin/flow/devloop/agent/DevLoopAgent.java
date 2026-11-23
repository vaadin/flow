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
     * The system properties key the {@link Instrumentation} handle used to be
     * published under.
     * <p>
     * It is no longer written. The system properties table is a
     * {@code Hashtable<Object, Object>} and will hold an arbitrary object,
     * which is what made it a convenient way to hand the handle across class
     * loaders - but a table that is declared to map strings to strings and does
     * not is a trap for everything else in the JVM that reads it. Measured
     * against WildFly 38: Narayana merges the system properties when it loads
     * {@code jbossts-properties.xml}, took the handle for a string, and the
     * transactions subsystem failed its boot operations with a
     * {@link NullPointerException} - fatally, after the HTTP listener had
     * already bound. The handle now travels by {@link #get()} instead.
     * <p>
     * Kept only so that a reader who finds the name in an older log or an older
     * dev server can see what became of it.
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
     * <p>
     * This is the one channel. A {@code -javaagent} jar is appended to the
     * system class path, so this class is the system class loader's and its
     * static is the same object for every loader beneath it - including a
     * servlet container's, which is the case the system-properties table was
     * once used for. A caller that cannot name the class directly, because the
     * agent jar is not on its own class path, reaches it through
     * {@link ClassLoader#getSystemClassLoader()}; see the dev server's
     * {@code DevLoopRedefiner}.
     *
     * @return the instrumentation handle, or {@code null}
     */
    public static Instrumentation get() {
        return instrumentation;
    }
}
