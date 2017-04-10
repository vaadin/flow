package com.vaadin.flow.dom;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class BasicElementStateProviderTest {

    private StateNode stateNode;

    @Before
    public void setUp() {
        stateNode = new StateNode(ElementPropertyMap.class);
    }

    @Test
    public void supportsSelfCreatedNode() {
        BasicElementStateProvider provider = BasicElementStateProvider.get();
        StateNode node = BasicElementStateProvider.get().createStateNode("foo");
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

    @Test
    public void setProperty_propertyStoredInModelMap() {
        BasicElementStateProvider.get().setProperty(stateNode, "foo", "bar",
                false);
        Assert.assertEquals("bar", stateNode
                .getFeature(ElementPropertyMap.class).getProperty("foo"));
    }

    @Test
    public void getProperty_propertyIsReadFromModelMap() {
        stateNode.getFeature(ElementPropertyMap.class).setProperty("foo", "bar",
                false);
        Assert.assertEquals("bar",
                BasicElementStateProvider.get().getProperty(stateNode, "foo"));
    }

    @Test
    public void hasProperty_propertyIsCheckedInModelMap() {
        Assert.assertFalse(
                BasicElementStateProvider.get().hasProperty(stateNode, "foo"));
        stateNode.getFeature(ElementPropertyMap.class).setProperty("foo", "bar",
                false);
        Assert.assertTrue(
                BasicElementStateProvider.get().hasProperty(stateNode, "foo"));
    }

    @Test
    public void removeProperty_propertyIsRemovedFromModelMap() {
        stateNode.getFeature(ElementPropertyMap.class).setProperty("foo", "bar",
                false);
        BasicElementStateProvider.get().removeProperty(stateNode, "foo");
        Assert.assertFalse(
                BasicElementStateProvider.get().hasProperty(stateNode, "foo"));
    }

    @Test
    public void getPropertyNames_propertiesAreReeadFromModelMap() {
        stateNode.getFeature(ElementPropertyMap.class).setProperty("foo", "bar",
                false);
        List<String> propertyNames = BasicElementStateProvider.get()
                .getPropertyNames(stateNode).collect(Collectors.toList());
        Assert.assertEquals(1, propertyNames.size());
        String name = propertyNames.get(0);
        Assert.assertEquals("foo", name);
    }
}
