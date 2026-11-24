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
package com.vaadin.cdi.itest;

import java.io.File;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

import com.vaadin.flow.server.InitParameters;

public class ArchiveProvider {

    public static WebArchive createWebArchive(String warName,
            Consumer<WebArchive> customizer) {
        WebArchive archive = base(warName);
        customizer.accept(archive);
        return archive;
    }

    public static WebArchive createWebArchive(String warName,
            Class... classes) {
        return createWebArchive(warName, archive -> archive.addClasses(classes)
                .addAsResource(new File("target/classes/META-INF")));
    }

    private static WebArchive base(String warName) {
        PomEquippedResolveStage pom = Maven.configureResolver().workOffline()
                .loadPomFromFile("target/effective-pom.xml");
        WebArchive archive = ShrinkWrap
                .create(WebArchive.class, warName + ".war")
                .addAsLibraries(pom.resolve("com.vaadin:vaadin-cdi",
                        "com.vaadin:flow-server", "com.vaadin:flow-client",
                        "com.vaadin:flow-html-components",
                        "com.vaadin:flow-polymer-template").withTransitivity()
                        .asFile())
                .addAsWebInfResource(EmptyAsset.INSTANCE,
                        ArchivePaths.create("beans.xml"))
                .addClasses(Counter.class, CounterFilter.class);
        addCspMode(archive);
        return applyContainerConfigurations(archive, pom);
    }

    /**
     * Passes the {@code vaadin.csp} system property of the test JVM to the
     * deployment as a {@code csp} context parameter. The containers run in
     * their own JVM, and each takes its JVM options in a different way. A web
     * fragment in a library JAR is used because some tests add their own
     * {@code web.xml}.
     */
    private static void addCspMode(WebArchive archive) {
        String cspMode = System.getProperty("vaadin.csp");
        if (cspMode == null || cspMode.isBlank()) {
            return;
        }
        String webFragment = """
                <?xml version="1.0" encoding="UTF-8"?>
                <web-fragment xmlns="https://jakarta.ee/xml/ns/jakartaee"
                    version="6.0">
                  <name>vaadin_csp_mode</name>
                  <context-param>
                    <param-name>%s</param-name>
                    <param-value>%s</param-value>
                  </context-param>
                </web-fragment>
                """.formatted(InitParameters.CSP, cspMode);
        archive.addAsLibrary(
                ShrinkWrap.create(JavaArchive.class, "vaadin-csp-mode.jar")
                        .addAsManifestResource(new StringAsset(webFragment),
                                "web-fragment.xml"));
    }

    private static WebArchive applyContainerConfigurations(WebArchive archive,
            PomEquippedResolveStage pom) {
        // Testing Tomcat + Weld
        String container = System.getProperty("arquillian.launch");
        if ("tomcat-weld".equals(container)) {
            archive.addAsLibraries(pom
                    .resolve("org.slf4j:slf4j-simple",
                            "org.jboss.weld.servlet:weld-servlet-shaded")
                    .withoutTransitivity().asFile());

        }
        // Workaround for https://github.com/payara/Payara/issues/5898
        // Slf4J implementation lookup error in Payara 6
        else if ("payara".equals(container)) {
            archive.addAsWebInfResource(AbstractCdiTest.class.getClassLoader()
                    .getResource("payara/glassfish-web.xml"),
                    "glassfish-web.xml")
                    .addAsLibraries(pom.resolve("org.slf4j:slf4j-simple")
                            .withoutTransitivity().asFile());

        } else if ("wildfly".equals(container)) {
            archive.addAsWebInfResource(
                    AbstractCdiTest.class.getClassLoader().getResource(
                            "wildfly/jboss-deployment-structure.xml"),
                    "jboss-deployment-structure.xml");
        }

        return archive;
    }

}
