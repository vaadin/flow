/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.UIInternals.JavaScriptInvocation;
import com.vaadin.ui.UI;

public class ViewRendererTest {

    private final static String ANOTHER_VIEW_TITLE = "test";
    private final static String DYNAMIC_VIEW_TITLE = "dynamic";

    public static class TestView implements View {
        private Element element = ElementFactory.createDiv();
        private List<Location> locations = new ArrayList<>();
        private String namePlaceholderValue;
        private String wildcardValue;

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

    @Title(ANOTHER_VIEW_TITLE)
    public static class AnotherTestView extends TestView {

    }

    @Title("not used")
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

    @Title("foobar")
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

    public static class AnotherParentView extends ParentView {
    }

    private final Router router = new Router();
    private final UI ui = new UI();
    private final NavigationEvent dummyEvent = new NavigationEvent(router,
            new Location(""), ui);

    @Test
    public void showSimpleView() {
        new StaticViewRenderer(TestView.class).handle(dummyEvent);

        List<View> viewChain = ui.getActiveViewChain();
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
        new StaticViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> viewChain = ui.getActiveViewChain();
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
        new StaticViewRenderer(TestView.class).handle(dummyEvent);

        List<View> firstChain = ui.getActiveViewChain();
        TestView view = (TestView) firstChain.get(0);

        Assert.assertEquals(1, view.locations.size());

        new StaticViewRenderer(TestView.class).handle(dummyEvent);

        Assert.assertEquals(2, view.locations.size());

        List<View> secondChain = ui.getActiveViewChain();

        Assert.assertNotSame(firstChain, secondChain);
        Assert.assertSame(view, secondChain.get(0));
    }

    @Test
    public void reuseFirstParentView() {
        new StaticViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> firstChain = ui.getActiveViewChain();

        new StaticViewRenderer(AnotherTestView.class, AnotherParentView.class)
                .handle(dummyEvent);

        List<View> secondChain = ui.getActiveViewChain();

        // Last item in each chain should be reused
        Assert.assertSame(firstChain.get(2), secondChain.get(1));
    }

    @Test
    public void testReuse_orderChanged() {
        new StaticViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> firstChain = ui.getActiveViewChain();

        new StaticViewRenderer(TestView.class, AnotherParentView.class,
                ParentView.class).handle(dummyEvent);

        List<View> secondChain = ui.getActiveViewChain();

        Assert.assertEquals(Arrays.asList(firstChain.get(0), firstChain.get(2),
                firstChain.get(1)), secondChain);
    }

    @Test
    public void testReuseAllViews() {
        new StaticViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        // setChildView throws if it's invoked
        new StaticViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);
    }

    @Test
    public void testRemoveChildView() {
        new StaticViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        ParentView parentView = (ParentView) ui.getActiveViewChain().get(1);

        new StaticViewRenderer(ParentView.class, AnotherParentView.class)
                .handle(dummyEvent);

        Assert.assertEquals(0, parentView.getElement().getChildCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameTypeTwice_constructorThrows() {
        new StaticViewRenderer(ParentView.class, ParentView.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameParentTypeTwice_constructorThrows() {
        new StaticViewRenderer(TestView.class, ParentView.class,
                ParentView.class);
    }

    @Test
    public void routeParamtersInEvent() {
        router.reconfigure(c -> c.setRoute("foo/{name}/*", TestView.class));
        router.navigate(ui, new Location("foo/bar/baz/"));
        TestView testView = (TestView) ui.getActiveViewChain().get(0);

        Assert.assertEquals("bar", testView.namePlaceholderValue);
        Assert.assertEquals("baz/", testView.wildcardValue);
    }

    @Test
    public void testViewTitle_titleAnnotation_titleUpdated() {
        new StaticViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate(ANOTHER_VIEW_TITLE);
    }

    @Test
    public void testViewTitle_titleSetPreviouslyButNotDefinedForNextView_emptyTitleSet() {
        new StaticViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate(ANOTHER_VIEW_TITLE);

        new StaticViewRenderer(TestView.class).handle(dummyEvent);

        verifyViewTitleUpdate("");
    }

    @Test(expected = AssertionError.class)
    public void testViewTitle_nullTitleReturned_noTitleSet() {
        new StaticViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate(ANOTHER_VIEW_TITLE);

        new StaticViewRenderer(NullTitleView.class).handle(dummyEvent);
    }

    @Test
    public void testViewDynamicTitle() {
        new StaticViewRenderer(DynamicTitleView.class).handle(dummyEvent);

        verifyViewTitleUpdate(DYNAMIC_VIEW_TITLE);
    }

    @Test
    public void testViewTitle_onlyParentHasTitle_defaultTitleUsed() {
        new StaticViewRenderer(TestView.class, ParentView.class)
                .handle(dummyEvent);

        verifyViewTitleUpdate("");
    }

    @Test
    public void testViewTitle_customPageTitle_generator_isAlwaysUsed() {
        setPageTitleGenerator(lce -> "foobar");

        new StaticViewRenderer(DynamicTitleView.class).handle(dummyEvent);

        verifyViewTitleUpdate("foobar");

        setPageTitleGenerator(lce -> "akbar");

        new StaticViewRenderer(AnotherTestView.class).handle(dummyEvent);

        verifyViewTitleUpdate("akbar");

        setPageTitleGenerator(new DefaultPageTitleGenerator());

        new StaticViewRenderer(DynamicTitleView.class).handle(dummyEvent);

        verifyViewTitleUpdate(DYNAMIC_VIEW_TITLE);
    }

    @Test(expected = AssertionError.class)
    public void testViewTitle_nullPageTitleGenerated_noTitleUpdate() {
        setPageTitleGenerator(lce -> null);

        new StaticViewRenderer(DynamicTitleView.class).handle(dummyEvent);
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

    private void verifyNoTitleUpdate() {
        Assert.assertEquals("Page.setTitle should not have been triggered", 0,
                ui.getInternals().dumpPendingJavaScriptInvocations()
                        .size());
    }
}
