package com.vaadin.flow.dom;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.NodeVisitor.ElementType;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.server.VaadinRequest;

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

    @Test
    public void getParent_parentNodeIsNull_parentIsNull() {
        Element div = ElementFactory.createDiv();
        Assert.assertNull(
                BasicElementStateProvider.get().getParent(div.getNode()));
    }

    @Test
    public void getParent_parentNodeIsNotNull_parentIsNotNull() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);
        Assert.assertEquals(parent,
                BasicElementStateProvider.get().getParent(child.getNode()));
    }

    @Test
    public void getParent_parentNodeIsShadowRootNode_parentIsShadowRoot() {
        ShadowRoot parent = ElementFactory.createDiv().attachShadow();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);
        Assert.assertEquals(parent,
                BasicElementStateProvider.get().getParent(child.getNode()));
    }

    @Test
    public void createStateNode_stateNodeHasRequiredElementDataFeature() {
        StateNode stateNode = BasicElementStateProvider.createStateNode("div");
        Assert.assertTrue(stateNode.isReportedFeature(ElementData.class));
    }

    @Test
    public void visitOnlyNode_hasDescendants_nodeVisitedAndNoDescendantsVisited() {
        TestNodeVisitor visitor = new TestNodeVisitor();
        visitor.visitDescendants = false;

        Map<Node<?>, ElementType> map = new HashMap<>();

        Element subject = createHierarchy(map);

        BasicElementStateProvider.get().visit(subject.getNode(), visitor);

        Assert.assertEquals(1, visitor.visited.size());
        Assert.assertEquals(subject,
                visitor.visited.keySet().iterator().next());
        Assert.assertEquals(ElementType.REGULAR,
                visitor.visited.values().iterator().next());
    }

    @Test
    public void visitOnlyNode_hasDescendants_nodeAndDescendatnsAreVisited() {
        TestNodeVisitor visitor = new TestNodeVisitor();
        visitor.visitDescendants = true;

        Map<Node<?>, ElementType> map = new HashMap<>();

        Element subject = createHierarchy(map);

        BasicElementStateProvider.get().visit(subject.getNode(), visitor);

        Assert.assertTrue(map.size() > 1);

        Assert.assertEquals(
                "The collected descendants doesn't match expected descendatns",
                map, visitor.visited);
    }

    @Test
    public void setVisible() {
        Element element = ElementFactory.createDiv();

        Assert.assertTrue(
                element.getNode().getFeature(ElementData.class).isVisible());

        BasicElementStateProvider.get().setVisible(element.getNode(), true);

        Assert.assertTrue(
                element.getNode().getFeature(ElementData.class).isVisible());

        BasicElementStateProvider.get().setVisible(element.getNode(), false);

        Assert.assertFalse(
                element.getNode().getFeature(ElementData.class).isVisible());

    }

    private Element createHierarchy(Map<Node<?>, ElementType> map) {
        Element root = ElementFactory.createDiv();

        map.put(root, ElementType.REGULAR);

        ShadowRoot shadowRoot = root.attachShadow();

        map.put(shadowRoot, null);

        Element shadowChild = ElementFactory.createAnchor();
        Element shadowVirtualChild = ElementFactory.createBr();
        shadowRoot.appendChild(shadowChild);
        shadowRoot.appendVirtualChild(shadowVirtualChild);

        map.put(shadowChild, ElementType.REGULAR);
        map.put(shadowVirtualChild, ElementType.VIRTUAL);

        Element child = ElementFactory.createDiv();

        root.appendChild(child);

        map.put(child, ElementType.REGULAR);

        Element virtualChild = ElementFactory.createDiv();

        root.appendVirtualChild(virtualChild);

        map.put(virtualChild, ElementType.VIRTUAL);

        Element virtualGrandChild = ElementFactory.createDiv();

        child.getStateProvider().appendVirtualChild(child.getNode(),
                virtualGrandChild, NodeProperties.INJECT_BY_ID, "id");

        map.put(virtualGrandChild, ElementType.VIRTUAL_ATTACHED);

        return root;
    }
}
