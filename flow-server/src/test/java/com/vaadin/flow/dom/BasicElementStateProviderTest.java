package com.vaadin.flow.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class BasicElementStateProviderTest {

    @Test
    public void supportsSelfCreatedNode() {
        BasicElementStateProvider provider = BasicElementStateProvider.get();
        StateNode node = BasicElementStateProvider.createStateNode("foo");
        Assert.assertTrue(provider.supports(node));
    }

    @Test
    public void doesNotSupportEmptyNode() {
        BasicElementStateProvider provider = BasicElementStateProvider.get();
        Assert.assertFalse(provider.supports(new StateNode()));
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
        Assert.assertTrue(provider.supports(rootNode));

    }
}
