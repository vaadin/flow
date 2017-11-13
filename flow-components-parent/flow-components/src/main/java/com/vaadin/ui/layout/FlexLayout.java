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
package com.vaadin.ui.layout;

import java.util.Arrays;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HasOrderedComponents;
import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.common.HasStyle;

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
@Tag(Tag.DIV)
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
    public enum JustifyContentMode {
        START("flex-start"), END("flex-end"), BETWEEN("space-between"), AROUND(
                "space-around"), EVENLY("space-evenly");

        private final String flexValue;

        JustifyContentMode(String flexValue) {
            this.flexValue = flexValue;
        }

        String getFlexValue() {
            return flexValue;
        }

        static JustifyContentMode toJustifyContentMode(String flexValue,
                JustifyContentMode defaultValue) {
            return Arrays.stream(values())
                    .filter(justifyContent -> justifyContent.getFlexValue()
                            .equals(flexValue))
                    .findFirst().orElse(defaultValue);
        }

    }

    private JustifyContentMode justifyContentMode = JustifyContentMode.BETWEEN;

    /**
     * Default constructor. Creates an empty layout with justify content mode as
     * {@link JustifyContentMode#START}, with items aligned as
     * {@link Alignment#STRETCH}.
     */
    public FlexLayout() {
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
     * aligned by using the {@link #setAlignSelf(Alignment, Component...)}
     * method.
     * <p>
     * It effectively sets the {@code "alignItems"} style value.
     * <p>
     * The default alignment is {@link Alignment#STRETCH}.
     *
     * @param alignment
     *            the alignment to apply to the components. Setting
     *            <code>null</code> will reset the alignment to its default
     */
    public void setAlignItems(Alignment alignment) {
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
    public Alignment getAlignItems() {
        return Alignment.toAlignment(getStyle().get(ALIGN_ITEMS_CSS_PROPERTY),
                Alignment.STRETCH);
    }

    /**
     * Sets an alignment for individual components inside the layout. This
     * individual alignment for the component overrides any alignment set at the
     * {@link #setAlignItems(Alignment)}.
     * <p>
     * It effectively sets the {@code "alignSelf"} style value.
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
    public void setAlignSelf(Alignment alignment, Component... components) {
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
    public Alignment getAlignSelf(Component component) {
        return Alignment.toAlignment(
                component.getElement().getStyle().get(ALIGN_SELF_CSS_PROPERTY),
                Alignment.AUTO);
    }

    /**
     * Sets the flex grow property of the components inside the layout. The flex
     * grow property specifies what amount of the available space inside the
     * layout the component should take up, proportionally to the other
     * components.
     * <p>
     * For example, if all components have a flex grow property value set to 1,
     * the remaining space in the layout will be distributed equally to all
     * components inside the layout. If you set a flex grow property of one
     * component to 2, that component will take twice the available space as the
     * other components, and so on.
     * <p>
     * Setting to flex grow property value 0 disables the expansion of the
     * component. Negative values are not allowed.
     *
     * @param flexGrow
     *            the proportion of the available space the components should
     *            take up
     * @param components
     *            the components to apply the flex grow property
     */
    public void setFlexGrow(double flexGrow, Component... components) {
        if (flexGrow < 0) {
            throw new IllegalArgumentException(
                    "Flex grow property cannot be negative");
        }
        if (flexGrow == 0) {
            for (Component component : components) {
                component.getElement().getStyle()
                        .remove(FLEX_GROW_CSS_PROPERTY);
            }
        } else {
            for (Component component : components) {
                component.getElement().getStyle().set(FLEX_GROW_CSS_PROPERTY,
                        String.valueOf(flexGrow));
            }
        }
    }

    /**
     * Gets the flex grow property of a given component.
     *
     * @param component
     *            the component to read the flex grow property from
     * @return the flex grow property, or 0 if none was set
     */
    public double getFlexGrow(Component component) {
        String ratio = component.getElement().getStyle()
                .get(FLEX_GROW_CSS_PROPERTY);
        if (ratio == null || ratio.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(ratio);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "The flex grow property of the component is not parseable to double: "
                            + ratio,
                    e);
        }
    }

    /**
     * Gets the {@link JustifyContentMode} used by this layout.
     * <p>
     * The default justify content mode is {@link JustifyContentMode#START}.
     *
     * @param justifyContentMode
     *            the justify content mode of the layout, never
     *            <code>null</code>
     */
    public void setJustifyContentMode(JustifyContentMode justifyContentMode) {
        if (justifyContentMode == null) {
            throw new IllegalArgumentException(
                    "The 'justifyContentMode' argument can not be null");
        }
        this.justifyContentMode = justifyContentMode;
        getElement().getStyle().set(JUSTIFY_CONTENT_CSS_PROPERTY,
                justifyContentMode.getFlexValue());
    }

    /**
     * Gets the current justify content mode of the layout.
     * <p>
     * The default justify content mode is {@link JustifyContentMode#START}.
     *
     * @return the justify content mode used by the layout, never
     *         <code>null</code>
     */
    public JustifyContentMode getJustifyContentMode() {
        return justifyContentMode;
    }

    @Override
    public void replace(Component oldComponent, Component newComponent) {
        Alignment alignSelf = null;
        double flexGrow = 0;
        if (oldComponent != null) {
            alignSelf = getAlignSelf(oldComponent);
            flexGrow = getFlexGrow(oldComponent);
        }
        HasOrderedComponents.super.replace(oldComponent, newComponent);
        if (newComponent != null && oldComponent != null) {
            setAlignSelf(alignSelf, newComponent);
            setFlexGrow(flexGrow, newComponent);
        }
    }

}
