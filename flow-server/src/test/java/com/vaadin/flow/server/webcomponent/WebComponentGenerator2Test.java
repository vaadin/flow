/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server.webcomponent;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponent;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;

public class WebComponentGenerator2Test {

    @Test
    public void generatedReplacementMapContainsExpectedEntries() {
        // this is usually constructed automatically:
        WebComponentBuilder<MyComponent> builder =
                new WebComponentBuilder<>(new MyComponentExporter());


        Map<String, String> replacementsMap = WebComponentGenerator2
                .getReplacementsMap("document.body", "my-component",
                        builder.getPropertyDataSet(), "/foo");

        Assert.assertTrue("Missing dashed tag name",
                replacementsMap.containsKey("TagDash"));
        Assert.assertTrue("Missing camel cased tag name",
                replacementsMap.containsKey("TagCamel"));
        Assert.assertTrue("Missing 'PropertyMethods'",
                replacementsMap.containsKey("PropertyMethods"));
        Assert.assertTrue("Missing 'Properties'",
                replacementsMap.containsKey("Properties"));
        Assert.assertTrue("No 'RootElement' specified",
                replacementsMap.containsKey("RootElement"));
        Assert.assertTrue("Missing servlet context path",
                replacementsMap.containsKey("servlet_context"));

        Assert.assertEquals("my-component", replacementsMap.get("TagDash"));
        Assert.assertEquals("MyComponent", replacementsMap.get("TagCamel"));

        Assert.assertEquals("document.body",
                replacementsMap.get("RootElement"));

        Assert.assertEquals("/foo", replacementsMap.get("servlet_context"));

        String propertyMethods = replacementsMap.get("PropertyMethods");
        Assert.assertTrue(propertyMethods.contains("_sync_message"));
        Assert.assertTrue(propertyMethods.contains("_sync_integerValue"));
        Assert.assertTrue(propertyMethods.contains("_sync_response"));

        String properties = replacementsMap.get("Properties");
        Assert.assertTrue(properties
                .contains("\"message\":{\"type\":\"String\",\"value\":\"\""));
        Assert.assertTrue(properties.contains(
                "\"integerValue\":{\"type\":\"Integer\",\"observer\""));
        Assert.assertTrue(properties.contains(
                "\"response\":{\"type\":\"String\",\"value\":\"hello\""));

    }

    @WebComponent("my-component")
    public static class MyComponent extends Component {
        // these will be initialized by the callbacks (if this was the real
        // world)
        private String response;
        private int integerValue;
        private String message;


        public void setResponse(String response) {
            this.response = response;
        }

        public void setIntegerValue(int integerValue) {
            this.integerValue = integerValue;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class MyComponentExporter implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return "tag";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            definition.addProperty("response", "hello")
                    .onChange(MyComponent::setMessage);
            definition.addProperty("integerValue", Integer.class)
                    .onChange(MyComponent::setIntegerValue);
            definition.addProperty("message", "")
                    .onChange(MyComponent::setMessage);
        }
    }
}
