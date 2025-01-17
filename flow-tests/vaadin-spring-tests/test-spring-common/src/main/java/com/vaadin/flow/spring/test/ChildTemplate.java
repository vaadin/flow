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
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("child-template")
@JsModule("./ChildTemplate.js")
public class ChildTemplate extends PolymerTemplate<TemplateModel> {

    @Component
    @VaadinSessionScope
    public static class BackendImpl implements Backend {

        @Override
        public String getMessage() {
            return "foo";
        }
    }

    public interface Backend {
        String getMessage();
    }

    @Autowired
    private Backend backend;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getElement().setProperty("message", backend.getMessage());
    }
}
