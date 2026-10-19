/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Properties;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinServletService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BundleParserTest {

    private MockVaadinServletService service;
    private DeploymentConfiguration configuration;

    @BeforeEach
    void init() {
        configuration = mock(DeploymentConfiguration.class);
        when(configuration.getStringProperty(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Properties properties = new Properties();
        when(configuration.getInitParameters()).thenReturn(properties);

        Instantiator instantiator = mock(Instantiator.class);
        when(instantiator.getServiceInitListeners()).thenReturn(Stream.empty());
        when(instantiator.getDependencyFilters(any()))
                .thenReturn(Stream.empty());
        when(instantiator.getIndexHtmlRequestListeners(any()))
                .thenReturn(Stream.empty());
        service = new MockVaadinServletService(configuration);
        service.init(instantiator);
    }

    @Test
    void parseTemplateElement_stringContentNotSeenAsComment() {
        String source = "static get template() { return html`<vaadin-text-field label=\"Nats Url(s)\" placeholder=\"nats://server:port\" id=\"natsUrlTxt\" style=\"width:100%\"></vaadin-text-field>`;}";
        Element element = BundleParser.parseTemplateElement("nats.js", source);

        Element natsElement = element.getElementById("natsUrlTxt");
        assertNotNull(natsElement, "Found element by Id");
        assertEquals("vaadin-text-field", natsElement.tagName(),
                "Invalid tag for element");

        assertEquals("nats://server:port", natsElement.attr("placeholder"),
                "Parsed value for attribute 'placeholder' was wrong.");

    }

    @Test
    void parseTemplateElement_spacesBetweenHtmlAndTick() {
        String source = "static get template() { return html    `<div id='bar'></div>`;}";
        Element element = BundleParser.parseTemplateElement("foo.js", source);

        assertNotNull(element.getElementById("bar"));
    }
}
