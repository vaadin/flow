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
import java.util.Arrays;

import com.vaadin.annotations.Bower;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.Resource;
import com.vaadin.shared.Position;

/**
 * A notification message, used to display temporary messages to the user - for
 * example "Document saved", or "Save failed".
 * <p>
 * The notification message can consist of several parts: caption, description
 * and icon. It is usually used with only caption - one should be wary of
 * filling the notification with too much information.
 * </p>
 * <p>
 * The notification message tries to be as unobtrusive as possible, while still
 * drawing needed attention. There are several basic types of messages that can
 * be used in different situations:
 * <ul>
 * <li>TYPE_HUMANIZED_MESSAGE fades away quickly as soon as the user uses the
 * mouse or types something. It can be used to show fairly unimportant messages,
 * such as feedback that an operation succeeded ("Document Saved") - the kind of
 * messages the user ignores once the application is familiar.</li>
 * <li>TYPE_WARNING_MESSAGE is shown for a short while after the user uses the
 * mouse or types something. It's default style is also more noticeable than the
 * humanized message. It can be used for messages that do not contain a lot of
 * important information, but should be noticed by the user. Despite the name,
 * it does not have to be a warning, but can be used instead of the humanized
 * message whenever you want to make the message a little more noticeable.</li>
 * <li>TYPE_ERROR_MESSAGE requires to user to click it before disappearing, and
 * can be used for critical messages.</li>
 * <li>TYPE_TRAY_NOTIFICATION is shown for a while in the lower left corner of
 * the window, and can be used for "convenience notifications" that do not have
 * to be noticed immediately, and should not interfere with the current task -
 * for instance to show "You have a new message in your inbox" while the user is
 * working in some other area of the application.</li>
 * </ul>
 * </p>
 * <p>
 * In addition to the basic pre-configured types, a Notification can also be
 * configured to show up in a custom position, for a specified time (or until
 * clicked), and with a custom stylename. An icon can also be added.
 * </p>
 *
 */
@Bower("vaadin-notification")
@Tag("vaadin-notification")
public class Notification extends AbstractComponent implements Serializable {
    public enum Type {
        HUMANIZED_MESSAGE("humanized"), WARNING_MESSAGE(
                "warning"), ERROR_MESSAGE("error"), TRAY_NOTIFICATION(
                        "tray"), /**
                                  * @since 7.2
                                  */
        ASSISTIVE_NOTIFICATION("assistive");

        private String style;

        Type(String style) {
            this.style = style;
        }

        /**
         * @since 7.2
         *
         * @return the style name for this notification type.
         */
        public String getStyle() {
            return style;
        }
    }

    public static final int DELAY_FOREVER = -1;
    public static final int DELAY_NONE = 0;

    private Resource icon;
    private String styleName;

    /**
     * Creates a "humanized" notification message.
     *
     * The caption is rendered as plain text with HTML automatically escaped.
     *
     * @param caption
     *            The message to show
     */
    public Notification(String caption) {
        this(caption, null, Type.HUMANIZED_MESSAGE);
    }

    /**
     * Creates a notification message of the specified type.
     *
     * The caption is rendered as plain text with HTML automatically escaped.
     *
     * @param caption
     *            The message to show
     * @param type
     *            The type of message
     */
    public Notification(String caption, Type type) {
        this(caption, null, type);
    }

    /**
     * Creates a "humanized" notification message with a bigger caption and
     * smaller description.
     *
     * The caption and description are rendered as plain text with HTML
     * automatically escaped.
     *
     * @param caption
     *            The message caption
     * @param description
     *            The message description
     */
    public Notification(String caption, String description) {
        this(caption, description, Type.HUMANIZED_MESSAGE);
    }

    /**
     * Creates a notification message of the specified type, with a bigger
     * caption and smaller description.
     *
     * The caption and description are rendered as plain text with HTML
     * automatically escaped.
     *
     * @param caption
     *            The message caption
     * @param description
     *            The message description
     * @param type
     *            The type of message
     */
    public Notification(String caption, String description, Type type) {
        this(caption, description, type, false);
    }

    /**
     * Creates a notification message of the specified type, with a bigger
     * caption and smaller description.
     *
     * Care should be taken to to avoid XSS vulnerabilities if html is allowed.
     *
     * @param caption
     *            The message caption
     * @param description
     *            The message description
     * @param type
     *            The type of message
     * @param htmlContentAllowed
     *            Whether html in the caption and description should be
     *            displayed as html or as plain text
     */
    public Notification(String caption, String description, Type type,
            boolean htmlContentAllowed) {
        getElement().appendChild(new Element("div"));
        getElement().appendChild(new Element("div"));
        setCaption(caption);
        setDescription(description);
        setType(type);
    }

    private void setType(Type type) {
        styleName = type.getStyle();
        switch (type) {
        case WARNING_MESSAGE:
            setDelayMsec(1500);
            break;
        case ERROR_MESSAGE:
            setDelayMsec(-1);
            break;
        case TRAY_NOTIFICATION:
            setDelayMsec(3000);
            setPosition(Position.BOTTOM_RIGHT);
            break;
        case ASSISTIVE_NOTIFICATION:
            setDelayMsec(3000);
            setPosition(Position.ASSISTIVE);
            break;
        case HUMANIZED_MESSAGE:
        default:
            break;
        }
    }

    /**
     * Gets the caption part of the notification message.
     *
     * @return The message caption
     */
    @Override
    public String getCaption() {
        return getElement().getChild(0).getTextContent();
    }

    /**
     * Sets the caption part of the notification message
     *
     * @param caption
     *            The message caption
     */
    @Override
    public void setCaption(String caption) {
        getElement().getChild(0).setTextContent(caption);
    }

    /**
     * Gets the description part of the notification message.
     *
     * @return The message description.
     */
    public String getDescription() {
        return getElement().getChild(1).getTextContent();
    }

    /**
     * Sets the description part of the notification message.
     *
     * @param description
     */
    public void setDescription(String description) {
        getElement().getChild(1).setTextContent(description);
    }

    /**
     * Gets the position of the notification message.
     *
     * @return The position
     */
    public Position getPosition() {
        for (Position p : Position.values()) {
            boolean hasAll = Arrays.stream(p.getClassNames())
                    .allMatch(n -> getElement().hasClass(n));
            if (hasAll) {
                return p;
            }
        }

        return null;
    }

    /**
     * Sets the position of the notification message.
     *
     * @param position
     *            The desired notification position
     */
    public void setPosition(Position position) {
        for (String className : Position.getAllClassNames()) {
            getElement().removeClass(className);
        }

        for (String className : position.getClassNames()) {
            getElement().addClass(className);
        }
    }

    /**
     * Gets the icon part of the notification message.
     *
     * @return The message icon
     */
    @Override
    public Resource getIcon() {
        return icon;
    }

    /**
     * Sets the icon part of the notification message.
     *
     * @param icon
     *            The desired message icon
     */
    @Override
    public void setIcon(Resource icon) {
        this.icon = icon;
    }

    /**
     * Gets the delay before the notification disappears.
     *
     * @return the delay in msec, -1 indicates the message has to be clicked.
     */
    public int getDelayMsec() {
        return getElement().getAttribute("autoClose", -1);
    }

    /**
     * Sets the delay before the notification disappears.
     *
     * @param delayMsec
     *            the desired delay in msec, -1 to require the user to click the
     *            message
     */
    public void setDelayMsec(int delayMsec) {
        getElement().setAttribute("autoClose", delayMsec);
    }

    /**
     * Sets the style name for the notification message.
     *
     * @param styleName
     *            The desired style name.
     */
    @Override
    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    /**
     * Gets the style name for the notification message.
     *
     * @return
     */
    @Override
    public String getStyleName() {
        return styleName;
    }

    public void show(Page page) {
        show(page.getUI());

    }

    /**
     * Shows this notification on a Page.
     *
     * @param page
     *            The page on which the notification should be shown
     */
    public void show(UI ui) {
        // Workaround for hierarchy issue when client side removes the element
        // from the DOM
        getElement().setAttribute("keepOnClose", true);

        getElement().addEventListener("iron-overlay-closed", e -> {
            ui.getRoot().getLayer(20000).removeComponent(this);
        });

        ui.getRoot().ensureLayer(20000).addComponent(this);
    }

    /**
     * Shows a notification message on the middle of the current page. The
     * message automatically disappears ("humanized message").
     *
     * The caption is rendered as plain text with HTML automatically escaped.
     *
     * @see #Notification(String)
     * @see #show(Page)
     *
     * @param caption
     *            The message
     */
    public static void show(String caption) {
        new Notification(caption).show(UI.getCurrent());
    }

    /**
     * Shows a notification message the current page. The position and behavior
     * of the message depends on the type, which is one of the basic types
     * defined in {@link Notification}, for instance
     * Notification.TYPE_WARNING_MESSAGE.
     *
     * The caption is rendered as plain text with HTML automatically escaped.
     *
     * @see #Notification(String, int)
     * @see #show(Page)
     *
     * @param caption
     *            The message
     * @param type
     *            The message type
     */
    public static void show(String caption, Type type) {
        new Notification(caption, type).show(UI.getCurrent());
    }

    /**
     * Shows a notification message the current page. The position and behavior
     * of the message depends on the type, which is one of the basic types
     * defined in {@link Notification}, for instance
     * Notification.TYPE_WARNING_MESSAGE.
     *
     * The caption is rendered as plain text with HTML automatically escaped.
     *
     * @see #Notification(String, Type)
     * @see #show(Page)
     *
     * @param caption
     *            The message
     * @param description
     *            The message description
     * @param type
     *            The message type
     */
    public static void show(String caption, String description, Type type) {
        new Notification(caption, description, type).show(UI.getCurrent());
    }
}
