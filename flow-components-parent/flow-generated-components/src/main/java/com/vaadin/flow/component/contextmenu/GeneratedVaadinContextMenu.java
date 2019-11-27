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
package com.vaadin.flow.component.contextmenu;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-context-menu>} is a Web Component for creating context menus.
 * The content of the menu can be populated in two ways: imperatively by using
 * renderer callback function and declaratively by using Polymer's Templates.
 * </p>
 * <h3>Rendering</h3>
 * <p>
 * By default, the context menu uses the content provided using the renderer
 * callback function.
 * </p>
 * <p>
 * The renderer function provides {@code root}, {@code contextMenu},
 * {@code model} arguments when applicable. Generate DOM content by using
 * {@code model} object properties if needed, append it to the {@code root}
 * element and control the state of the host element by accessing
 * {@code contextMenu}. Before generating new content, users are able to check
 * if there is already content in {@code root} for reusing it.
 * </p>
 * <p>
 * &lt;vaadin-context-menu id=&quot;contextMenu&quot;&gt; &lt;p&gt;This
 * paragraph has a context menu.&lt;/p&gt; &lt;/vaadin-context-menu&gt;
 * {@code const contextMenu = document.querySelector('#contextMenu');
 * contextMenu.renderer = (root, contextMenu, context) =&gt; let listBox =
 * root.firstElementChild; if (!listBox) { listBox =
 * document.createElement('vaadin-list-box'); root.appendChild(listBox); }
 * </p>
 * <p>
 * let item = listBox.querySelector('vaadin-item'); if (!item) { item =
 * document.createElement('vaadin-item'); listBox.appendChild(item); }
 * item.textContent = 'Content of the selector: ' + context.target.textContent;
 * };}
 * </p>
 * <p>
 * You can access the menu context inside the renderer using
 * {@code context.target} and {@code context.detail}.
 * </p>
 * <p>
 * Renderer is called on the opening of the context-menu and each time the
 * related context is updated. DOM generated during the renderer call can be
 * reused in the next renderer call and will be provided with the {@code root}
 * argument. On first call it will be empty.
 * </p>
 * <p>
 * <strong>NOTE:</strong> when the {@code renderer} function is defined, the
 * template content is not in use.
 * </p>
 * <h3>Polymer Templates</h3>
 * <p>
 * Alternatively to using the {@code renderer}, you are able to populate the
 * menu content using Polymer's Templates:
 * </p>
 * <p>
 * &lt;vaadin-context-menu&gt; &lt;template&gt; &lt;vaadin-list-box&gt;
 * &lt;vaadin-item&gt;First menu item&lt;/vaadin-item&gt;
 * &lt;vaadin-item&gt;Second menu item&lt;/vaadin-item&gt;
 * &lt;/vaadin-list-box&gt; &lt;/template&gt; &lt;/vaadin-context-menu&gt;
 * </p>
 * <h3>“vaadin-contextmenu” Gesture Event</h3>
 * <p>
 * {@code vaadin-contextmenu} is a gesture event (a custom event fired by
 * Polymer), which is dispatched after either {@code contextmenu} and long touch
 * events. This enables support for both mouse and touch environments in a
 * uniform way.
 * </p>
 * <p>
 * {@code <vaadin-context-menu>} opens the menu overlay on the
 * {@code vaadin-contextmenu} event by default.
 * </p>
 * <h3>Menu Listener</h3>
 * <p>
 * By default, the {@code <vaadin-context-menu>} element listens for the menu
 * opening event on itself. In order to have a context menu on your content,
 * wrap your content with the {@code <vaadin-context-menu>} element, and add a
 * template element with a menu. Example:
 * </p>
 * <p>
 * &lt;vaadin-context-menu&gt; &lt;template&gt; &lt;vaadin-list-box&gt;
 * &lt;vaadin-item&gt;First menu item&lt;/vaadin-item&gt;
 * &lt;vaadin-item&gt;Second menu item&lt;/vaadin-item&gt;
 * &lt;/vaadin-list-box&gt; &lt;/template&gt;
 * </p>
 * <p>
 * &lt;p&gt;This paragraph has the context menu provided in the above
 * template.&lt;/p&gt; &lt;p&gt;Another paragraph with the context
 * menu.&lt;/p&gt; &lt;/vaadin-context-menu&gt;
 * </p>
 * <p>
 * In case if you do not want to wrap the page content, you can listen for
 * events on an element outside the {@code <vaadin-context-menu>} by setting the
 * {@code listenOn} property:
 * </p>
 * <p>
 * &lt;vaadin-context-menu id=&quot;customListener&quot;&gt; &lt;template&gt;
 * &lt;vaadin-list-box&gt; ... &lt;/vaadin-list-box&gt; &lt;/template&gt;
 * &lt;/vaadin-context-menu&gt;
 * </p>
 * <p>
 * &lt;div id=&quot;menuListener&quot;&gt;The element that listens for the
 * context menu.&lt;/div&gt;
 * </p>
 * <p>
 * &amp;lt;script&amp;gt; const contextMenu =
 * document.querySelector('vaadin-context-menu#customListener');
 * contextMenu.listenOn = document.querySelector('#menuListener');
 * &amp;lt;/script&amp;gt;
 * </p>
 * <h3>Filtering Menu Targets</h3>
 * <p>
 * By default, the listener element and all its descendants open the context
 * menu. You can filter the menu targets to a smaller set of elements inside the
 * listener element by setting the {@code selector} property.
 * </p>
 * <p>
 * In the following example, only the elements matching {@code .has-menu} will
 * open the context menu:
 * </p>
 * <p>
 * &lt;vaadin-context-menu selector=&quot;.has-menu&quot;&gt; &lt;template&gt;
 * &lt;vaadin-list-box&gt; ... &lt;/vaadin-list-box&gt; &lt;/template&gt;
 * </p>
 * <p>
 * &lt;p class=&quot;has-menu&quot;&gt;This paragraph opens the context
 * menu&lt;/p&gt; &lt;p&gt;This paragraph does not open the context
 * menu&lt;/p&gt; &lt;/vaadin-context-menu&gt;
 * </p>
 * <h3>Menu Context</h3>
 * <p>
 * You can bind to the following properties in the menu template:
 * </p>
 * <ul>
 * <li>{@code target} is the menu opening event target, which is the element
 * that the user has called the context menu for</li>
 * <li>{@code detail} is the menu opening event detail</li>
 * </ul>
 * <p>
 * In the following example, the menu item text is composed with the contents of
 * the element that opened the menu:
 * </p>
 * <p>
 * &lt;vaadin-context-menu selector=&quot;li&quot;&gt; &lt;template&gt;
 * &lt;vaadin-list-box&gt; &lt;vaadin-item&gt;The menu target:
 * [[target.textContent]]&lt;/vaadin-item&gt; &lt;/vaadin-list-box&gt;
 * &lt;/template&gt;
 * </p>
 * <p>
 * &lt;ul&gt; &lt;li&gt;Foo&lt;/li&gt; &lt;li&gt;Bar&lt;/li&gt;
 * &lt;li&gt;Baz&lt;/li&gt; &lt;/ul&gt; &lt;/vaadin-context-menu&gt;
 * </p>
 * <h3>Styling</h3>
 * <p>
 * {@code <vaadin-context-menu>} uses {@code <vaadin-context-menu-overlay>}
 * internal themable component as the actual visible context menu overlay. See
 * See <a href=
 * "https://github.com/vaadin/vaadin-overlay/blob/master/src/vaadin-overlay.html"
 * >{@code <vaadin-overlay>} documentation</a> for
 * {@code <vaadin-context-menu-overlay>} parts.
 * </p>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 * <p>
 * Note: the {@code theme} attribute value set on {@code <vaadin-context-menu>}
 * is propagated to the internal {@code <vaadin-context-menu-overlay>}
 * component.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.ContextMenuElement#4.2.0", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-context-menu")
@HtmlImport("frontend://bower_components/vaadin-context-menu/src/vaadin-context-menu.html")
public abstract class GeneratedVaadinContextMenu<R extends GeneratedVaadinContextMenu<R>>
        extends Component implements HasStyle, ClickNotifier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * CSS selector that can be used to target any child element of the context
     * menu to listen for {@code openOn} events.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selector} property from the webcomponent
     */
    protected String getSelectorString() {
        return getElement().getProperty("selector");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * CSS selector that can be used to target any child element of the context
     * menu to listen for {@code openOn} events.
     * </p>
     * 
     * @param selector
     *            the String value to set
     */
    protected void setSelector(String selector) {
        getElement().setProperty("selector", selector == null ? "" : selector);
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
     * Event name to listen for opening the context menu.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code openOn} property from the webcomponent
     */
    protected String getOpenOnString() {
        return getElement().getProperty("openOn");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Event name to listen for opening the context menu.
     * </p>
     * 
     * @param openOn
     *            the String value to set
     */
    protected void setOpenOn(String openOn) {
        getElement().setProperty("openOn", openOn == null ? "" : openOn);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The target element that's listened to for context menu opening events. By
     * default the vaadin-context-menu listens to the target's
     * {@code vaadin-contextmenu} events.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code listenOn} property from the webcomponent
     */
    protected JsonObject getListenOnJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("listenOn");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The target element that's listened to for context menu opening events. By
     * default the vaadin-context-menu listens to the target's
     * {@code vaadin-contextmenu} events.
     * </p>
     * 
     * @param listenOn
     *            the JsonObject value to set
     */
    protected void setListenOn(JsonObject listenOn) {
        getElement().setPropertyJson("listenOn", listenOn);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Event name to listen for closing the context menu.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code closeOn} property from the webcomponent
     */
    protected String getCloseOnString() {
        return getElement().getProperty("closeOn");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Event name to listen for closing the context menu.
     * </p>
     * 
     * @param closeOn
     *            the String value to set
     */
    protected void setCloseOn(String closeOn) {
        getElement().setProperty("closeOn", closeOn == null ? "" : closeOn);
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

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Closes the overlay.
     * </p>
     */
    protected void close() {
        getElement().callFunction("close");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Opens the overlay.
     * </p>
     */
    protected void open() {
        getElement().callFunction("open");
    }

    public static class OpenedChangeEvent<R extends GeneratedVaadinContextMenu<R>>
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