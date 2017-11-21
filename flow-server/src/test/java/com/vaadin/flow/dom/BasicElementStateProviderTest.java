package com.vaadin.flow.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.nodefeature.ElementData;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class BasicElementStateProviderTest {

    @Test
    public void supportsSelfCreatedNode() {
        BasicElementStateProvider provider = BasicElementStateProvider.get();
        StateNode node = BasicElementStateProvider.createStateNode("foo");
        assertTrue(provider.supports(node));
    }

    @Test
    public void doesNotSupportEmptyNode() {
        BasicElementStateProvider provider = BasicElementStateProvider.get();
        assertFalse(provider.supports(new StateNode()));
    }

    @Test
    public void supportsUIRootNode() {
        BasicElementStateProvider provider = BasicElementStateProvider.get();
        UI ui = new UI() {

            @Override
            protected void init(VaadinRequest request) {

            }
        };
        StateNode rootNode = ui.getInternals().getStateTree().getRootNode();
        assertTrue(provider.supports(rootNode));

    }

    @Test
    public void getParent_parentNodeIsNull_parentIsNull() {
        Element div = ElementFactory.createDiv();
        assertNull(
                BasicElementStateProvider.get().getParent(div.getNode()));
    }

    @Test
    public void getParent_parentNodeIsNotNull_parentIsNotNull() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);
        assertEquals(parent,
                BasicElementStateProvider.get().getParent(child.getNode()));
    }

    @Test
    public void getParent_parentNodeIsShadowRootNode_parentIsShadowRoot() {
        ShadowRoot parent = ElementFactory.createDiv().attachShadow();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);
        assertEquals(parent,
                BasicElementStateProvider.get().getParent(child.getNode()));
    }

    @Test
    public void createStateNode_stateNodeHasRequiredElementDataFeature() {
        StateNode stateNode = BasicElementStateProvider.createStateNode("div");
        assertTrue(stateNode.isReportedFeature(ElementData.class));
    }
}
