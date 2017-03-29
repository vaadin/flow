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

import java.util.Arrays;

import com.vaadin.annotations.WebComponents;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@WebComponents(1)
public class DomRepeatUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        DomRepeatPolymerTemplate template = new DomRepeatPolymerTemplate();
        template.setId("template");
        DomRepeatPolymerTemplate.Employee employee1 = new DomRepeatPolymerTemplate.Employee("name1", "title1", "email1", 1L);
        DomRepeatPolymerTemplate.Employee employee2 = new DomRepeatPolymerTemplate.Employee("name2", "title2", "email2", 2L);
        template.setEmployees(Arrays.asList(employee1, employee2));

        add(template);
    }
}
