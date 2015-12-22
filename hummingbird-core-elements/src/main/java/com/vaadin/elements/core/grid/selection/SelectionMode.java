package com.vaadin.elements.core.grid.selection;

import com.vaadin.elements.core.grid.Grid;

/**
 * Selection modes representing built-in {@link SelectionModel SelectionModels}
 * that come bundled with {@link Grid}.
 * <p>
 * Passing one of these enums into {@link Grid#setSelectionMode(SelectionMode)}
 * is equivalent to calling {@link Grid#setSelectionModel(SelectionModel)} with
 * one of the built-in implementations of {@link SelectionModel}.
 *
 * @see Grid#setSelectionMode(SelectionMode)
 * @see Grid#setSelectionModel(SelectionModel)
 */
public enum SelectionMode {
    /** A SelectionMode that maps to {@link SingleSelectionModel} */
    SINGLE {
        @Override
        public SelectionModel createModel() {
            return new SingleSelectionModel();
        }

    },

    /** A SelectionMode that maps to {@link MultiSelectionModel} */
    MULTI {
        @Override
        public SelectionModel createModel() {
            return new MultiSelectionModel();
        }
    },

    /** A SelectionMode that maps to {@link NoSelectionModel} */
    NONE {
        @Override
        public SelectionModel createModel() {
            return new NoSelectionModel();
        }
    };

    public abstract SelectionModel createModel();
}