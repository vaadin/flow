package com.vaadin.flow.plugin.maven;

import com.vaadin.flow.server.frontend.EndpointUsageDetector;
import com.vaadin.flow.server.frontend.Options;

public class TestEndpointUsageDetector implements EndpointUsageDetector {

    @Override
    public boolean areEndpointsUsed(Options options) {
        return true;
    }
}
