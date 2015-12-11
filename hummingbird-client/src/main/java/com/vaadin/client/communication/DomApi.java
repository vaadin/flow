package com.vaadin.client.communication;

import java.util.logging.Logger;

import com.google.gwt.dom.client.Node;

public class DomApi {
    // Start by using Plain Dom API, change this when/if Polymer is loaded
    private static boolean polymerLoaded = false;
    private static DomApiImpl impl = new PlainDomApiImpl();

    public static DomElement wrap(Node node) {
        return impl.wrap(node);
    }

    public static void flush() {
        impl.flushDom();
    }

    public static void polymerMaybeLoaded() {
        if (polymerLoaded) {
            return;
        }

        if (PolymerDomApiImpl.isAvailable()) {
            polymerLoaded = true;
            getLogger().info("Polymer is now loaded, using Polymer DOM API");
            impl = new PolymerDomApiImpl();
        }
    }

    private static Logger getLogger() {
        return Logger.getLogger(DomApi.class.getName());
    }
}
