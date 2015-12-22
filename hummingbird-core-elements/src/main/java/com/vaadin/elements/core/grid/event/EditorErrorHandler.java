package com.vaadin.elements.core.grid.event;

import java.io.Serializable;

/**
 * Error handler for the editor
 */
public interface EditorErrorHandler extends Serializable {

    /**
     * Called when an exception occurs while the editor row is being saved
     *
     * @param event
     *            An event providing more information about the error
     */
    void commitError(CommitErrorEvent event);
}