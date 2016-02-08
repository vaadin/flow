package com.vaadin.hummingbird.uitest;

import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

public class PhantomJSTest extends AbstractTestBenchTest {

    @Override
    public void setup() throws Exception {
        DesiredCapabilities cap = DesiredCapabilities.phantomjs();
        PhantomJSDriver phantomJSDriver = new PhantomJSDriver(cap);
        setDriver(phantomJSDriver);
    }

}