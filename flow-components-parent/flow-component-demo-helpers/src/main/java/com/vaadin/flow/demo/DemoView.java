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
package com.vaadin.flow.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Base class for all the Views that demo some component.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.DIV)
@Theme(Lumo.class)
@StyleSheet("src/css/demo.css")
@StyleSheet("src/css/prism.css")
@JavaScript("src/script/prism.js")
public abstract class DemoView extends Component
        implements HasComponents, HasUrlParameter<String>, HasStyle {
    static final String VARIANT_TOGGLE_BUTTONS_DIV_ID = "variantToggleButtonsDiv";
    static final String COMPONENT_WITH_VARIANTS_ID = "componentWithVariantsDemo";

    private final DemoNavigationBar navBar = new DemoNavigationBar();
    private final Div container = new Div();

    private final Map<String, Div> tabComponents = new HashMap<>();
    private final Map<String, List<SourceCodeExample>> sourceCodeExamples = new HashMap<>();

    protected DemoView() {
        Route annotation = getClass().getAnnotation(Route.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    getClass().getName() + " should be annotated with @"
                            + Route.class.getName() + " to be a valid view");
        }
        addClassName("demo-view");
        navBar.addClassName("demo-nav");
        add(navBar);
        add(container);

        populateSources();
        initView();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (tabComponents.size() <= 1) {
            remove(navBar);
        }
    }

    /**
     * Builds the content of the view.
     */
    protected abstract void initView();

    /**
     * When called the view should populate the given SourceContainer with
     * sample source code to be shown.
     */
    public void populateSources() {
        SourceContentResolver.getSourceCodeExamplesForClass(getClass())
                .forEach(this::putSourceCode);
    }

    private void putSourceCode(SourceCodeExample example) {
        String heading = example.getHeading();
        List<SourceCodeExample> list = sourceCodeExamples
                .computeIfAbsent(heading, key -> new ArrayList<>());
        list.add(example);
    }

    /**
     * Creates and adds a new component card to the "Basic usage" tab in the
     * view. It automatically adds any source code examples with the same
     * heading to the bottom of the card.
     *
     * @param heading
     *            the header text of the card, that is added to the layout. If
     *            <code>null</code> or empty, the header is not added
     *
     * @param components
     *            components to add on creation. If <code>null</code> or empty,
     *            the card is created without the components inside
     * @return created component container card
     * @see #addCard(String, String, Component...)
     */
    public Card addCard(String heading, Component... components) {
        return addCard("Basic usage", "", heading, components);
    }

    /**
     * Creates and adds a new component card to a specific tab in the view. It
     * automatically adds any source code examples with the same heading to the
     * bottom of the card.
     * <p>
     * The href of the tab is defined based on the tab name. For example, a tab
     * named "Advanced usage" has the "advanced-tab" as href (all in lower case
     * and with "-" in place of spaces and special characters).
     *
     * @param tabName
     *            the name of the tab that will contain the demo, not
     *            <code>null</code>
     * @param heading
     *            the header text of the card, that is added to the layout. If
     *            <code>null</code> or empty, the header is not added
     * @param components
     *            components to add on creation. If <code>null</code> or empty,
     *            the card is created without the components inside
     * @return created component container card
     */
    public Card addCard(String tabName, String heading,
            Component... components) {
        String tabUrl = tabName.toLowerCase().replaceAll("[\\W]", "-");
        return addCard(tabName, tabUrl, heading, components);
    }

    private Card addCard(String tabName, String tabUrl, String heading,
            Component... components) {
        Div tab = tabComponents.computeIfAbsent(tabUrl, url -> {
            navBar.addLink(tabName, getTabUrl(tabUrl));
            return new Div();
        });

        if (heading != null && !heading.isEmpty()) {
            tab.add(new H3(heading));
        }

        Card card = new Card();
        if (components != null && components.length > 0) {
            card.add(components);
        }

        List<SourceCodeExample> list = sourceCodeExamples.get(heading);
        if (list != null) {
            list.stream().map(this::createSourceContent).forEach(card::add);
        }

        tab.add(card);
        return card;
    }

    private String getTabUrl(String relativeHref) {
        String href = relativeHref == null || relativeHref.isEmpty() ? ""
                : "/" + relativeHref;
        return getClass().getAnnotation(Route.class).value() + href;
    }

    private SourceContent createSourceContent(
            SourceCodeExample sourceCodeExample) {
        SourceContent content = new SourceContent();
        String sourceString = sourceCodeExample.getSourceCode();
        switch (sourceCodeExample.getSourceType()) {
        case CSS:
            content.addCss(sourceString);
            break;
        case JAVA:
            content.addCode(sourceString);
            break;
        case HTML:
            content.addHtml(sourceString);
            break;
        case UNDEFINED:
        default:
            content.addCode(sourceString);
            break;
        }
        return content;
    }

    private void showTab(String tabUrl) {
        Div tab = tabComponents.get(tabUrl);
        if (tab != null) {
            container.removeAll();
            container.add(tab);
            navBar.setActive(getTabUrl(tabUrl));
            tab.getElement().getNode().runWhenAttached(ui -> ui.getPage()
                    .executeJavaScript("Prism.highlightAll();"));
        }
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        showTab(parameter == null ? "" : parameter);
    }

    /**
     * Adds a demo that shows how the component looks like with specific
     * variants applied.
     *
     * @param componentSupplier
     *            a method that creates the component to which variants will be
     *            applied to
     * @param addVariant
     *            a function that adds the new variant to the component
     * @param removeVariant
     *            a function that removes the variant from the component
     * @param variantToThemeName
     *            function that converts variant to an html theme name
     * @param variants
     *            list of variants to show in the demos
     * @param <T>
     *            variants' type
     * @param <C>
     *            component's type
     */
    protected <T extends Enum<?>, C extends Component & HasTheme> void addVariantsDemo(
            Supplier<C> componentSupplier, BiConsumer<C, T> addVariant,
            BiConsumer<C, T> removeVariant,
            Function<T, String> variantToThemeName, T... variants) {

        C component = componentSupplier.get();
        component.setId(COMPONENT_WITH_VARIANTS_ID);

        Div message = new Div();
        message.setText(
                "Toggle a variant to see how the component's appearance will change.");

        Div variantsToggles = new Div();
        variantsToggles.setId(VARIANT_TOGGLE_BUTTONS_DIV_ID);
        for (T variant : variants) {
            if (variant.name().startsWith("LUMO_")) {
                String variantName = variantToThemeName.apply(variant);
                variantsToggles
                        .add(new NativeButton(
                                getButtonText(variantName,
                                        component.getThemeNames()
                                                .contains(variantName)),
                                event -> {
                                    boolean variantPresent = component
                                            .getThemeNames()
                                            .contains(variantName);
                                    if (variantPresent) {
                                        removeVariant.accept(component,
                                                variant);
                                    } else {
                                        addVariant.accept(component, variant);
                                    }
                                    event.getSource().setText(getButtonText(
                                            variantName, !variantPresent));
                                }));

            }
        }
        addCard("Theme variants usage", message, component, variantsToggles);
    }

    private String getButtonText(String variantName, boolean variantPresent) {
        return String.format(
                variantPresent ? "Remove '%s' variant" : "Add '%s' variant",
                variantName);
    }
}
