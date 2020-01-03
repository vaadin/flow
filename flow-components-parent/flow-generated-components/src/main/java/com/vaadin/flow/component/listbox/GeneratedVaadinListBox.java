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
package com.vaadin.flow.component.listbox;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-list-box>} is a Web Component for creating menus.
 * </p>
 * <p>
 * {@code
<vaadin-list-box selected="2">
<vaadin-item>Item 1</vaadin-item>
<vaadin-item>Item 2</vaadin-item>
<vaadin-item>Item 3</vaadin-item>
<vaadin-item>Item 4</vaadin-item>
</vaadin-list-box>}
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code items}</td>
 * <td>The items container</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.ListBoxElement#1.1.0", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-list-box")
@HtmlImport("frontend://bower_components/vaadin-list-box/src/vaadin-list-box.html")
public abstract class GeneratedVaadinListBox<R extends GeneratedVaadinListBox<R>>
        extends Component implements HasStyle {

    protected void focus() {
        getElement().callFunction("focus");
    }
}