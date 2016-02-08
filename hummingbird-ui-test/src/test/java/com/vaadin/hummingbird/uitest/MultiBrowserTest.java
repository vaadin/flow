package com.vaadin.hummingbird.uitest;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.Parameters;
import com.vaadin.testbench.annotations.BrowserConfiguration;
import com.vaadin.testbench.annotations.BrowserFactory;
import com.vaadin.testbench.annotations.RunOnHub;
import com.vaadin.testbench.parallel.Browser;
import com.vaadin.testbench.parallel.ParallelRunner;

@BrowserFactory(SauceLabsBrowserFactory.class)
@RunWith(ParallelRunner.class)
@RunOnHub
public class MultiBrowserTest extends AbstractTestBenchTest {

    static {
        Parameters.setTestsInParallel(5);
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        try {
            testBench().resizeViewPortTo(1000, 800);
        } catch (UnsupportedOperationException e) {
            // Opera does not support this...
        }

    }

    /**
     * Produces a collection of browsers to run the test on. This method is
     * executed by the test runner when determining how many test methods to
     * invoke and with what parameters. For each returned value a test method is
     * ran and before running that,
     * {@link #setDesiredCapabilities(DesiredCapabilities)} is invoked with the
     * value returned by this method.
     *
     * This method is not static to allow overriding it in sub classes. By
     * default runs the test only on Firefox
     *
     * @return The browsers to run the test on
     */
    @BrowserConfiguration
    public List<DesiredCapabilities> getBrowsersToTest() {
        SauceLabsBrowserFactory f = new SauceLabsBrowserFactory();

        List<DesiredCapabilities> browserCapabilities = new ArrayList<>();
        browserCapabilities.add(f.create(Browser.CHROME));
        browserCapabilities.add(f.create(Browser.FIREFOX));
        browserCapabilities.add(f.create(Browser.SAFARI));
        browserCapabilities.add(f.create(Browser.IE11));
        browserCapabilities.add(f.create(Browser.EDGE));
        browserCapabilities.add(f.createIphone6Ios92());
        browserCapabilities.add(f.createAndroid51Phone());

        String travisJob = System.getenv("TRAVIS_JOB_NUMBER");
        if (travisJob != null) {
            for (DesiredCapabilities dc : browserCapabilities) {
                dc.setCapability("tunnel-identifier", travisJob);
            }
        }

        return browserCapabilities;
    }

    @Override
    protected String getHubURL() {
        String username = "username";
        String key = "access_key";
        if (System.getProperty("SAUCE_USERNAME") != null) {
            username = System.getProperty("SAUCE_USERNAME");
        }
        if (System.getProperty("SAUCE_ACCESS_KEY") != null) {
            key = System.getProperty("SAUCE_ACCESS_KEY");
        }
        return "http://" + username + ":" + key
                + "@ondemand.saucelabs.com/wd/hub";
    }
}