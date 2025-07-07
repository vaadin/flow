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

package com.vaadin.flow.server.webcomponent;

import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

import static org.hamcrest.core.StringContains.containsString;
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
                        new WebComponentExporter.WebComponentConfigurationFactory()
                                .create(exporter).getPropertyDataSet(),
                        "/foo", generateUi, null);

        Assert.assertTrue("Missing dashed tag name",
                replacementsMap.containsKey("TagDash"));
        Assert.assertTrue("Missing camel cased tag name",
                replacementsMap.containsKey("TagCamel"));
        Assert.assertTrue("Missing 'AttributeChange'",
                replacementsMap.containsKey("AttributeChange"));
        Assert.assertTrue("Missing 'PropertyDefaults'",
                replacementsMap.containsKey("PropertyDefaults"));
        Assert.assertTrue("Missing 'PropertyMethods'",
                replacementsMap.containsKey("PropertyMethods"));
        Assert.assertTrue("Missing 'PropertyValues'",
                replacementsMap.containsKey("PropertyValues"));
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

        String attributeChange = replacementsMap.get("AttributeChange");
        MatcherAssert.assertThat(attributeChange,
                containsString(String.format("if (attribute === 'message') {%n"
                        + "  this['message'] = this._deserializeValue(value, "
                        + "String);")));
        MatcherAssert.assertThat(attributeChange, containsString(
                String.format("if (attribute === 'integer-value') {%n"
                        + "  this['integer-value'] = this._deserializeValue"
                        + "(value, Number);")));
        MatcherAssert.assertThat(attributeChange, containsString(
                String.format("if (attribute === 'camel-case-value') {%n"
                        + "  this['camelCaseValue'] = this._deserializeValue"
                        + "(value, Number);")));
        MatcherAssert.assertThat(attributeChange,
                containsString(String.format("if (attribute === 'response') {%n"
                        + "  this['response'] = this._deserializeValue(value,"
                        + " String);")));

        String propertyMethods = replacementsMap.get("PropertyMethods");
        MatcherAssert.assertThat(propertyMethods,
                containsString("get ['message']() {"));
        MatcherAssert.assertThat(propertyMethods,
                containsString("set ['message'](value) {"));
        MatcherAssert.assertThat(propertyMethods,
                containsString("this._sync('message',"));
        MatcherAssert.assertThat(propertyMethods,
                containsString("set ['integer-value'](value) {"));
        MatcherAssert.assertThat(propertyMethods,
                containsString("this._sync('integer-value',"));
        MatcherAssert.assertThat(propertyMethods,
                containsString("set ['response'](value) {"));
        MatcherAssert.assertThat(propertyMethods,
                containsString("this._sync('response',"));

        String propertyValues = replacementsMap.get("PropertyValues");
        MatcherAssert.assertThat(propertyValues,
                containsString("'message': this['message']"));
        MatcherAssert.assertThat(propertyValues,
                containsString("'integer-value': this['integer-value']"));
        MatcherAssert.assertThat(propertyValues,
                containsString("'response': this['response']"));

        String propertyDefaults = replacementsMap.get("PropertyDefaults");
        MatcherAssert.assertThat(propertyDefaults,
                containsString("this['_message'] = ''"));
        MatcherAssert.assertThat(propertyDefaults,
                containsString("this['_integer-value'] = 0"));
        MatcherAssert.assertThat(propertyDefaults,
                containsString("this['_response'] = 'hello'"));
    }

    @Test
    public void providesJSModulesInNpmMode() {
        String module = WebComponentGenerator.generateModule(
                new DefaultWebComponentExporterFactory<MyComponent>(
                        MyComponentExporter.class),
                "", null);
        // make sure that the test works on windows machines:
        module = module.replace("\r", "");
        MatcherAssert.assertThat(module, startsWith(
                "import {applyCss} from 'Frontend/generated/css.generated.js';\n"
                        + "\nclass Tag extends HTMLElement {"));
        MatcherAssert.assertThat(module, containsString("style.innerHTML = `\n" //
                + "      :host {\n" //
                + "        position: relative;\n" //
                + "        display: inline-block;\n" //
                + "      }\n" //
                + "    `;\n"));

        MatcherAssert.assertThat(module,
                containsString("customElements.define('tag', Tag);\n"));
    }

    @Test
    public void providedJSModuleContainsCorrectThemeReplacements() {
        String module = WebComponentGenerator
                .generateModule(new DefaultWebComponentExporterFactory<>(
                        MyComponentExporter.class), "", "my-theme");
        // make sure that the test works on windows machines:
        module = module.replace("\r", "");
        MatcherAssert.assertThat(module,
                startsWith("import {applyTheme} from '"
                        + "Frontend/generated/theme.js';\n\nclass Tag extends "
                        + "HTMLElement {"));
        MatcherAssert.assertThat(module, containsString("style.innerHTML = `\n" //
                + "      :host {\n" //
                + "        position: relative;\n" //
                + "        display: inline-block;\n" //
                + "      }\n" //
                + "    `;\n"));

        MatcherAssert.assertThat(module, containsString(
                "applyTheme(shadow);\n    shadow.appendChild(style);"));
        MatcherAssert.assertThat(module,
                containsString("customElements.define('tag', Tag);\n"));
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
            addProperty("response", "hello").onChange(MyComponent::setMessage);
            addProperty("integer-value", 0)
                    .onChange(MyComponent::setIntegerValue);
            addProperty("message", "").onChange(MyComponent::setMessage);
            addProperty("camelCaseValue", 0);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }
}
