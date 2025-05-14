/*
 * Copyright 2000-2025 Vaadin Ltd.
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
     * Key for {@link ElementData#getJavaClass()}.
     */
    public static final String JAVA_CLASS = "jc";

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
     * JsonObject {@code @name} type value for {@link VirtualChildrenList}.
     */
    public static final String INJECT_BY_NAME = "@name";

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
