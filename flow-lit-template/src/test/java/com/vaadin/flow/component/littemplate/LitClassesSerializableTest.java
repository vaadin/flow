/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.littemplate;

import java.util.stream.Stream;

import com.vaadin.flow.testutil.ClassesSerializableTest;

public class LitClassesSerializableTest extends ClassesSerializableTest {

    @Override
    protected Stream<String> getExcludedPatterns() {
        return Stream.concat(Stream.of(
                "com\\.vaadin\\.flow\\.component\\.littemplate\\.BundleLitParser(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.littemplate\\.internal\\.LitTemplateParserImpl",
                "com\\.vaadin\\.flow\\.component\\.littemplate\\.LitTemplateParser(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.littemplate\\.LitTemplateInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.littemplate\\.InjectableLitElementInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ApplicationRouteRegistry(\\$.*)?"),
                super.getExcludedPatterns());
    }
}
