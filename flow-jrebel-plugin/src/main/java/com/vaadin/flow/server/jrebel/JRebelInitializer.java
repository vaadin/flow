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
package com.vaadin.flow.server.jrebel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.javarebel.ClassEventListener;

/**
 * Initialize and maintain JRebel {@link ClassEventListener} which receives
 * class change events.
 */
public class JRebelInitializer implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        final String genericErrorMessage = "Unable to initialize JRebel plugin";
        // Check if we have JRebel on the classpath
        try {
            Class<?> classEventListenerImplClass = Class.forName(
                    "com.vaadin.flow.server.jrebel.JRebelClassEventListener");
            Constructor ctor = classEventListenerImplClass
                    .getConstructor(VaadinService.class);
            ctor.newInstance(event.getSource());
            getLogger().info("Started JRebel initializer");
        } catch (NoClassDefFoundError e) {
            // Failed because classloader could not load a JRebel class; just
            // log a warning in this case
            // (https://github.com/vaadin/flow/issues/7875)
            if (e.getMessage().contains("org/zeroturnaround/javarebel")) {
                getLogger().warn(
                        "Unable to initialize JRebel plugin; ensure that you are running the application under the JRebel agent");
            } else {
                getLogger().error(genericErrorMessage, e);
            }
        } catch (NoSuchMethodException | InstantiationException
                | IllegalAccessException | InvocationTargetException
                | ClassNotFoundException e) {
            getLogger().error(genericErrorMessage, e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JRebelInitializer.class.getName());
    }

}
