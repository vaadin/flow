/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

/**
 * Various node properties' ids.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public final class NodeProperties {

    /**
     * Key for {@link ElementData#getTag()}.
     */
    public static final String TAG = "tag";

    /**
     * Key for {@link ElementData#getPayload()}.
     */
    public static final String PAYLOAD = "payload";

    /**
     * Key for {@link TextNodeMap#getText()}.
     */
    public static final String TEXT = "text";
    /**
     * Key for {@link ShadowRootData}.
     */
    public static final String SHADOW_ROOT = "shadowRoot";
    /**
     * Key for {@link BasicTypeValue#getValue()}.
     */
    public static final String VALUE = "value";

    /**
     * JsonObject type key for {@link VirtualChildrenList}.
     */
    public static final String TYPE = "type";

    /**
     * JsonObject in-memory type value for {@link VirtualChildrenList}.
     */
    public static final String IN_MEMORY_CHILD = "inMemory";

    /**
     * JsonObject {@code @id} type value for {@link VirtualChildrenList}.
     */
    public static final String INJECT_BY_ID = "@id";

    /**
     * JsonObject template-in-template type value for
     * {@link VirtualChildrenList}.
     */
    public static final String TEMPLATE_IN_TEMPLATE = "subTemplate";

    /**
     * Key for {@link ElementData#isVisible()}.
     */
    public static final String VISIBLE = "visible";

    /** Key for id property. */
    public static final String ID = "id";

    /**
     * The property value used on the client side only in addition to
     * {@link #VISIBLE}.
     */
    public static final String VISIBILITY_BOUND_PROPERTY = "bound";

    /**
     * The property used on the client side only in addition to
     * {@link #VISIBLE}. Stores the client side value of "hidden" property.
     */
    public static final String VISIBILITY_HIDDEN_PROPERTY = "hidden";

    /**
     * The property used on the client side only in addition to
     * {@link #VISIBLE}. It stores the client side value of the CSS "display"
     * property to be able to restore when making a hidden element visible
     * again. Used only when the element is inside a shadow root, and the CSS
     * "display: none" is set in addition the "hidden" attribute.
     */
    public static final String VISIBILITY_STYLE_DISPLAY_PROPERTY = "styleDisplay";

    /**
     * The property in Json object which marks the object as special value
     * transmitting URI (not just any string).
     * <p>
     * Used in the {@link ElementAttributeMap}.
     */
    public static final String URI_ATTRIBUTE = "uri";

    private NodeProperties() {
    }
}
