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

/**
 * A special type of parent which allows the user to add and remove components
 * to it. Typically does not have any restrictions on the number of children it
 * can contain.
 *
 * @author Vaadin Ltd.
 * @since 3.0
 */
public interface ComponentContainer extends HasComponents {

    /**
     * AlignmentHandler is most commonly an advanced {@link Layout} that can
     * align its components.
     */
    interface AlignmentHandler extends Serializable {
    
        /**
         * Set alignment for one contained component in this layout. Use
         * predefined alignments from Alignment class.
         *
         * Example: <code>
         *      layout.setComponentAlignment(myComponent, Alignment.TOP_RIGHT);
         * </code>
         *
         * @param childComponent
         *            the component to align within it's layout cell.
         * @param alignment
         *            the Alignment value to be set
         */
        public void setComponentAlignment(Component childComponent,
                Alignment alignment);
    
        /**
         * Returns the current Alignment of given component.
         *
         * @param childComponent
         * @return the {@link Alignment}
         */
        public Alignment getComponentAlignment(Component childComponent);
    
        /**
         * Sets the alignment used for new components added to this layout. The
         * default is {@link Alignment#TOP_LEFT}.
         *
         * @param defaultComponentAlignment
         *            The new default alignment
         */
        public void setDefaultComponentAlignment(
                Alignment defaultComponentAlignment);
    
        /**
         * Returns the alignment used for new components added to this layout
         *
         * @return The default alignment
         */
        public Alignment getDefaultComponentAlignment();
    
    }

    /**
     * This type of layout supports automatic addition of space between its
     * components.
     *
     */
    interface SpacingHandler extends Serializable {
        /**
         * Enable spacing between child components within this layout.
         *
         * <p>
         * <strong>NOTE:</strong> This will only affect the space between
         * components, not the space around all the components in the layout
         * (i.e. do not confuse this with the cellspacing attribute of a HTML
         * Table). Use {@link #setMargin(boolean)} to add space around the
         * layout.
         * </p>
         *
         * <p>
         * See the reference manual for more information about CSS rules for
         * defining the amount of spacing to use.
         * </p>
         *
         * @param enabled
         *            true if spacing should be turned on, false if it should be
         *            turned off
         */
        public void setSpacing(boolean enabled);
    
        /**
         *
         * @return true if spacing between child components within this layout
         *         is enabled, false otherwise
         */
        public boolean isSpacing();
    }

    /**
     * This type of layout supports automatic addition of margins (space around
     * its components).
     */
    interface MarginHandler extends Serializable {
    
        /**
         * Enable layout margins. Affects all four sides of the layout.
         * <p>
         * The size of the margin is decided by the {@code margin} style in the
         * theme
         *
         * @param enabled
         *            true if margins should be enabled on all sides, false to
         *            disable all margins
         */
        public void setMargin(boolean enabled);
    
        /**
         * Checks if layout margins are enabled.
         *
         * @return true if margins are enabled on all sides, false otherwise
         */
        public boolean isMargin();
    }

    /**
     * Adds the component into this container.
     *
     * @param c
     *            the component to be added.
     */
    public void addComponent(Component c);

    /**
     * Adds the components in the given order to this component container.
     *
     * @param components
     *            The components to add.
     */
    public void addComponents(Component... components);

    /**
     * Removes the component from this container.
     *
     * @param c
     *            the component to be removed.
     */
    public void removeComponent(Component c);

    /**
     * Removes all components from this container.
     */
    public void removeAllComponents();

    /**
     * Replaces the component in the container with another one without changing
     * position.
     *
     * <p>
     * This method replaces component with another one is such way that the new
     * component overtakes the position of the old component. If the old
     * component is not in the container, the new component is added to the
     * container. If the both component are already in the container, their
     * positions are swapped. Component attach and detach events should be taken
     * care as with add and remove.
     * </p>
     *
     * @param oldComponent
     *            the old component that will be replaced.
     * @param newComponent
     *            the new component to be replaced.
     */
    public void replaceComponent(Component oldComponent,
            Component newComponent);

    /**
     * Gets the number of children this {@link ComponentContainer} has. This
     * must be symmetric with what {@link #getComponentIterator()} returns.
     *
     * @return The number of child components this container has.
     * @since 7.0.0
     */
    public int getComponentCount();

    /**
     * Moves all components from an another container into this container. The
     * components are removed from <code>source</code>.
     *
     * @param source
     *            the container which contains the components that are to be
     *            moved to this container.
     */
    public void moveComponentsFrom(ComponentContainer source);

}
