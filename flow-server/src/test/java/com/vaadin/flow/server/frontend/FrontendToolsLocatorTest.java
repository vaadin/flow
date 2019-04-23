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
