package com.vaadin.hummingbird.dom;

/**
 * Interface for listening element detach events. It is invoked when the
 * element is detached from the UI.
 */
@FunctionalInterface
public interface ElementDetachListener {
    /**
     * Invoked when an element is detached from the UI.
     *
     * @param event
     *            the detach event fired
     */
    void onDetach(ElementDetachEvent event);
}