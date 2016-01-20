package com.vaadin.elements.core.grid.event;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.vaadin.util.ReflectTools;

/**
 * Interface for an editor event listener
 */
public interface EditorListener extends Serializable {

    public static final Method EDITOR_OPEN_METHOD = ReflectTools.findMethod(
            EditorEvent.class, "editorOpened", EditorOpenEvent.class);
    public static final Method EDITOR_MOVE_METHOD = ReflectTools.findMethod(
            EditorEvent.class, "editorMoved", EditorMoveEvent.class);
    public static final Method EDITOR_CLOSE_METHOD = ReflectTools.findMethod(
            EditorEvent.class, "editorClosed", EditorCloseEvent.class);

    /**
     * Called when an editor is opened
     *
     * @param e
     *            an editor open event object
     */
    public void editorOpened(EditorOpenEvent e);

    /**
     * Called when an editor is reopened without closing it first
     *
     * @param e
     *            an editor move event object
     */
    public void editorMoved(EditorMoveEvent e);

    /**
     * Called when an editor is closed
     *
     * @param e
     *            an editor close event object
     */
    public void editorClosed(EditorCloseEvent e);

}