/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.util.ReflectTools;
import com.vaadin.router.Location;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.PageTitle;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIInternals.JavaScriptInvocation;
import com.vaadin.ui.common.HasText;

public class ViewRendererTest {

    private final static String ANOTHER_VIEW_TITLE = "test";
    private final static String DYNAMIC_VIEW_TITLE = "dynamic";

    private static final ThreadLocal<Boolean> blockNewViewInstances = new ThreadLocal<>();

    public static class TestView implements View {
        private Element element = ElementFactory.createDiv();
        private List<Location> locations = new ArrayList<>();
        private String namePlaceholderValue;
        private String wildcardValue;

        public TestView() {
            Assert.assertFalse("View instance creation is not allowed",
                    Boolean.TRUE.equals(blockNewViewInstances.get()));
        }

        @Override
        public final Element getElement() {
            return element;
        }

        @Override
        public void onLocationChange(LocationChangeEvent event) {
            locations.add(event.getLocation());
            namePlaceholderValue = event.getPathParameter("name");
            wildcardValue = event.getPathWildcard();
        }
    }

    @PageTitle(ANOTHER_VIEW_TITLE)
    public static class AnotherTestView extends TestView {

    }

    @PageTitle("not used")
    public static class DynamicTitleView extends TestView {

        @Override
        public String getTitle(LocationChangeEvent event) {
            return DYNAMIC_VIEW_TITLE;
        }
    }

    public static class NullTitleView extends TestView {

        @Override
        public String getTitle(LocationChangeEvent event) {
            return null;
        }
    }

    @PageTitle("foobar")
    public static class ParentView extends TestView implements HasChildView {
        @Override
        public void setChildView(View childView) {
            if (childView != null) {
                Assert.assertNotEquals(
                        "setChildView should not be called if the child has not changed",
                        getElement(), childView.getElement().getParent());
            }

            getElement().removeAllChildren();
            if (childView != null) {
                Element element = childView.getElement();

                element.removeFromParent();
                getElement().appendChild(element);
            }
        }
    }

    public static class ErrorView extends TestView implements HasText {

        public ErrorView() {
            setText("custom errorview");
        }
    }

    public static class AnotherParentView extends ParentView {
    }

    public static class StatusCodeView extends TestView {
        @Override
        public void onLocationChange(LocationChangeEvent event) {
            event.setStatusCode(event.getStatusCode() + 1);
        }
    }

    public static class RerouteView extends TestView {
        @Override
        public void onLocationChange(LocationChangeEvent event) {
            event.rerouteToErrorView();
        }
    }

    private final Router router = new Router();
    private final UI ui = new UI();
    private final NavigationEvent dummyEvent = new NavigationEvent(router,
            new Location(""), ui, NavigationTrigger.PROGRAMMATIC);

    @Test
    public void showSimpleView() {
        new TestViewRenderer(TestView.class).handle(dummyEvent);

        List<View> viewChain = ui.getInternals().getActiveViewChain();
        Assert.assertEquals(1, viewChain.size());

        View viewInstance = viewChain.get(0);
        Assert.assertSame(TestView.class, viewInstance.getClass());

        Assert.assertEquals(ui.getElement(),
                viewInstance.getElement().getParent());
        Assert.assertEquals(1, ui.getElement().getChildCount());

        Assert.assertEquals(Arrays.asList(dummyEvent.getLocation()),
                ((TestView) viewInstance).locations);
    }

    @Test
    public void showNestedView() {
        new TestViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> viewChain = ui.getInternals().getActiveViewChain();
        Assert.assertEquals(3, viewChain.size());
        Assert.assertEquals(
                Arrays.asList(TestView.class, ParentView.class,
                        AnotherParentView.class),
                viewChain.stream().map(Object::getClass)
                        .collect(Collectors.toList()));

        Element element = null;
        for (View view : viewChain) {
            Assert.assertEquals(Arrays.asList(dummyEvent.getLocation()),
                    ((TestView) view).locations);

            Element viewElement = view.getElement();
            if (element != null) {
                Assert.assertEquals(viewElement, element.getParent());
            }
            element = viewElement;
        }
        Assert.assertEquals(ui.getElement(), element.getParent());
        Assert.assertEquals(1, ui.getElement().getChildCount());
    }

    @Test
    public void reuseSingleView() {
        new TestViewRenderer(TestView.class).handle(dummyEvent);

        List<View> firstChain = ui.getInternals().getActiveViewChain();
        TestView view = (TestView) firstChain.get(0);

        Assert.assertEquals(1, view.locations.size());

        new TestViewRenderer(TestView.class).handle(dummyEvent);

        Assert.assertEquals(2, view.locations.size());

        List<View> secondChain = ui.getInternals().getActiveViewChain();

        Assert.assertNotSame(firstChain, secondChain);
        Assert.assertSame(view, secondChain.get(0));
    }

    @Test
    public void reuseFirstParentView() {
        new TestViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> firstChain = ui.getInternals().getActiveViewChain();

        new TestViewRenderer(AnotherTestView.class, AnotherParentView.class)
                .handle(dummyEvent);

        List<View> secondChain = ui.getInternals().getActiveViewChain();

        // Last item in each chain should be reused
        Assert.assertSame(firstChain.get(2), secondChain.get(1));
    }

    @Test
    public void testReuse_orderChanged() {
        new TestViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> firstChain = ui.getInternals().getActiveViewChain();

        new TestViewRenderer(TestView.class, AnotherParentView.class,
                ParentView.class).handle(dummyEvent);

        List<View> secondChain = ui.getInternals().getActiveViewChain();

        Assert.assertEquals(Arrays.asList(firstChain.get(0), firstChain.get(2),
                firstChain.get(1)), secondChain);
    }

    @Test
    public void testReuseAllViews() {
        new TestViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        try {
            blockNewViewInstances.set(Boolean.TRUE);
            // setChildView and view constructors throws if invoked
            new TestViewRenderer(TestView.class, ParentView.class,
                    AnotherParentView.class).handle(dummyEvent);
        } finally {
            blockNewViewInstances.remove();
        }
    }

    @Test
    public void testRemoveChildView() {
        new TestViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        ParentView parentView = (ParentView) ui.getInternals()
                .getActiveViewChain().get(1);

        new TestViewRenderer(ParentView.class, AnotherParentView.class)
                .handle(dummyEvent);

        Assert.assertEquals(0, parentView.getElement().getChildCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameTypeTwice_constructorThrows() {
        new TestViewRenderer(ParentView.class, ParentView.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameParentTypeTwice_constructorThrows() {
        new TestViewRenderer(TestView.class, ParentView.class,
                ParentView.class);
    }

    @Test
    public void routeParametersInEvent() {
        router.reconfigure(c -> c.setRoute("foo/{name}/*", TestView.class));
        router.navigate(ui, new Location("foo/bar/baz/"),
                NavigationTrigger.PROGRAMMATIC);
        TestView testView = (TestView) ui.getInternals().getActiveViewChain()
                .get(0);

        Assert.assertEquals("bar", testView.namePlaceholderValue);
        Assert.assertEquals("baz/", testView.wildcardValue);
    }

    @Test
    public void testViewTitle_titleAnnotation_titleUpdated() {
        new TestViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate(ANOTHER_VIEW_TITLE);
    }

    @Test
    public void testViewTitle_titleSetPreviouslyButNotDefinedForNextView_emptyTitleSet() {
        new TestViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate(ANOTHER_VIEW_TITLE);

        new TestViewRenderer(TestView.class).handle(dummyEvent);

        verifyViewTitleUpdate("");
    }

    @Test(expected = AssertionError.class)
    public void testViewTitle_nullTitleReturned_noTitleSet() {
        new TestViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate(ANOTHER_VIEW_TITLE);

        new TestViewRenderer(NullTitleView.class).handle(dummyEvent);
    }

    @Test
    public void testViewDynamicTitle() {
        new TestViewRenderer(DynamicTitleView.class).handle(dummyEvent);

        verifyViewTitleUpdate(DYNAMIC_VIEW_TITLE);
    }

    @Test
    public void testViewTitle_onlyParentHasTitle_defaultTitleUsed() {
        new TestViewRenderer(TestView.class, ParentView.class)
                .handle(dummyEvent);

        verifyViewTitleUpdate("");
    }

    @Test
    public void testViewTitle_customPageTitle_generator_isAlwaysUsed() {
        setPageTitleGenerator(lce -> "foobar");

        new TestViewRenderer(DynamicTitleView.class).handle(dummyEvent);

        verifyViewTitleUpdate("foobar");

        setPageTitleGenerator(lce -> "akbar");

        new TestViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate("akbar");

        setPageTitleGenerator(new DefaultPageTitleGenerator());

        new TestViewRenderer(DynamicTitleView.class).handle(dummyEvent);

        verifyViewTitleUpdate(DYNAMIC_VIEW_TITLE);
    }

    @Test(expected = AssertionError.class)
    public void testViewTitle_nullPageTitleGenerated_noTitleUpdate() {
        setPageTitleGenerator(lce -> null);

        new TestViewRenderer(DynamicTitleView.class).handle(dummyEvent);
    }

    @Test
    public void testViewInstantiationCustomization() {
        // override default implementation of reusing the views if possible
        ViewRenderer renderer = new TestViewRenderer(TestView.class,
                ParentView.class) {

            @Override
            protected <T extends View> T getView(Class<T> viewType,
                    NavigationEvent event) {
                // always return a new view
                return ReflectTools.createInstance(viewType);
            }
        };
        renderer.handle(dummyEvent);

        View view1 = dummyEvent.getUI().getInternals().getActiveViewChain()
                .get(0);
        View parentView1 = dummyEvent.getUI().getInternals()
                .getActiveViewChain().get(1);

        renderer.handle(dummyEvent);

        View view2 = dummyEvent.getUI().getInternals().getActiveViewChain()
                .get(0);
        View parentView2 = dummyEvent.getUI().getInternals()
                .getActiveViewChain().get(1);

        Assert.assertNotSame(view1, view2);
        Assert.assertNotSame(parentView1, parentView2);
    }

    @Test
    public void testStatusCode() {
        TestViewRenderer renderer = new TestViewRenderer(StatusCodeView.class);

        int statusCode = renderer.handle(dummyEvent);

        // StatusCodeView increments default status code with 1
        Assert.assertEquals(201, statusCode);
    }

    @Test
    public void testViewReroute() {
        TestViewRenderer renderer = new TestViewRenderer(RerouteView.class);

        int statusCode = renderer.handle(dummyEvent);

        Assert.assertEquals(404, statusCode);

        List<View> activeViewChain = ui.getInternals().getActiveViewChain();

        Assert.assertEquals(1, activeViewChain.size());
        Assert.assertSame(DefaultErrorView.class,
                activeViewChain.get(0).getClass());
    }

    private void setPageTitleGenerator(PageTitleGenerator generator) {
        dummyEvent.getSource()
                .reconfigure(conf -> conf.setPageTitleGenerator(generator));
    }

    private void verifyViewTitleUpdate(String pageTitle) {
        List<JavaScriptInvocation> jsInvocations = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        Assert.assertEquals("Page.setTitle should use title from annotation",
                pageTitle, jsInvocations.get(0).getParameters().get(0));
    }
}
