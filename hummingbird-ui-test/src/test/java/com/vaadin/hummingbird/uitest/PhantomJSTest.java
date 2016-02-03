package com.vaadin.hummingbird.uitest;

import org.junit.Before;
import org.junit.Rule;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.ScreenshotOnFailureRule;

public class PhantomJSTest extends AbstractTestBenchTest {

    @Rule
    public ScreenshotOnFailureRule screenshotOnFailure = new ScreenshotOnFailureRule(
            this, true);

    @Before
    public void setupDriver() throws Exception {
        DesiredCapabilities cap = DesiredCapabilities.phantomjs();
        // cap.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
        // "--logLevel=DEBUG");
        PhantomJSDriver phantomJSDriver = new PhantomJSDriver(cap);
        // phantomJSDriver.setLogLevel(Level.FINEST);
        setDriver(phantomJSDriver);
    }

}