/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TestUIInitListener implements UIInitListener {

    List<UIInitEvent> events = new ArrayList<>();

    @Override
    public void uiInit(UIInitEvent event) {
        events.add(event);
    }

}
