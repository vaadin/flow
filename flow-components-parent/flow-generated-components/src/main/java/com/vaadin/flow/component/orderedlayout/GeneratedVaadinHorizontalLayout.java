/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.orderedlayout;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-horizontal-layout>} provides a simple way to horizontally
 * align your HTML elements.
 * </p>
 * <p>{@code <vaadin-horizontal-layout>
 * </p>
 * <div>Item 1</div>
  <div>Item 2</div>
</vaadin-horizontal-layout>} <h3>Built-in Theme Variations</h3>
 * <p>
 * {@code <vaadin-horizontal-layout>} supports the following theme variations:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Theme variation</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code theme=&quot;margin&quot;}</td>
 * <td>Applies the default amount of CSS margin for the host element (specified
 * by the theme)</td>
 * </tr>
 * <tr>
 * <td>{@code theme=&quot;padding&quot;}</td>
 * <td>Applies the default amount of CSS padding for the host element (specified
 * by the theme)</td>
 * </tr>
 * <tr>
 * <td>{@code theme=&quot;spacing&quot;}</td>
 * <td>Applies the default amount of CSS margin between items (specified by the
 * theme)</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.HorizontalLayoutElement#1.1.0",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-horizontal-layout")
@HtmlImport("frontend://bower_components/vaadin-ordered-layout/src/vaadin-horizontal-layout.html")
public abstract class GeneratedVaadinHorizontalLayout<R extends GeneratedVaadinHorizontalLayout<R>>
        extends Component implements HasStyle, HasTheme {

    /**
     * Adds theme variants to the component.
     * 
     * @param variants
     *            theme variants to add
     */
    public void addThemeVariants(HorizontalLayoutVariant... variants) {
        getThemeNames().addAll(
                Stream.of(variants).map(HorizontalLayoutVariant::getVariantName)
                        .collect(Collectors.toList()));
    }

    /**
     * Removes theme variants from the component.
     * 
     * @param variants
     *            theme variants to remove
     */
    public void removeThemeVariants(HorizontalLayoutVariant... variants) {
        getThemeNames().removeAll(
                Stream.of(variants).map(HorizontalLayoutVariant::getVariantName)
                        .collect(Collectors.toList()));
    }
}