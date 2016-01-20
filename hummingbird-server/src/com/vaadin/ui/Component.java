/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import java.io.Serializable;
import java.util.Locale;

import com.vaadin.event.FieldEvents;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.Sizeable;

/**
 * {@code Component} is the top-level interface that is and must be implemented
 * by all Vaadin components. {@code Component} is paired with
 * {@link AbstractComponent}, which provides a default implementation for all
 * the methods defined in this interface.
 * 
 * <p>
 * Components are laid out in the user interface hierarchically. The layout is
 * managed by layout components, or more generally by components that implement
 * the {@link ComponentContainer} interface. Such a container is the
 * <i>parent</i> of the contained components.
 * </p>
 * 
 * <p>
 * The {@link #getParent()} method allows retrieving the parent component of a
 * component. While there is a {@link #setParent(Component) setParent()}, you
 * rarely need it as you normally add components with the
 * {@link ComponentContainer#addComponent(Component) addComponent()} method of
 * the layout or other {@code ComponentContainer}, which automatically sets the
 * parent.
 * </p>
 * 
 * <p>
 * A component becomes <i>attached</i> to an application (and the
 * {@link #attach()} is called) when it or one of its parents is attached to the
 * main window of the application through its containment hierarchy.
 * </p>
 * 
 * @author Vaadin Ltd.
 * @since 3.0
 */
public interface Component extends ClientConnector, Sizeable, Serializable {

    /**
     * Gets all user-defined CSS style names of a component. If the component
     * has multiple style names defined, the return string is a space-separated
     * list of style names. Built-in style names defined in Vaadin or GWT are
     * not returned.
     * 
     * <p>
     * The style names are returned only in the basic form in which they were
     * added; each user-defined style name shows as two CSS style class names in
     * the rendered HTML: one as it was given and one prefixed with the
     * component-specific style name. Only the former is returned.
     * </p>
     * 
     * @return the style name or a space-separated list of user-defined style
     *         names of the component
     * @see #setStyleName(String)
     * @see #addStyleName(String)
     * @see #removeStyleName(String)
     */
    public String getStyleName();

    /**
     * Sets one or more user-defined style names of the component, replacing any
     * previous user-defined styles. Multiple styles can be specified as a
     * space-separated list of style names. The style names must be valid CSS
     * class names and should not conflict with any built-in style names in
     * Vaadin or GWT.
     * 
     * <pre>
     * Label label = new Label(&quot;This text has a lot of style&quot;);
     * label.setStyleName(&quot;myonestyle myotherstyle&quot;);
     * </pre>
     * 
     * <p>
     * Each style name will occur in two versions: one as specified and one that
     * is prefixed with the style name of the component. For example, if you
     * have a {@code Button} component and give it "{@code mystyle}" style, the
     * component will have both "{@code mystyle}" and "{@code v-button-mystyle}"
     * styles. You could then style the component either with:
     * </p>
     * 
     * <pre>
     * .myonestyle {background: blue;}
     * </pre>
     * 
     * <p>
     * or
     * </p>
     * 
     * <pre>
     * .v-button-myonestyle {background: blue;}
     * </pre>
     * 
     * <p>
     * It is normally a good practice to use {@link #addStyleName(String)
     * addStyleName()} rather than this setter, as different software
     * abstraction layers can then add their own styles without accidentally
     * removing those defined in other layers.
     * </p>
     * 
     * <p>
     * This method will trigger a {@link RepaintRequestEvent}.
     * </p>
     * 
     * @param style
     *            the new style or styles of the component as a space-separated
     *            list
     * @see #getStyleName()
     * @see #addStyleName(String)
     * @see #removeStyleName(String)
     */
    public void setStyleName(String style);

    /**
     * Adds one or more style names to this component. Multiple styles can be
     * specified as a space-separated list of style names. The style name will
     * be rendered as a HTML class name, which can be used in a CSS definition.
     * 
     * <pre>
     * Label label = new Label(&quot;This text has style&quot;);
     * label.addStyleName(&quot;mystyle&quot;);
     * </pre>
     * 
     * <p>
     * Each style name will occur in two versions: one as specified and one that
     * is prefixed with the style name of the component. For example, if you
     * have a {@code Button} component and give it "{@code mystyle}" style, the
     * component will have both "{@code mystyle}" and "{@code v-button-mystyle}"
     * styles. You could then style the component either with:
     * </p>
     * 
     * <pre>
     * .mystyle {font-style: italic;}
     * </pre>
     * 
     * <p>
     * or
     * </p>
     * 
     * <pre>
     * .v-button-mystyle {font-style: italic;}
     * </pre>
     * 
     * <p>
     * This method will trigger a {@link RepaintRequestEvent}.
     * </p>
     * 
     * @param style
     *            the new style to be added to the component
     * @see #getStyleName()
     * @see #setStyleName(String)
     * @see #removeStyleName(String)
     */
    public void addStyleName(String style);

    /**
     * Removes one or more style names from component. Multiple styles can be
     * specified as a space-separated list of style names.
     * 
     * <p>
     * The parameter must be a valid CSS style name. Only user-defined style
     * names added with {@link #addStyleName(String) addStyleName()} or
     * {@link #setStyleName(String) setStyleName()} can be removed; built-in
     * style names defined in Vaadin or GWT can not be removed.
     * </p>
     * 
     * * This method will trigger a {@link RepaintRequestEvent}.
     * 
     * @param style
     *            the style name or style names to be removed
     * @see #getStyleName()
     * @see #setStyleName(String)
     * @see #addStyleName(String)
     */
    public void removeStyleName(String style);

    /**
     * Tests whether the component is enabled or not. A user can not interact
     * with disabled components. Disabled components are rendered in a style
     * that indicates the status, usually in gray color. Children of a disabled
     * component are also disabled. Components are enabled by default.
     * 
     * <p>
     * As a security feature, all updates for disabled components are blocked on
     * the server-side.
     * </p>
     * 
     * <p>
     * Note that this method only returns the status of the component and does
     * not take parents into account. Even though this method returns true the
     * component can be disabled to the user if a parent is disabled.
     * </p>
     * 
     * @return <code>true</code> if the component and its parent are enabled,
     *         <code>false</code> otherwise.
     * @see VariableOwner#isEnabled()
     */
    public boolean isEnabled();

    /**
     * Enables or disables the component. The user can not interact with
     * disabled components, which are shown with a style that indicates the
     * status, usually shaded in light gray color. Components are enabled by
     * default.
     * 
     * <pre>
     * Button enabled = new Button(&quot;Enabled&quot;);
     * enabled.setEnabled(true); // The default
     * layout.addComponent(enabled);
     * 
     * Button disabled = new Button(&quot;Disabled&quot;);
     * disabled.setEnabled(false);
     * layout.addComponent(disabled);
     * </pre>
     * 
     * <p>
     * This method will trigger a {@link RepaintRequestEvent} for the component
     * and, if it is a {@link ComponentContainer}, for all its children
     * recursively.
     * </p>
     * 
     * @param enabled
     *            a boolean value specifying if the component should be enabled
     *            or not
     */
    public void setEnabled(boolean enabled);

    /**
     * Tests the <i>visibility</i> property of the component.
     * 
     * <p>
     * Visible components are drawn in the user interface, while invisible ones
     * are not. The effect is not merely a cosmetic CSS change - no information
     * about an invisible component will be sent to the client. The effect is
     * thus the same as removing the component from its parent. Making a
     * component invisible through this property can alter the positioning of
     * other components.
     * </p>
     * 
     * <p>
     * A component is visible only if all its parents are also visible. This is
     * not checked by this method though, so even if this method returns true,
     * the component can be hidden from the user because a parent is set to
     * invisible.
     * </p>
     * 
     * @return <code>true</code> if the component has been set to be visible in
     *         the user interface, <code>false</code> if not
     * @see #setVisible(boolean)
     * @see #attach()
     */
    public boolean isVisible();

    /**
     * Sets the visibility of the component.
     * 
     * <p>
     * Visible components are drawn in the user interface, while invisible ones
     * are not. The effect is not merely a cosmetic CSS change - no information
     * about an invisible component will be sent to the client. The effect is
     * thus the same as removing the component from its parent.
     * </p>
     * 
     * <pre>
     * TextField readonly = new TextField(&quot;Read-Only&quot;);
     * readonly.setValue(&quot;You can't see this!&quot;);
     * readonly.setVisible(false);
     * layout.addComponent(readonly);
     * </pre>
     * 
     * <p>
     * A component is visible only if all of its parents are also visible. If a
     * component is explicitly set to be invisible, changes in the visibility of
     * its parents will not change the visibility of the component.
     * </p>
     * 
     * @param visible
     *            the boolean value specifying if the component should be
     *            visible after the call or not.
     * @see #isVisible()
     */
    public void setVisible(boolean visible);

    /**
     * Sets the parent connector of the component.
     * 
     * <p>
     * This method automatically calls {@link #attach()} if the component
     * becomes attached to the session, regardless of whether it was attached
     * previously. Conversely, if the component currently is attached to the
     * session, {@link #detach()} is called for the connector before attaching
     * it to a new parent.
     * </p>
     * <p>
     * This method is rarely called directly.
     * {@link ComponentContainer#addComponent(Component)} or a
     * {@link HasComponents} specific method is normally used for adding
     * components to a parent and the used method will call this method
     * implicitly.
     * </p>
     * 
     * @param parent
     *            the parent connector
     * @throws IllegalStateException
     *             if a parent is given even though the connector already has a
     *             parent
     */
    public void setParent(HasComponents parent);

    /**
     * Gets the parent component of the component.
     * 
     * <p>
     * Components can be nested but a component can have only one parent. A
     * component that contains other components, that is, can be a parent,
     * should usually inherit the {@link ComponentContainer} interface.
     * </p>
     * 
     * @return the parent component
     */
    @Override
    public HasComponents getParent();

    /**
     * Gets the UI the component is attached to.
     * 
     * <p>
     * If the component is not attached to a UI through a component containment
     * hierarchy, <code>null</code> is returned.
     * </p>
     * 
     * @return the UI of the component or <code>null</code> if it is not
     *         attached to a UI
     */
    @Override
    public UI getUI();

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Reimplementing the {@code attach()} method is useful for tasks that need
     * to get a reference to the parent, window, or application object with the
     * {@link #getParent()}, {@link #getUI()}, and {@link #getSession()}
     * methods. A component does not yet know these objects in the constructor,
     * so in such case, the methods will return {@code null}. For example, the
     * following is invalid:
     * </p>
     * 
     * <pre>
     * public class AttachExample extends CustomComponent {
     *     public AttachExample() {
     *         // ERROR: We can't access the application object yet.
     *         ClassResource r = new ClassResource(&quot;smiley.jpg&quot;,
     *                 getApplication());
     *         Embedded image = new Embedded(&quot;Image:&quot;, r);
     *         setCompositionRoot(image);
     *     }
     * }
     * </pre>
     * 
     * <p>
     * Adding a component to an application triggers calling the
     * {@link #attach()} method for the component. Correspondingly, removing a
     * component from a container triggers calling the {@link #detach()} method.
     * If the parent of an added component is already connected to the
     * application, the {@code attach()} is called immediately from
     * {@link #setParent(Component)}.
     * </p>
     * 
     * <pre>
     * public class AttachExample extends CustomComponent {
     *     public AttachExample() {
     *     }
     * 
     *     &#064;Override
     *     public void attach() {
     *         super.attach(); // Must call.
     * 
     *         // Now we know who ultimately owns us.
     *         ClassResource r = new ClassResource(&quot;smiley.jpg&quot;,
     *                 getApplication());
     *         Embedded image = new Embedded(&quot;Image:&quot;, r);
     *         setCompositionRoot(image);
     *     }
     * }
     * </pre>
     */
    @Override
    public void attach();

    /**
     * Gets the locale of the component.
     * 
     * <p>
     * If a component does not have a locale set, the locale of its parent is
     * returned, and so on. Eventually, if no parent has locale set, the locale
     * of the application is returned. If the application does not have a locale
     * set, it is determined by <code>Locale.getDefault()</code>.
     * </p>
     * 
     * <p>
     * As the component must be attached before its locale can be acquired,
     * using this method in the internationalization of component captions, etc.
     * is generally not feasible. For such use case, we recommend using an
     * otherwise acquired reference to the application locale.
     * </p>
     * 
     * @return Locale of this component or {@code null} if the component and
     *         none of its parents has a locale set and the component is not yet
     *         attached to an application.
     */
    public Locale getLocale();

    /**
     * Adds an unique id for component that is used in the client-side for
     * testing purposes. Keeping identifiers unique is the responsibility of the
     * programmer.
     * 
     * @param id
     *            An alphanumeric id
     */
    public void setId(String id);

    /**
     * Gets currently set debug identifier
     * 
     * @return current id, null if not set
     */
    public String getId();

    /* Component event framework */

    /**
     * A sub-interface implemented by components that can obtain input focus.
     * This includes all {@link Field} components as well as some other
     * components, such as {@link Upload}.
     * 
     * <p>
     * Focus can be set with {@link #focus()}. This interface does not provide
     * an accessor that would allow finding out the currently focused component;
     * focus information can be acquired for some (but not all) {@link Field}
     * components through the {@link com.vaadin.event.FieldEvents.FocusListener}
     * and {@link com.vaadin.event.FieldEvents.BlurListener} interfaces.
     * </p>
     * 
     * @see FieldEvents
     */
    public interface Focusable extends Component {

        /**
         * Sets the focus to this component.
         * 
         * <pre>
         * Form loginBox = new Form();
         * loginBox.setCaption(&quot;Login&quot;);
         * layout.addComponent(loginBox);
         * 
         * // Create the first field which will be focused
         * TextField username = new TextField(&quot;User name&quot;);
         * loginBox.addField(&quot;username&quot;, username);
         * 
         * // Set focus to the user name
         * username.focus();
         * 
         * TextField password = new TextField(&quot;Password&quot;);
         * loginBox.addField(&quot;password&quot;, password);
         * 
         * Button login = new Button(&quot;Login&quot;);
         * loginBox.getFooter().addComponent(login);
         * </pre>
         * 
         * <p>
         * Notice that this interface does not provide an accessor that would
         * allow finding out the currently focused component. Focus information
         * can be acquired for some (but not all) {@link Field} components
         * through the {@link com.vaadin.event.FieldEvents.FocusListener} and
         * {@link com.vaadin.event.FieldEvents.BlurListener} interfaces.
         * </p>
         * 
         * @see com.vaadin.event.FieldEvents
         * @see com.vaadin.event.FieldEvents.FocusEvent
         * @see com.vaadin.event.FieldEvents.FocusListener
         * @see com.vaadin.event.FieldEvents.BlurEvent
         * @see com.vaadin.event.FieldEvents.BlurListener
         */
        public void focus();

        /**
         * Gets the <i>tabulator index</i> of the {@code Focusable} component.
         * 
         * @return tab index set for the {@code Focusable} component
         * @see #setTabIndex(int)
         */
        public int getTabIndex();

        /**
         * Sets the <i>tabulator index</i> of the {@code Focusable} component.
         * The tab index property is used to specify the order in which the
         * fields are focused when the user presses the Tab key. Components with
         * a defined tab index are focused sequentially first, and then the
         * components with no tab index.
         * 
         * <pre>
         * Form loginBox = new Form();
         * loginBox.setCaption(&quot;Login&quot;);
         * layout.addComponent(loginBox);
         * 
         * // Create the first field which will be focused
         * TextField username = new TextField(&quot;User name&quot;);
         * loginBox.addField(&quot;username&quot;, username);
         * 
         * // Set focus to the user name
         * username.focus();
         * 
         * TextField password = new TextField(&quot;Password&quot;);
         * loginBox.addField(&quot;password&quot;, password);
         * 
         * Button login = new Button(&quot;Login&quot;);
         * loginBox.getFooter().addComponent(login);
         * 
         * // An additional component which natural focus order would
         * // be after the button.
         * CheckBox remember = new CheckBox(&quot;Remember me&quot;);
         * loginBox.getFooter().addComponent(remember);
         * 
         * username.setTabIndex(1);
         * password.setTabIndex(2);
         * remember.setTabIndex(3); // Different than natural place
         * login.setTabIndex(4);
         * </pre>
         * 
         * <p>
         * After all focusable user interface components are done, the browser
         * can begin again from the component with the smallest tab index, or it
         * can take the focus out of the page, for example, to the location bar.
         * </p>
         * 
         * <p>
         * If the tab index is not set (is set to zero), the default tab order
         * is used. The order is somewhat browser-dependent, but generally
         * follows the HTML structure of the page.
         * </p>
         * 
         * <p>
         * A negative value means that the component is completely removed from
         * the tabulation order and can not be reached by pressing the Tab key
         * at all.
         * </p>
         * 
         * @param tabIndex
         *            the tab order of this component. Indexes usually start
         *            from 1. Zero means that default tab order should be used.
         *            A negative value means that the field should not be
         *            included in the tabbing sequence.
         * @see #getTabIndex()
         */
        public void setTabIndex(int tabIndex);

    }

}
