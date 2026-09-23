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
package com.vaadin.flow.devloop.test.it;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resource leg through a servlet container.
 * <p>
 * {@link DevLoopCssIT} covers the sibling module's stylesheet, which a
 * container serves because it arrives in a jar on the webapp class path. The
 * application's own is the case only a WAR has: {@code src/main/resources} is
 * packaged into {@code WEB-INF/classes}, which the servlet spec has no
 * container serve from - Spring Boot serves
 * {@code classpath:/META-INF/resources} itself, which is why
 * {@code test-devloop-spring} can fetch its own over HTTP and this module
 * cannot. The copy on the classpath is what is observable here, and it is the
 * part the daemon is responsible for either way.
 */
class DevLoopJettyCssIT extends AbstractDevLoopIT {

    private static final Path APP_CSS = APP
            .resolve("src/main/resources/META-INF/resources/styles.css");

    @Test
    void cssEdit_refreshesTheApplicationsOwnClasspathCopy() throws Exception {
        patch.replace(APP_CSS, "margin: 0", "margin: 4px");

        cli.run("apply").assertExitCode(0).assertOutputContains("styles.css");

        Path copy = APP.resolve("target/classes/META-INF/resources")
                .resolve("styles.css");
        assertTrue(Files.readString(copy).contains("margin: 4px"),
                () -> "expected the copied stylesheet at " + copy
                        + " to hold the edited value");
    }
}
