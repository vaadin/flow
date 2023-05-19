/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.stream.Stream;

import com.vaadin.flow.testutil.ClassesSerializableTest;

public class PolymerClassesSerializableTest extends ClassesSerializableTest {

    @Override
    protected Stream<String> getExcludedPatterns() {
        return Stream.concat(Stream.of(
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.DefaultTemplateParser",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.NpmTemplateParser",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.BundleParser",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.BundleParser\\$DependencyVisitor",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateDataAnalyzer\\$.*",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateParser(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.InjectablePolymerElementInitializer",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateDataAnalyzer",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.IdCollector"),
                super.getExcludedPatterns());
    }
}
