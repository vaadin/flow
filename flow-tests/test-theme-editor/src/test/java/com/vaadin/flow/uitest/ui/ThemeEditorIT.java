/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.base.devserver.themeeditor.ThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.base.devserver.themeeditor.utils.ComponentsMetadata;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsCheckboxPropertyEditorElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsThemeClassNameEditorElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsThemeColorPropertyEditorElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsThemeRangePropertyEditorElement;
import com.vaadin.testbench.TestBenchElement;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.fail;

@NotThreadSafe
public class ThemeEditorIT extends AbstractThemeEditorIT {
    @Override
    protected String getTestPath() {
        return "/context/view/com.vaadin.flow.uitest.ui.ThemeEditorView";
    }

    @Test
    public void testButton() throws IOException {
        List<Metadata> buttonMetadata = extractMetadata("vaadin-button");

        open();

        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();
        devTools.expand();

        devTools.showThemeEditor();

        TestBenchElement themeEditor = devTools
                .$("vaadin-dev-tools-theme-editor").first();
        themeEditor.$("button").first().click();

        new Actions(getDriver()).click(findElement(By.id("button"))).perform();

        DevToolsThemeClassNameEditorElement classNameEditor = themeEditor
                .$(DevToolsThemeClassNameEditorElement.class).waitForFirst();
        String cssClassName = classNameEditor.getValue();

        TestBenchElement propertiesList = themeEditor
                .$("vaadin-dev-tools" + "-theme-property-list").waitForFirst();

        List<TestBenchElement> propertyEditors = propertiesList.$("*")
                .hasAttribute("data-testid").all();

        List<String> dataTestIds = new ArrayList<>(propertyEditors.size());
        for (TestBenchElement testBenchElement : propertyEditors) {
            WebElement webElement = testBenchElement.getWrappedElement();
            String dataTestIdAttribute = webElement.getAttribute("data-testid");
            dataTestIds.add(dataTestIdAttribute);
        }

        for (Metadata metadata : buttonMetadata) {
            Map<String, String> cssProperties = new HashMap<>();
            for (Property property : metadata.getProperties()) {
                if (!dataTestIds.contains(property.getPropertyName())) {
                    fail();
                }
                Optional<String> oValue = updateProperty(propertiesList,
                        property);
                if (!oValue.isEmpty()) {
                    String value = oValue.get();
                    cssProperties.put(property.getPropertyName(), value);
                    // ouch...
                    if (property.getPropertyName().equals("border-width")
                            && !value.equals("0px")) {
                        cssProperties.put("border-style", "solid");
                    }
                }
            }
            String cssSelector = createSelector(metadata, cssClassName);
            Optional<CssRule> oActual = getCssRuleFromFile(cssSelector);
            if (oActual.isEmpty()) {
                Assert.assertEquals(cssSelector, "");
            } else {
                CssRule expected = new CssRule(cssSelector, cssProperties);
                CssRule actual = oActual.get();
                Assert.assertEquals(expected, actual);
            }
        }
    }

    private String createSelector(Metadata metadata, String cssClassName) {
        if (hasPseudoElement(metadata.getSelector())) {
            return addCssClassNameToSelectorWithPseudoElement(
                    metadata.getSelector(), cssClassName);
        } else {
            return String.format("%s.%s", metadata.getSelector(), cssClassName);
        }
    }

    private String addCssClassNameToSelectorWithPseudoElement(String selector,
            String cssClassName) {
        return selector.replaceFirst("(.+)::(.+)",
                String.format("$1.%s::$2", cssClassName));
    }

    private boolean hasPseudoElement(String selector) {
        return selector.matches(".+::.+");
    }

    private Optional<CssRule> getCssRuleFromFile(String selector) {
        ThemeModifier themeModifier = new TestThemeModifier();
        List<CssRule> cssRules = themeModifier.getCssRules(List.of(selector));
        if (cssRules.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(cssRules.get(0));
        }
    }

    private Optional<String> updateProperty(TestBenchElement container,
            Property property) {
        TestBenchElement element = container.$("*")
                .attribute("data-testid", property.getPropertyName()).first();
        switch (property.getEditorType()) {
        case "color": {
            DevToolsThemeColorPropertyEditorElement colorEditor = element
                    .wrap(DevToolsThemeColorPropertyEditorElement.class);
            String value = "rgba(116, 219, 0, 1)";
            colorEditor.setValue(value);
            // TODO: we should normalize the value inside CssRule equals
            // comparison...
            return Optional.of(value.replaceAll(" ", ""));
        }
        case "checkbox": {
            DevToolsCheckboxPropertyEditorElement checkboxEditor = element
                    .wrap(DevToolsCheckboxPropertyEditorElement.class);
            checkboxEditor.setChecked(true);
            String value = checkboxEditor.getCheckedValue();
            return Optional.of(value);
        }
        case "range": {
            DevToolsThemeRangePropertyEditorElement rangeEditor = element
                    .wrap(DevToolsThemeRangePropertyEditorElement.class);
            String value = "3px";
            rangeEditor.setValue(value);
            return Optional.of(value);
        }
        }
        return Optional.empty();
    }

    private List<Metadata> extractMetadata(String componentName)
            throws IOException {
        String content = ComponentsMetadata.getMetadataContent(componentName);
        JsonArray jsonArray = Json.instance().parse(content);
        return JsonUtils.stream(jsonArray).map(
                m -> JsonUtils.readToObject((JsonObject) m, Metadata.class))
                .toList();
    }
}
