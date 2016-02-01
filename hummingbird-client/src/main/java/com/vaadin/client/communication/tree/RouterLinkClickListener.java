package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

public class RouterLinkClickListener implements DomListener {

    public static final String ATTRIBUTE_NAME = "router-link";

    private JavaScriptObject jsListener;

    public static void addRouterLinkListener(Element element) {
        TreeUpdater.debug("INSERTING ROUTER CLICK LISTENER FOR: "
                + TreeUpdater.debugHtml(element));
        RouterLinkClickListener listener = new RouterLinkClickListener();
        JavaScriptObject javaScriptObject = TreeUpdater.addDomListener(element,
                "click", listener);
        listener.jsListener = javaScriptObject;
    }

    @Override
    public void handleEvent(JavaScriptObject event) {
        NativeEvent clickEvent = event.cast();
        Element element = clickEvent.getEventTarget().cast();

        // remove handler if element didn't have the attribute
        if (!element.hasAttribute(ATTRIBUTE_NAME)) {
            TreeUpdater.debug("REMOVING ROUTER CLICK LISTENER FOR: "
                    + TreeUpdater.debugHtml(element));
            TreeUpdater.removeDomListener(element, "click", jsListener);
            jsListener = null;
            return;
        }

        if (clickEvent.getCtrlKey() || clickEvent.getMetaKey()
                || clickEvent.getAltKey() || clickEvent.getShiftKey()) {
            // ignore if any keys used
        } else {
            TreeUpdater.debug("HANDLING ROUTER CLICK EVENT FOR: "
                    + TreeUpdater.debugHtml(element));
            clickEvent.preventDefault();
            clickEvent.stopPropagation();
            if (element.hasAttribute("href")) {
                String href = element.getAttribute("href");
                JavaScriptObject propertyJSO = element
                        .getPropertyJSO("router-link");
                fireRouterLinkClick(element, href, propertyJSO);
            }
        }
    }

    public static native void fireRouterLinkClick(Element element,
            String linkHref, JavaScriptObject params)

    /*-{
        var event = new CustomEvent('router-link-click',
            {bubbles: true, detail: {href: linkHref, routerlink: params}
            });
        element.dispatchEvent(event);
    }-*/;
}
