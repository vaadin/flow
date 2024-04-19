package com.vaadin.flow.testutil;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("vaadin-dev-tools")
public class DevToolsElement extends TestBenchElement {

    public void setLiveReload(boolean enabled) {
        callFunction("setActive", enabled);
    }

}
