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
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsCheckboxPropertyEditorElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsComponentPickerElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsThemeClassNameEditorElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsThemeColorPropertyEditorElement;
import com.vaadin.flow.uitest.ui.testbench.DevToolsThemeRangePropertyEditorElement;
import com.vaadin.testbench.TestBenchElement;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NotThreadSafe
public class ThemeEditorIT extends AbstractThemeEditorIT {
    private static String javaContent = "";

    @Override
    protected String getTestPath() {
        return "/context/view/com.vaadin.flow.uitest.ui.ThemeEditorView";
    }

    @BeforeClass
    public static void initialize() {
        try {
            ComponentTracker.Location location = new ComponentTracker.Location(
                    "com.vaadin.flow.uitest.ui.ThemeEditorView",
                    "ThemeEditorView.java", "ThemeEditorView", 1);
            File javaFile = location
                    .findJavaFile(new TestAbstractConfiguration());
            javaContent = Files.readString(javaFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() {
        try {
            ComponentTracker.Location location = new ComponentTracker.Location(
                    "com.vaadin.flow.uitest.ui.ThemeEditorView",
                    "ThemeEditorView.java", "ThemeEditorView", 1);
            File javaFile = location
                    .findJavaFile(new TestAbstractConfiguration());
            try (FileWriter fw = new FileWriter(javaFile)) {
                fw.write(javaContent);
            }
            File styleSheetFile = getStyleSheetFile();
            if (styleSheetFile.exists()) {
                styleSheetFile.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getStyleSheetFile() {
        TestThemeModifier themeModifier = new TestThemeModifier();
        return themeModifier.getStyleSheetFileWithoutSideEffects();
    }

    @Test
    public void testButton() throws IOException {
        performTagTest("vaadin-button", By.id("button"));
    }

    @Test
    public void testIcon() throws IOException {
        performTagTest("vaadin-icon", By.id("icon"));
    }

    @Test
    public void testCheckbox() throws IOException {
        performTagTest("vaadin-checkbox", By.id("checkbox"));
    }

    @Test
    public void testVaadinAccordion() throws IOException {
        performTagTestUsingComponentPicker("vaadin-accordion",
                By.id("accordion"));
    }

    private void performTagTest(String tagName, By componentSelector)
            throws IOException {
        performTagTest(tagName, componentSelector, false);
    }

    private void performTagTestUsingComponentPicker(String tagName,
            By componentSelector) throws IOException {
        performTagTest(tagName, componentSelector, true);
    }

    private void performTagTest(String tagName, By componentSelector,
            boolean useComponentPicker) throws IOException {
        List<Metadata> allMetadata = extractMetadata(tagName);

        open();

        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();
        devTools.expand();

        devTools.showThemeEditor();

        TestBenchElement themeEditor = devTools
                .$("vaadin-dev-tools-theme-editor").first();
        themeEditor.$("button").first().click();

        if (useComponentPicker) {
            selectComponentUsingComponentPicker(devTools, tagName,
                    componentSelector);
        } else {
            new Actions(getDriver()).click(findElement(componentSelector))
                    .perform();
        }

        DevToolsThemeClassNameEditorElement classNameEditor = themeEditor
                .$(DevToolsThemeClassNameEditorElement.class).waitForFirst();
        String cssClassName = classNameEditor.getValue();

        TestBenchElement themePropertiesList = themeEditor
                .$("vaadin-dev-tools" + "-theme-property-list").waitForFirst();

        List<TestBenchElement> sections = themePropertiesList.$("*")
                .attribute("class", "section").all();
        for (TestBenchElement section : sections) {
            String sectionName = section.$("span").first().getText().strip();
            Optional<Metadata> oMetadata = allMetadata.stream()
                    .filter(m -> m.getDisplayName().strip().equals(sectionName))
                    .findFirst();
            Assert.assertTrue(String.format(
                    "Couldn't find a metadata for the section named '%s'",
                    sectionName), oMetadata.isPresent());
            Metadata metadata = oMetadata.get();

            List<TestBenchElement> propertyEditors = section.$("*")
                    .hasAttribute("data-testid").all();

            List<String> dataTestIds = new ArrayList<>(propertyEditors.size());
            for (TestBenchElement testBenchElement : propertyEditors) {
                WebElement webElement = testBenchElement.getWrappedElement();
                String dataTestIdAttribute = webElement
                        .getAttribute("data-testid");
                dataTestIds.add(dataTestIdAttribute);
            }

            String cssSelector = createSelector(metadata, tagName,
                    cssClassName);
            Map<String, String> expectedCssProperties = new HashMap<>();
            for (Property property : metadata.getProperties()) {
                Assert.assertTrue(String.format(
                        "The property '%s' doesn't appear in section '%s' from the dev tools window. Available section properties: %s",
                        property.getPropertyName(), sectionName,
                        dataTestIds.stream().collect(Collectors.joining(", "))),
                        dataTestIds.contains(property.getPropertyName()));

                TestBenchElement sectionPropertyList = section.$("div")
                        .attribute("class", "property-list").first();

                Instant baseTime = Instant.now();
                String value = updatePropertyInDevTools(sectionPropertyList,
                        property);

                expectedCssProperties.put(property.getPropertyName(), value);
                // ouch...
                if (property.getPropertyName().equals("border-width")
                        && !value.equals("0px")) {
                    expectedCssProperties.put("border-style", "solid");
                }

                waitForStyleSheetFileToChange(baseTime);

                checkCssSelectorProperties(cssSelector, expectedCssProperties);
            }
        }
    }

    private void selectComponentUsingComponentPicker(DevToolsElement devTools,
            String tagName, By componentSelector) {
        DevToolsComponentPickerElement componentPicker = devTools
                .$(DevToolsComponentPickerElement.class).waitForFirst();

        WebElement target = findElement(componentSelector);

        componentPicker.moveToElement(target);
        List<String> options = componentPicker.getOptions();
        Assert.assertTrue(String.format(
                "'%s' isn't in the list of options. Available options: %s",
                tagName, options.stream().collect(Collectors.joining(", "))),
                options.contains(tagName));

        // let's just test if we can select an option by using the dialog menu
        Optional<String> oOption = options.stream()
                .filter(o -> !o.equals(tagName)).findAny();
        if (oOption.isPresent()) {
            String option = oOption.get();
            componentPicker.moveToOption(option);
            Assert.assertEquals(option, componentPicker.getSelectedOption());
        }

        componentPicker.selectOption(tagName);
    }

    private Optional<Instant> getLastModifiedTime(File file) {
        try {
            if (!file.exists()) {
                return Optional.empty();
            }
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class);
            return Optional.of(attributes.lastModifiedTime().toInstant());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForStyleSheetFileToChange(Instant baseTime) {
        new FluentWait<File>(getStyleSheetFile())
                .withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(500)).until(f -> {
                    Optional<Instant> oLastModifiedTime = getLastModifiedTime(
                            f);
                    if (oLastModifiedTime.isEmpty()) {
                        return false;
                    } else {
                        Instant lastModifiedTime = oLastModifiedTime.get();
                        return lastModifiedTime.isAfter(baseTime);
                    }
                });
    }

    private void checkCssSelectorProperties(String cssSelector,
            Map<String, String> expectedCssProperties) {
        CssRule expected = new CssRule(cssSelector, expectedCssProperties);
        CssRule actual = waitForCssRuleToAppearInStyleFile(cssSelector);
        Assert.assertEquals(expected, actual);
    }

    private CssRule waitForCssRuleToAppearInStyleFile(String cssSelector) {
        new FluentWait<String>(cssSelector).withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(500)).until(f -> {
                    return getCssRuleFromFile(cssSelector).isPresent();
                });
        return getCssRuleFromFile(cssSelector).get();
    }

    private String createSelector(Metadata metadata, String tagName,
            String cssClassName) {
        return metadata.getSelector().replaceFirst(tagName,
                String.format("%s.%s", tagName, cssClassName));
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

    private String updatePropertyInDevTools(TestBenchElement container,
            Property property) {
        TestBenchElement element = container.$("*")
                .attribute("data-testid", property.getPropertyName()).first();
        String result = switch (property.getEditorType()) {
        case "color" -> {
            DevToolsThemeColorPropertyEditorElement colorEditor = element
                    .wrap(DevToolsThemeColorPropertyEditorElement.class);
            String value = "rgba(116, 219, 0, 1)";
            colorEditor.setValue(value);
            waitForModifiedLabel(colorEditor);
            yield value;
        }
        case "checkbox" -> {
            DevToolsCheckboxPropertyEditorElement checkboxEditor = element
                    .wrap(DevToolsCheckboxPropertyEditorElement.class);
            checkboxEditor.setChecked(true);
            waitForModifiedLabel(checkboxEditor);
            String value = checkboxEditor.getCheckedValue();
            yield value;
        }
        case "range" -> {
            DevToolsThemeRangePropertyEditorElement rangeEditor = element
                    .wrap(DevToolsThemeRangePropertyEditorElement.class);
            String value = "3px";
            rangeEditor.setValue(value);
            waitForModifiedLabel(rangeEditor);
            yield value;
        }
        default -> {
            throw new RuntimeException(String.format(
                    "The property '%s' is associated with a unknown editor: '%s'",
                    property.getPropertyName(), property.getEditorType()));
        }
        };
        return result;
    }

    private void waitForModifiedLabel(TestBenchElement element) {
        element.$("span").attribute("class", "modified").waitForFirst();
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
