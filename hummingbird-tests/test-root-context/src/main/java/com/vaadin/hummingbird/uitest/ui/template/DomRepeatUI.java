/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.annotations.WebComponents;
import com.vaadin.annotations.WebComponents.PolyfillVersion;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@WebComponents(PolyfillVersion.V1)
public class DomRepeatUI extends UI {
    static final int NUMBER_OF_EMPLOYEES = 3;
    static final String EVENT_INDEX_ID = "eventIndex";
    static final String REPEAT_INDEX_ID = "repeatIndex";
    static final String TR_ID_PREFIX = "name";

    @Override
    protected void init(VaadinRequest request) {
        DomRepeatPolymerTemplate template = new DomRepeatPolymerTemplate();
        template.setId("template");
        template.setEmployees(IntStream.range(0, NUMBER_OF_EMPLOYEES)
                .mapToObj(i -> new DomRepeatPolymerTemplate.Employee("name" + i,
                        "title" + i, "email" + i))
                .collect(Collectors.toList()));

        add(template);
    }
}
