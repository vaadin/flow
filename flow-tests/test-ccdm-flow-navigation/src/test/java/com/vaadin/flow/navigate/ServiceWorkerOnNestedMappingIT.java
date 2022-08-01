package com.vaadin.flow.navigate;

import org.junit.After;
import org.junit.Ignore;

@Ignore("Vite, service worker not working on nested path")
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
