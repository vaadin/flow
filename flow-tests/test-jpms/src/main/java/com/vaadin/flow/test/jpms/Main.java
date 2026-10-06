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
package com.vaadin.flow.test.jpms;

import java.nio.file.Files;

import org.eclipse.jetty.ee11.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee11.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * Starts the application in embedded Jetty.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.getInteger("port", 8080));
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        // Everything is served from the module path, the web application
        // root only has to exist
        context.setBaseResource(ResourceFactory.of(context)
                .newResource(Files.createTempDirectory("test-jpms").toUri()));
        // Scan all jars and class directories, including the ones on the
        // module path, for ServletContainerInitializers and their
        // @HandlesTypes classes
        context.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN,
                ".*\\.jar|.*/classes/.*");
        context.setConfigurationDiscovered(true);
        context.addConfiguration(new AnnotationConfiguration());
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        server.start();
        server.join();
    }
}
