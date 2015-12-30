package com.vaadin.client.communication;

import java.util.logging.Logger;

import com.google.gwt.dom.client.Node;

public class DomApi {
    /**
     * Flag for tracking if Polymer-micro.html is loaded (contains dom)
     */
    private static boolean polymerMicroLoaded = false;

    /**
     * Flag for tracking if Polymer.html is loaded (contains updateStyles)
     */
    private static boolean polymerFullLoaded = false;

    // Start by using Plain Dom API, change this when/if Polymer is loaded
    private static DomApiImpl impl = new PlainDomApiImpl();

    public static DomElement wrap(Node node) {
        return impl.wrap(node);
    }

    public static void flush() {
        impl.flushDom();
    }

    public static void polymerMaybeLoaded() {
        if (polymerFullLoaded) {
            return;
        }

        if (PolymerDomApiImpl.isPolymerFullLoaded()) {
            // Full Polymer loaded
            if (!polymerMicroLoaded) {
                // Full loads micro automatically
                polymerMicroLoaded();
            }
            polymerFullLoaded = true;
            polymerFullLoaded();
        } else if (!polymerMicroLoaded
                && PolymerDomApiImpl.isPolymerMicroLoaded()) {
            // Only micro loaded
            polymerMicroLoaded();
        }
    }

    private static void polymerFullLoaded() {
        /*
         * In case the initial loading page has contained Polymer style modules,
         * we need to trigger update styles for those. Since
         * Polymer.updateStyles can be costly, it should not be triggered (and
         * currently isn't) otherwise.
         */
        getLogger().info("Updating Polymer styles");
        PolymerDomApiImpl.updateStyles();
    }

    private static void polymerMicroLoaded() {
        polymerMicroLoaded = true;
        getLogger().info("Polymer micro is now loaded, using Polymer DOM API");
        impl = new PolymerDomApiImpl();
    }

    private static Logger getLogger() {
        return Logger.getLogger(DomApi.class.getName());
    }
}
