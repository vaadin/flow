package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class FlowModeAbstractMojo extends AbstractMojo{
    /**
     * Whether or not we are running in bowerMode.
     */
    @Parameter(defaultValue = "${vaadin.bowerMode}")
    public String bowerMode;
    /**
     * Whether or not we are running in productionMode.
     */
    @Parameter(defaultValue = "${vaadin.productionMode}")
    public boolean productionMode;

    public boolean bower;

    @Override
    public void execute() {
        // Default mode for V14 is bower true
        bower = bowerMode != null ? Boolean.valueOf(bowerMode) : isDefaultBower();
    }

    abstract boolean isDefaultBower();
}
