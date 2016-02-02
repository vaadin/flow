package com.vaadin.hummingbird.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;

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
}
