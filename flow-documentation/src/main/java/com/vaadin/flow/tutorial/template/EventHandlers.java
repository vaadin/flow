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
package com.vaadin.flow.tutorial.template;

import com.vaadin.annotations.ClientDelegate;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.AngularTemplate;

@Deprecated
@CodeFor("deprecated/tutorial-template-event-handlers.asciidoc")
public class EventHandlers {
    public interface MyModel extends TemplateModel {
        public void setHelloText(String helloText);
    }

    public class MyTemplate extends AngularTemplate {

        @Override
        public MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @ClientDelegate
        private void sayHello(String name) {
            getModel().setHelloText("Hello " + name);
        }
    }
}
