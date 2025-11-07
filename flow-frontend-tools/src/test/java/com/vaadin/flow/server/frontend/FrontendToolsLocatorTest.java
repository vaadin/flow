/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.Optional;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrontendToolsLocatorTest {
    private final FrontendToolsLocator locator = new FrontendToolsLocator();

    @Test
    public void toolLocated() {
        Optional<File> echoLocation = locator.tryLocateTool("mvn");
        assertTrue("Should be able to find 'mvn' binary",
                echoLocation.isPresent());
    }

    @Test
    public void toolLocated_verificationFailed() {
        Assume.assumeFalse("Cannot run the test on Windows",
                locator.isWindows());
        Optional<File> sedLocation = locator.tryLocateTool("sed");
        assertFalse(
                "Sed location should not be available due to lack of '-v' flag support",
                sedLocation.isPresent());
    }

    @Test
    public void toolNotLocated() {
        Optional<File> unknownToolLocation = locator
                .tryLocateTool("sdhajgsdiasg!");
        assertFalse("Unknown tool should not be found in the system",
                unknownToolLocation.isPresent());
    }

    @Test
    public void nonExistentTool_notVerified() {
        assertFalse("Non-existent tool should not be a valid one",
                locator.verifyTool(new File("whatever!")));
    }
}
