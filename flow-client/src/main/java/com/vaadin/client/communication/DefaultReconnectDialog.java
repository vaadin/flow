/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.client.communication;

import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.WidgetUtil;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Visibility;
import elemental.dom.Element;
import elemental.events.EventRemover;

/**
 * The default implementation of the reconnect dialog
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultReconnectDialog implements ReconnectDialog {

    private static final String STYLE_RECONNECTING = "active";
    private static final String STYLE_MODAL = "modal";
    private static final String STYLE_BODY_RECONNECTING = "v-reconnecting";

    private Element label;

    private EventRemover clickHandler = null;
    private Element root;

    /**
     * Creates a new instance.
     */
    public DefaultReconnectDialog() {
        root = Browser.getDocument().createElement("div");
        root.setClassName("v-reconnect-dialog");
        Element spinner = Browser.getDocument().createElement("div");
        spinner.setClassName("spinner");

        label = Browser.getDocument().createElement("span");
        label.setClassName("text");

        root.appendChild(spinner);
        root.appendChild(label);

    }

    @Override
    public void setText(String text) {
        label.setTextContent(text);
    }

    @Override
    public void setReconnecting(boolean reconnecting) {
        if (reconnecting) {
            root.getClassList().add(STYLE_RECONNECTING);
        } else {
            root.getClassList().remove(STYLE_RECONNECTING);
        }

        Element body = Browser.getDocument().getBody();
        if (reconnecting) {
            body.getClassList().add(STYLE_BODY_RECONNECTING);
        } else {
            body.getClassList().remove(STYLE_BODY_RECONNECTING);
        }

        // Click to refresh after giving up
        if (!reconnecting) {
            clickHandler = root.addEventListener("click",
                    // refresh
                    event -> WidgetUtil.redirect(null), false);
        } else {
            if (clickHandler != null) {
                clickHandler.remove();
                clickHandler = null;
            }
        }
    }

    @Override
    public void show() {
        if (root.getParentElement() == null) {
            Browser.getDocument().getBody().appendChild(root);
        }
    }

    @Override
    public void preload() {
        setModal(false); // Don't interfere with application use
        show();
        root.getStyle().setVisibility(Visibility.HIDDEN);
        root.getClassList().add(STYLE_RECONNECTING);

        Scheduler.get().scheduleDeferred(() -> {
            root.getStyle().setVisibility(Visibility.VISIBLE);
            root.getClassList().remove(STYLE_RECONNECTING);
            hide();
        });
    }

    @Override
    public boolean isVisible() {
        return root.getParentElement() != null;
    }

    @Override
    public void hide() {
        if (isVisible()) {
            root.getParentElement().removeChild(root);
        }
    }

    @Override
    public void setModal(boolean modal) {
        if (modal) {
            root.getClassList().add(STYLE_MODAL);
        } else {
            root.getClassList().remove(STYLE_MODAL);
        }
    }

    @Override
    public boolean isModal() {
        return root.getClassList().contains(STYLE_MODAL);
    }
}
