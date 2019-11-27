/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component.dialog;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-dialog>} is a Web Component for creating customized modal
 * dialogs. The content of the dialog can be populated in two ways: imperatively
 * by using renderer callback function and declaratively by using Polymer's
 * Templates.
 * </p>
 * <h3>Rendering</h3>
 * <p>
 * By default, the dialog uses the content provided by using the renderer
 * callback function.
 * </p>
 * <p>
 * The renderer function provides {@code root}, {@code dialog} arguments.
 * Generate DOM content, append it to the {@code root} element and control the
 * state of the host element by accessing {@code dialog}. Before generating new
 * content, users are able to check if there is already content in {@code root}
 * for reusing it.
 * </p>
 * <p>
 * &lt;vaadin-dialog id=&quot;dialog&quot;&gt;&lt;/vaadin-dialog&gt;
 * {@code const dialog = document.querySelector('#dialog');dialog.renderer =
 * function(root, dialog) root.textContent = &quot;Sample dialog&quot;; };}
 * </p>
 * <p>
 * Renderer is called on the opening of the dialog. DOM generated during the
 * renderer call can be reused in the next renderer call and will be provided
 * with the {@code root} argument. On first call it will be empty.
 * </p>
 * <h3>Polymer Templates</h3>
 * <p>
 * Alternatively, the content can be provided with Polymer's Template. Dialog
 * finds the first child template and uses that in case renderer callback
 * function is not provided. You can also set a custom template using the
 * {@code template} property.
 * </p>
 * <p>
 * &lt;vaadin-dialog opened&gt; &lt;template&gt; Sample dialog &lt;/template&gt;
 * &lt;/vaadin-dialog&gt;
 * </p>
 * <h3>Styling</h3>
 * <p>
 * See <a href=
 * "https://github.com/vaadin/vaadin-overlay/blob/master/src/vaadin-overlay.html"
 * >{@code <vaadin-overlay>} documentation</a> for
 * {@code <vaadin-dialog-overlay>} parts.
 * </p>
 * <p>
 * Note: the {@code theme} attribute value set on {@code <vaadin-dialog>} is
 * propagated to the internal {@code <vaadin-dialog-overlay>} component.
 * </p>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.DialogElement#null", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-dialog")
@HtmlImport("frontend://bower_components/vaadin-dialog/src/vaadin-dialog.html")
public abstract class GeneratedVaadinDialog<R extends GeneratedVaadinDialog<R>>
        extends Component {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Theme to apply to the overlay element
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code theme} property from the webcomponent
     */
    protected String getThemeString() {
        return getElement().getProperty("theme");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Theme to apply to the overlay element
     * </p>
     * 
     * @param theme
     *            the String value to set
     */
    protected void setTheme(String theme) {
        getElement().setProperty("theme", theme == null ? "" : theme);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the overlay is currently displayed.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    protected boolean isOpenedBoolean() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the overlay is currently displayed.
     * </p>
     * 
     * @param opened
     *            the boolean value to set
     */
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set the {@code aria-label} attribute for assistive technologies like
     * screen readers. An {@code undefined} value for this property (the
     * default) means that the {@code aria-label} attribute is not present at
     * all.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code ariaLabel} property from the webcomponent
     */
    protected String getAriaLabelString() {
        return getElement().getProperty("ariaLabel");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set the {@code aria-label} attribute for assistive technologies like
     * screen readers. An {@code undefined} value for this property (the
     * default) means that the {@code aria-label} attribute is not present at
     * all.
     * </p>
     * 
     * @param ariaLabel
     *            the String value to set
     */
    protected void setAriaLabel(String ariaLabel) {
        getElement().setProperty("ariaLabel",
                ariaLabel == null ? "" : ariaLabel);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Manually invoke existing renderer.
     * </p>
     */
    protected void render() {
        getElement().callFunction("render");
    }

    public static class OpenedChangeEvent<R extends GeneratedVaadinDialog<R>>
            extends ComponentEvent<R> {
        private final boolean opened;

        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.opened = source.isOpenedBoolean();
        }

        public boolean isOpened() {
            return opened;
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("opened",
                        event -> listener.onComponentEvent(
                                new OpenedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}