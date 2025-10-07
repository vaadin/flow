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
package com.vaadin.flow.spring.test.filtering;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("")
public class ClassScannerView extends Div {

    public static Set<Class<?>> classes = Collections.emptySet();
    public static final String SCANNED_CLASSES = "scanned-classes";

    public ClassScannerView() {
        Span scannedClasses = new Span(classes.stream()
                .map(Class::getSimpleName).collect(Collectors.joining(",")));
        scannedClasses.setId(SCANNED_CLASSES);
        add(scannedClasses);
    }

}
