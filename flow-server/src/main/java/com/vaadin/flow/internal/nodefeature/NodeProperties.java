/*
 * Copyright 2000-2018 Vaadin Ltd.
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

    private NodeProperties() {
    }
}
