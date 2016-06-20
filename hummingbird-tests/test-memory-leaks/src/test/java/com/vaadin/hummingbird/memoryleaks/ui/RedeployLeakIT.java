/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.hummingbird.memoryleaks.ui;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.hummingbird.testutil.FixedPhantomJSDriver;
import com.vaadin.testbench.By;

/**
 * Test that verifies no leaks happen during redeployment of a war. Uses Jetty
 * to deploy/undeploy the war file this module produces.
 *
 * If you run this from Eclipse it might not produce the correct result as it
 * uses files from the target folder.
 */
public class RedeployLeakIT {

    private static final String testClass = "com.vaadin.server.VaadinServlet";
    private Server server;
    private WebAppContext context;
    private WeakReference<Class<?>> testReference;

    @Test
    public void deployUndeployCheck() throws Exception {
        // DO NOT RUN FROM ECLIPSE
        // The test uses files from the target folder
        setup(7777);
        shutdownAndVerify();
    }

    @Test
    public void deployUseUndeployCheck() throws Exception {
        // DO NOT RUN FROM ECLIPSE
        // The test uses files from the target folder
        setup(7778);
        FixedPhantomJSDriver driver = new FixedPhantomJSDriver(
                DesiredCapabilities.phantomjs());
        try {
            driver.get("http://localhost:7778/");
            Assert.assertNotNull(driver.findElement(By.id("hello")));
        } finally {
            driver.close();
        }
        shutdownAndVerify();
    }

    public void setup(int port) throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("org.eclipse.jetty.util.log.class",
                JavaUtilLog.class.getName());

        server = new Server();

        final ServerConnector connector = new ServerConnector(server);

        connector.setPort(port);
        server.setConnectors(new ServerConnector[] { connector });

        File[] warDirs = new File("target").listFiles((FileFilter) file -> {
            return file.getName()
                    .matches("hummingbird-test-memory-leaks-.*-SNAPSHOT");
        });
        String wardir = "target/" + warDirs[0].getName();

        context = new WebAppContext();
        context.setResourceBase(wardir);
        context.setContextPath("/");
        context.setConfigurations(
                new Configuration[] { new AnnotationConfiguration(),
                        new WebXmlConfiguration(), new WebInfConfiguration(),
                        // new TagLibConfiguration(),
                        new PlusConfiguration(), new MetaInfConfiguration(),
                        new FragmentConfiguration(), new EnvConfiguration() });
        server.setHandler(context);
        server.start();

        testReference = new WeakReference<>(
                Class.forName(testClass, true, context.getClassLoader()));
        Assert.assertNotNull(testReference.get());

    }

    public void shutdownAndVerify() throws Exception {
        try {
            context.stop();
            context.destroy();
            context = null;

            // This is needed for the class to be actually GCed
            for (int i = 0; i < 10; i++) {
                System.gc();
                Thread.sleep(100);
                if (testReference.get() == null) {
                    break;
                }
            }

            Assert.assertNull(testReference.get());

        } finally {
            server.stop();
        }

    }

}
