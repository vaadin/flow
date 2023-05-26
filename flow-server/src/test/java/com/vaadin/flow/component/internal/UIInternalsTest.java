package com.vaadin.flow.component.internal;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

public class UIInternalsTest {

    @Mock
    UI ui;
    @Mock
    VaadinService vaadinService;

    UIInternals internals;

    @Route
    @Push
    @Tag(Tag.DIV)
    public static class RouteTarget extends Component implements RouterLayout {

    }

    @Route(value = "foo", layout = RouteTarget.class)
    @Tag(Tag.DIV)
    public static class RouteTarget1 extends Component {

    }

    @Tag(Tag.DIV)
    static class MainLayout extends Component implements RouterLayout {
        static String ID = "main-layout-id";

        public MainLayout() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    @ParentLayout(MainLayout.class)
    static class SubLayout extends Component implements RouterLayout {
        static String ID = "sub-layout-id";

        public SubLayout() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    @Route(value = "child", layout = SubLayout.class)
    static class FirstView extends Component {
        static String ID = "child-view-id";

        public FirstView() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    static class AnotherLayout extends Component implements RouterLayout {
        static String ID = "another-layout-id";

        public AnotherLayout() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    @Route(value = "another", layout = MainLayout.class)
    static class AnotherView extends Component {
        static String ID = "another-view-id";

        public AnotherView() {
            setId(ID);
        }
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);

        internals = new UIInternals(ui);
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                vaadinService);
        Mockito.when(vaadinService.getInstantiator())
                .thenReturn(new DefaultInstantiator(vaadinService));
        internals.setSession(session);
        Mockito.when(ui.getSession()).thenReturn(session);

        Mockito.when(ui.getInternals()).thenReturn(internals);
    }

    @Test
    public void dumpPendingJavaScriptInvocations_detachListenerRegisteredOnce() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        internals.addJavaScriptInvocation(
                new UIInternals.JavaScriptInvocation("", node));
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(Mockito.any());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_multipleInvocationPerNode_onlyOneDetachListenerRegistered() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        internals.addJavaScriptInvocation(
                new UIInternals.JavaScriptInvocation("1", node));
        internals.addJavaScriptInvocation(
                new UIInternals.JavaScriptInvocation("2", node));
        internals.addJavaScriptInvocation(
                new UIInternals.JavaScriptInvocation("3", node));
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(Mockito.any());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_registerOneDetachListenerPerNode() {
        StateNode node1 = Mockito.spy(new StateNode(ElementData.class));
        node1.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node1);
        internals.addJavaScriptInvocation(
                new UIInternals.JavaScriptInvocation("1", node1));

        StateNode node2 = Mockito.spy(new StateNode(ElementData.class));
        node2.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node2);
        internals.addJavaScriptInvocation(
                new UIInternals.JavaScriptInvocation("1", node2));

        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node1, Mockito.times(1))
                .addDetachListener(Mockito.any());
        Mockito.verify(node2, Mockito.times(1))
                .addDetachListener(Mockito.any());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_nodeDetached_pendingListPurged() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        UIInternals.JavaScriptInvocation invocation =
                new UIInternals.JavaScriptInvocation("", node);
        internals.addJavaScriptInvocation(invocation);
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(Mockito.any());

        node.setParent(null);

        Assert.assertEquals(0,
                internals.getPendingJavaScriptInvocations().size());
    }
}
