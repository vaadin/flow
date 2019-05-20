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
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;

import static org.hamcrest.core.StringStartsWith.startsWith;

public class WebComponentGeneratorTest {

    @Test
    public void generatedReplacementMapContainsExpectedEntriesIncludingUi() {
        assertGeneratedReplacementMapContainsExpectedEntries(true);
    }

    @Test
    public void generatedReplacementMapContainsExpectedEntriesExcludingUi() {
        assertGeneratedReplacementMapContainsExpectedEntries(false);
    }

    public void assertGeneratedReplacementMapContainsExpectedEntries(
            boolean generateUi) {
        MyComponentExporter exporter = new MyComponentExporter();

        Map<String, String> replacementsMap = WebComponentGenerator
                .getReplacementsMap("my-component",
                        new WebComponentExporter.WebComponentConfigurationFactory().create(exporter)
                                .getPropertyDataSet(),
                        "/foo", generateUi);

        Assert.assertTrue("Missing dashed tag name",
                replacementsMap.containsKey("TagDash"));
        Assert.assertTrue("Missing camel cased tag name",
                replacementsMap.containsKey("TagCamel"));
        Assert.assertTrue("Missing 'PropertyMethods'",
                replacementsMap.containsKey("PropertyMethods"));
        Assert.assertTrue("Missing 'Properties'",
                replacementsMap.containsKey("Properties"));
        Assert.assertTrue("Missing frontend resources path",
                replacementsMap.containsKey("frontend_resources"));
        Assert.assertTrue("Missing ui import",
                replacementsMap.containsKey("ui_import"));

        Assert.assertEquals("my-component", replacementsMap.get("TagDash"));
        Assert.assertEquals("MyComponent", replacementsMap.get("TagCamel"));

        Assert.assertEquals("/foo", replacementsMap.get("frontend_resources"));

        if (generateUi) {
            Assert.assertEquals(
                    "<link rel='import' href='web-component-ui.html'>",
                    replacementsMap.get("ui_import"));
        } else {
            Assert.assertEquals("", replacementsMap.get("ui_import"));
        }

        String propertyMethods = replacementsMap.get("PropertyMethods");
        Assert.assertTrue(propertyMethods.contains("_sync_message"));
        Assert.assertTrue(propertyMethods.contains("_sync_integerValue"));
        Assert.assertTrue(propertyMethods.contains("_sync_response"));

        String properties = replacementsMap.get("Properties");
        Assert.assertTrue(properties
                .contains("\"message\":{\"type\":\"String\",\"value\":\"\""));
        Assert.assertTrue(properties
                .contains("\"integer-value\":{\"type\":\"Integer\",\"value\":0,"
                        + "\"observer\""));
        Assert.assertTrue(properties.contains(
                "\"response\":{\"type\":\"String\",\"value\":\"hello\""));
    }

    @Test
    public void providesHTMLModuleInBowerMode() {
        String module = WebComponentGenerator.generateModule(MyComponentExporter.class, "",
                true);
        // make sure that the test works on windows machines:
        module = module.replace("\r","");
        Assert.assertThat(module, startsWith("" +
                        "<link rel=\"import\" href=\"bower_components/polymer/polymer-element.html\">\n" +
                        "\n" +
                        "\n" +
                        "<dom-module id=\"tag\">\n" +
                        "  <template>\n" +
                        "\n" +
                        "    <style>\n" +
                        "      :host {\n" +
                        "        display: inline-block;\n" +
                        "      }\n" +
                        "    </style>\n" +
                        "    <slot></slot>\n" +
                        "  </template>\n" +
                        "  <script>\n" +
                        "    class Tag extends Polymer.Element {\n" +
                        // this part is from the com.vaadin.flow.webcomponent-script-template
                        // .js to verify successful import
                        "      static get is() {\n" +
                        "    return 'tag';\n" +
                        "  }"
                ));
    }

    @Test
    public void providesJSModulesInNpmMode() {
        String module = WebComponentGenerator.generateModule(MyComponentExporter.class, "",
                false);
        // make sure that the test works on windows machines:
        module = module.replace("\r","");
        Assert.assertThat(module, startsWith("" +
                "import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';\n" +
                "\n" +
                "class Tag extends PolymerElement {\n" +
                "  static get template() {\n" +
                "    return html`\n" +
                "        <style>\n" +
                "          :host {\n" +
                "            display: inline-block;\n" +
                "          }\n" +
                "        </style>\n" +
                "        <slot></slot>\n" +
                "    `;\n" +
                "  }\n" +
                // this part is from the com.vaadin.flow.webcomponent-script-template
                // .js to verify successful import
                "    static get is() {\n" +
                "    return 'tag';\n" +
                "  }"
        ));
    }

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

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter() {
            super("tag");
            addProperty("response", "hello")
                    .onChange(MyComponent::setMessage);
            addProperty("integer-value", 0)
                    .onChange(MyComponent::setIntegerValue);
            addProperty("message", "")
                    .onChange(MyComponent::setMessage);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }
}
