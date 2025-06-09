/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
                "com\\.vaadin\\.flow\\.server\\.startup\\.ApplicationRouteRegistry(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.ComponentEffect"),
                super.getExcludedPatterns());
    }
}
