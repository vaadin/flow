/*
 * Copyright 2000-2026 Vaadin Ltd.
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

/**
 * Data binding, conversion and validation for Vaadin Flow.
 */
open module com.vaadin.flow.data {
    requires transitive com.vaadin.flow.server;
    // java.sql.Date converters
    requires transitive java.sql;

    // Bean validation is used when an implementation is available
    requires static jakarta.validation;
    requires static org.jspecify;

    exports com.vaadin.flow.data.binder;
    exports com.vaadin.flow.data.converter;
    exports com.vaadin.flow.data.event;
    exports com.vaadin.flow.data.provider;
    exports com.vaadin.flow.data.provider.hierarchy;
    exports com.vaadin.flow.data.selection;
    exports com.vaadin.flow.data.validator;
    exports com.vaadin.flow.data.value;
}
