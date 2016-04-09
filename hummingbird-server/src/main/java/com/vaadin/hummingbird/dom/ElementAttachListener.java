package com.vaadin.hummingbird.dom;

/**
 * Interface for listening element attach events. It is invoked when the
 * element is attached to a UI.
 */
@FunctionalInterface
public interface ElementAttachListener {
    /**
     * Invoked when an element is attached to the UI.
     *
     * @param event
     *            the attach event fired
     */
    void onAttach(ElementAttachEvent event);
}