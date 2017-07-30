package com.vaadin.flow.shared;

import com.vaadin.flow.nodefeature.BasicTypeValue;
import com.vaadin.flow.nodefeature.ElementData;
import com.vaadin.flow.nodefeature.ShadowRootData;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.nodefeature.TextNodeMap;

/**
 * Various node properties' ids.
 * 
 * @author Vaadin Ltd.
 */
public final class NodeProperties {

    /**
     * Key for {@link ElementData#getTag()}.
     */
    public static final String TAG = "tag";
    /**
     * Key for {@link TextNodeMap#getText()}.
     */
    public static final String TEXT = "text";
    /**
     * Key for {@link ShadowRootData}.
     */
    public static final String SHADOW_ROOT = "shadowRoot";
    /**
     * Key for {@link TemplateMap#getRootTemplate()}.
     */
    public static final String ROOT_TEMPLATE_ID = "root";
    /**
     * Key for {@link TemplateMap#getModelDescriptor()}.
     */
    public static final String MODEL_DESCRIPTOR = "descriptor";
    /**
     * Key for {@link BasicTypeValue#getValue()}.
     */
    public static final String VALUE = "value";

    private NodeProperties() {
    }
}
