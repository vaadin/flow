package com.vaadin.hummingbird.testutil;

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
        PhantomJSDriver phantomJSDriver = new PhantomJSDriver(cap);
        setDriver(phantomJSDriver);
    }

}