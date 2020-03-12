/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.plugin.hotswapagent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassFileEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;

/**
 * Flow plugin for HotswapAgent
 */
@Plugin(name = "Flow", description = "Vaadin Platform support", testedVersions = {
        "16.0-SNAPSHOT" }, expectedVersions = { "16.0-SNAPSHOT" })
public class FlowPlugin {
    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    private ReloadCommand reloadCommand;

    private ReflectionCommand clearReflectionCache = new ReflectionCommand(this,
            "com.vaadin.flow.internal.ReflectionCache", "clearAll");

    private Set<Class<?>> modifiedClasses = new HashSet<>();

    private Set<Class<?>> deletedClasses = new HashSet<>();

    private static AgentLogger LOGGER = AgentLogger.getLogger(FlowPlugin.class);

    public FlowPlugin() {
        LOGGER.info("Plugin {} enabled", getClass());
    }

    @OnClassLoadEvent(classNameRegexp = "com.vaadin.flow.server.VaadinServlet")
    public static void init(CtClass ctClass)
            throws NotFoundException, CannotCompileException {
        String src = PluginManagerInvoker
                .buildInitializePlugin(FlowPlugin.class);
        src += PluginManagerInvoker.buildCallPluginMethod(FlowPlugin.class,
                "registerServlet", "this", "java.lang.Object");
        ctClass.getDeclaredConstructor(new CtClass[0]).insertAfter(src);

        LOGGER.info("VaadinServlet class loaded and instrumented");
    }

    public void registerServlet(Object vaadinServlet) {
        try {
            Class<?> flowIntegrationClass = appClassLoader.loadClass(
                    "com.vaadin.flow.plugin.hotswapagent.FlowIntegration");
            Object flowIntegration = flowIntegrationClass.getConstructor()
                    .newInstance();
            scheduler.scheduleCommand(new ReflectionCommand(flowIntegration,
                    "servletInitialized", vaadinServlet));
            reloadCommand = new ReloadCommand(flowIntegration);
        } catch (ClassNotFoundException | NoSuchMethodException
                | InstantiationException | IllegalAccessException
                | InvocationTargetException ex) {
            LOGGER.error(null, ex);
            return;
        }
    }

    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void invalidateReflectionCache() throws Exception {
        LOGGER.debug("Clearing Vaadin reflection cache");
        scheduler.scheduleCommand(clearReflectionCache);
    }

    @OnClassFileEvent(classNameRegexp = ".*", events = { FileEvent.CREATE,
            FileEvent.MODIFY })
    public void classModified(CtClass ctClass) throws Exception {
        LOGGER.debug("Create/modify class file event for " + ctClass.getName());
        modifiedClasses.add(resolveClass(ctClass.getName()));

        // Note that it is fine to add multiple calls to refreshCommand, as old
        // instances will be removed when a newer one is queued
        scheduler.scheduleCommand(reloadCommand);
    }

    @OnClassFileEvent(classNameRegexp = ".*", events = { FileEvent.DELETE })
    public void classDeleted(CtClass ctClass) throws Exception {
        LOGGER.debug("Delete class file event for " + ctClass.getName());
        deletedClasses.add(resolveClass(ctClass.getName()));
        scheduler.scheduleCommand(reloadCommand);
    }

    private Class<?> resolveClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, appClassLoader);
    }

    private class ReloadCommand extends ReflectionCommand {
        private Object flowIntegration;

        ReloadCommand(Object flowIntegration) {
            super(flowIntegration, "reload", modifiedClasses, deletedClasses);
            this.flowIntegration = flowIntegration;
        }

        // NOTE: Identity equals semantics for refresh command replacement in
        // HotSwap (since modifiedClasses and deletedClasses are mutable)!

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            } else if (that instanceof ReloadCommand) {
                ReloadCommand thatCommand = (ReloadCommand) that;
                return this.flowIntegration == thatCommand.flowIntegration;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(flowIntegration);
        }

        @Override
        public void executeCommand() {
            super.executeCommand();
            modifiedClasses.clear();
            deletedClasses.clear();
        }
    }
}