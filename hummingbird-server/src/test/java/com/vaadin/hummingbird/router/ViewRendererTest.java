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

import com.vaadin.hummingbird.dom.Element;

public class ViewRendererTest {

    public static class TestView implements View {
        private Element element = new Element("div");
        private List<Location> locations = new ArrayList<>();

        @Override
        public final Element getElement() {
            return element;
        }

        @Override
        public void onLocationChange(LocationChangeEvent event) {
            locations.add(event.getLocation());
        }
    }

    public static class AnotherTestView extends TestView {

    }

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
    private final RouterUI ui = new RouterUI();
    private final NavigationEvent dummyEvent = new NavigationEvent(router,
            new Location(""), ui);

    @Test
    public void showSimpleView() {
        new ViewRenderer(TestView.class).handle(dummyEvent);

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
        new ViewRenderer(TestView.class, ParentView.class,
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
        new ViewRenderer(TestView.class).handle(dummyEvent);

        List<View> firstChain = ui.getActiveViewChain();
        TestView view = (TestView) firstChain.get(0);

        Assert.assertEquals(1, view.locations.size());

        new ViewRenderer(TestView.class).handle(dummyEvent);

        Assert.assertEquals(2, view.locations.size());

        List<View> secondChain = ui.getActiveViewChain();

        Assert.assertNotSame(firstChain, secondChain);
        Assert.assertSame(view, secondChain.get(0));
    }

    @Test
    public void reuseFirstParentView() {
        new ViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> firstChain = ui.getActiveViewChain();

        new ViewRenderer(AnotherTestView.class, AnotherParentView.class)
                .handle(dummyEvent);

        List<View> secondChain = ui.getActiveViewChain();

        // Last item in each chain should be reused
        Assert.assertSame(firstChain.get(2), secondChain.get(1));
    }

    @Test
    public void testReuse_orderChanged() {
        new ViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        List<View> firstChain = ui.getActiveViewChain();

        new ViewRenderer(TestView.class, AnotherParentView.class,
                ParentView.class).handle(dummyEvent);

        List<View> secondChain = ui.getActiveViewChain();

        Assert.assertEquals(Arrays.asList(firstChain.get(0), firstChain.get(2),
                firstChain.get(1)), secondChain);
    }

    @Test
    public void testReuseAllViews() {
        new ViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        // setChildView throws if it's invoked
        new ViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);
    }

    @Test
    public void testRemoveChildView() {
        new ViewRenderer(TestView.class, ParentView.class,
                AnotherParentView.class).handle(dummyEvent);

        ParentView parentView = (ParentView) ui.getActiveViewChain().get(1);

        new ViewRenderer(ParentView.class, AnotherParentView.class)
                .handle(dummyEvent);

        Assert.assertEquals(0, parentView.getElement().getChildCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameTypeTwice_constructorThrows() {
        new ViewRenderer(ParentView.class, ParentView.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameParentTypeTwice_constructorThrows() {
        new ViewRenderer(TestView.class, ParentView.class, ParentView.class);
    }

}
