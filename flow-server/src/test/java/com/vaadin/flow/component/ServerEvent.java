/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.math.BigDecimal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

public class ServerEvent extends ComponentEvent<Component> {

    private BigDecimal someValue;

    public ServerEvent(Component source, BigDecimal someValue) {
        super(source, false);
        this.someValue = someValue;
    }

    public BigDecimal getSomeValue() {
        return someValue;
    }

}
