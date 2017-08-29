/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.Arrays;

import com.vaadin.flow.dom.Element;

/**
 * A layout component that implements Flexbox. It uses the default
 * flex-direction and doesn't have any predetermined width or height.
 * <p>
 * This component can be used as a base class for more advanced layouts.
 * 
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flexible_Box_Layout/Using_CSS_flexible_boxes">Using
 *      CSS Flexible boxes on MDN</a>
 */
public class FlexLayout extends Component
        implements HasOrderedComponents<FlexLayout>, HasStyle, HasSize {

    protected static final String JUSTIFY_CONTENT_CSS_PROPERTY = "justifyContent";
    protected static final String FLEX_GROW_CSS_PROPERTY = "flexGrow";
    protected static final String ALIGN_SELF_CSS_PROPERTY = "alignSelf";
    protected static final String ALIGN_ITEMS_CSS_PROPERTY = "alignItems";

    /**
     * Enum with the possible values for the component alignment inside the
     * layout. It correlates to the <code>align-items</code> CSS property.
     */
    public enum Alignment {
        START("flex-start"), END("flex-end"), CENTER("center"), STRETCH(
                "stretch"), BASELINE("baseline"), AUTO("auto");

        private final String flexValue;

        Alignment(String flexValue) {
            this.flexValue = flexValue;
        }

        String getFlexValue() {
            return flexValue;
        }

        static Alignment toAlignment(String flexValue, Alignment defaultValue) {
            return Arrays.stream(values()).filter(
                    alignment -> alignment.getFlexValue().equals(flexValue))
                    .findFirst().orElse(defaultValue);
        }
    }

    /**
     * Enum with the possible values for the way the extra space inside the
     * layout is distributed among the components. It correlates to the
     * <code>justify-content</code> CSS property.
     *
     */
    public enum SpacingMode {
        BETWEEN("space-between"), AROUND("space-around"), EVENLY(
                "space-evenly");

        private final String flexValue;

        SpacingMode(String flexValue) {
            this.flexValue = flexValue;
        }

        String getFlexValue() {
            return flexValue;
        }

        static SpacingMode toSpacingMode(String flexValue,
                SpacingMode defaultValue) {
            return Arrays.stream(values())
                    .filter(spacing -> spacing.getFlexValue().equals(flexValue))
                    .findFirst().orElse(defaultValue);
        }

    }

    private SpacingMode spacingMode = SpacingMode.BETWEEN;
    private boolean spacing;

    /**
     * Default constructor. Creates an empty layout without spacing, with items
     * aligned as {@link Alignment#STRETCH}.
     */
    public FlexLayout() {
        super(new Element("div"));
        getStyle().set("display", "flex");
    }

    /**
     * Convenience constructor to create a layout with the children already
     * inside it.
     * 
     * @param children
     *            the items to add to this layout
     * @see #add(Component...)
     */
    public FlexLayout(Component... children) {
        this();
        add(children);
    }

    /**
     * Sets the default alignment to be used by all components without
     * individual alignments inside the layout. Individual components can be
     * aligned by using the
     * {@link #setComponentAlignment(Alignment, Component...)} method.
     * <p>
     * The default alignment is {@link Alignment#STRETCH}.
     * 
     * @param alignment
     *            the alignment to apply to the components. Setting
     *            <code>null</code> will reset the alignment to its default
     */
    public void setDefaultComponentAlignment(Alignment alignment) {
        if (alignment == null) {
            getStyle().remove(ALIGN_ITEMS_CSS_PROPERTY);
        } else {
            getStyle().set(ALIGN_ITEMS_CSS_PROPERTY, alignment.getFlexValue());
        }
    }

    /**
     * Gets the default alignment used by all components without individual
     * alignments inside the layout.
     * <p>
     * The default alignment is {@link Alignment#STRETCH}.
     * 
     * @return the general alignment used by the layout, never <code>null</code>
     */
    public Alignment getDefaultComponentAlignment() {
        return Alignment.toAlignment(getStyle().get(ALIGN_ITEMS_CSS_PROPERTY),
                Alignment.STRETCH);
    }

    /**
     * Sets an alignment for individual components inside the layout. This
     * individual alignment for the component overrides any alignment set at the
     * {@link #setDefaultComponentAlignment(Alignment)}.
     * <p>
     * The default alignment for individual components is
     * {@link Alignment#AUTO}.
     * 
     * @param alignment
     *            the individual alignment for the children components. Setting
     *            <code>null</code> will reset the alignment to its default
     * @param components
     *            The components to which the individual alignment should be set
     */
    public void setComponentAlignment(Alignment alignment,
            Component... components) {
        if (alignment == null) {
            for (Component component : components) {
                component.getElement().getStyle()
                        .remove(ALIGN_SELF_CSS_PROPERTY);
            }
        } else {
            for (Component component : components) {
                component.getElement().getStyle().set(ALIGN_SELF_CSS_PROPERTY,
                        alignment.getFlexValue());
            }
        }
    }

    /**
     * Gets the individual alignment of a given component.
     * <p>
     * The default alignment for individual components is
     * {@link Alignment#AUTO}.
     * 
     * @param component
     *            The component which individual layout should be read
     * @return the alignment of the component, never <code>null</code>
     */
    public Alignment getComponentAlignment(Component component) {
        return Alignment.toAlignment(
                component.getElement().getStyle().get(ALIGN_SELF_CSS_PROPERTY),
                Alignment.AUTO);
    }

    /**
     * Sets the expand ratio of the components inside the layout. The expand
     * ratio specifies what amount of the available space inside the layout the
     * component should take up, proportionally to the other components.
     * <p>
     * For example, if all components have a expand ratio set to 1, the
     * remaining space in the layout will be distributed equally to all
     * components inside the layout. If you set a expend ratio of one component
     * to 2, that component will take twice the available space as the other
     * components, and so on.
     * <p>
     * Setting to expand ratio 0 disables the expansion of the component.
     * Negative values are not allowed.
     * 
     * @param ratio
     *            the proportion of the available space the components should
     *            take up
     * @param components
     *            the components to apply the expand ratio
     */
    public void setExpandRatio(double ratio, Component... components) {
        if (ratio < 0) {
            throw new IllegalArgumentException(
                    "Expand ratio cannot be negative");
        }
        if (ratio == 0) {
            for (Component component : components) {
                component.getElement().getStyle()
                        .remove(FLEX_GROW_CSS_PROPERTY);
            }
        } else {
            for (Component component : components) {
                component.getElement().getStyle().set(FLEX_GROW_CSS_PROPERTY,
                        String.valueOf(ratio));
            }
        }
    }

    /**
     * Gets the expand ratio of a given component.
     * 
     * @param component
     *            the component to read the expand ratio from
     * @return the expand ratio, or 0 if none was set
     */
    public double getExpandRatio(Component component) {
        String ratio = component.getElement().getStyle()
                .get(FLEX_GROW_CSS_PROPERTY);
        if (ratio == null || ratio.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(ratio);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "The expand ratio of the component is not parseable to double: "
                            + ratio,
                    e);
        }
    }

    /**
     * Sets the layout to show empty space among items if the space in the
     * layout is greater than the space required by children. By default, all
     * components are rendered together and the remaining space is distributed
     * at the end of the layout.
     * <p>
     * When using spacing, the {@link SpacingMode} is taken in consideration
     * when distributing the available space. The default spacing mode is
     * {@link SpacingMode#BETWEEN}.
     * 
     * @param spacing
     *            <code>true</code> to enable empty spaces among children,
     *            <code>false</code> to disable
     * @see #setSpacingMode(SpacingMode)
     */
    public void setSpacing(boolean spacing) {
        this.spacing = spacing;
        if (spacing) {
            getElement().getStyle().set(JUSTIFY_CONTENT_CSS_PROPERTY,
                    spacingMode.getFlexValue());
        } else {
            getElement().getStyle().remove(JUSTIFY_CONTENT_CSS_PROPERTY);
        }
    }

    /**
     * Gets whether spacing is used in this layout. The default is
     * <code>false</code>.
     * 
     * @return <code>true</code> if spacing is used, <code>false</code>
     *         otherwise
     */
    public boolean isSpacing() {
        return spacing;
    }

    /**
     * Gets the {@link SpacingMode} used by this layout. The spacing mode is
     * only effective when {@link #setSpacing(boolean)} is set to
     * <code>true</code>.
     * <p>
     * The default spacing mode is {@link SpacingMode#BETWEEN}.
     * 
     * @param spacingMode
     *            the spacing mode of the layout, never <code>null</code>
     */
    public void setSpacingMode(SpacingMode spacingMode) {
        if (spacingMode == null) {
            throw new IllegalArgumentException(
                    "The 'spacingMode' argument can not be null");
        }
        this.spacingMode = spacingMode;
        if (spacing) {
            getElement().getStyle().set(JUSTIFY_CONTENT_CSS_PROPERTY,
                    spacingMode.getFlexValue());
        }
    }

    /**
     * Gets the current spacing mode of the layout. This property is only
     * effective when {@link #setSpacing(boolean)} is set to <code>true</code>.
     * <p>
     * The default spacing mode is {@link SpacingMode#BETWEEN}.
     * 
     * @return the spacing mode used by the layout, never <code>null</code>
     */
    public SpacingMode getSpacingMode() {
        return spacingMode;
    }

}
