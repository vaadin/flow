package com.vaadin.flow.testutil;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class CurrentInstanceCleaner extends RunListener {
    @Override
    public void testStarted(Description description) throws Exception {
        // Clear current instances before each test so a previous test does not
        // affect the test
        try {
            Class<?> cls = Class
                    .forName("com.vaadin.flow.internal.CurrentInstance");
            cls.getMethod("clearAll").invoke(null);
        } catch (Exception e) { // NOSONAR
            // Not a Flow module
        }
    }
}
