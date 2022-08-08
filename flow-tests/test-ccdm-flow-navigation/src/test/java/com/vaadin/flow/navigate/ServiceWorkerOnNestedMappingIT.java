package com.vaadin.flow.navigate;

import org.junit.After;
import org.junit.Ignore;

@Ignore("Service worker not working on nested path with VITE. See https://github.com/vaadin/flow/issues/14227")
public class ServiceWorkerOnNestedMappingIT extends ServiceWorkerIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/nested";
    }

    @After
    public void tearDown() {
        if (getDriver() != null) {
            checkLogsForErrors(message -> !message.toLowerCase()
                    .contains("failed to register a serviceworker"));
        }
    }

}
