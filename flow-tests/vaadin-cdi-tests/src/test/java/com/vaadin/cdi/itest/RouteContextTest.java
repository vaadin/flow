/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi.itest;

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WindowType;

import com.vaadin.cdi.itest.routecontext.ApartBean;
import com.vaadin.cdi.itest.routecontext.AssignedBean;
import com.vaadin.cdi.itest.routecontext.BeanNoOwner;
import com.vaadin.cdi.itest.routecontext.CustomExceptionSubButton;
import com.vaadin.cdi.itest.routecontext.CustomExceptionSubDiv;
import com.vaadin.cdi.itest.routecontext.DetailApartView;
import com.vaadin.cdi.itest.routecontext.DetailAssignedView;
import com.vaadin.cdi.itest.routecontext.ErrorHandlerView;
import com.vaadin.cdi.itest.routecontext.ErrorParentView;
import com.vaadin.cdi.itest.routecontext.ErrorView;
import com.vaadin.cdi.itest.routecontext.EventObserverLayout;
import com.vaadin.cdi.itest.routecontext.EventView;
import com.vaadin.cdi.itest.routecontext.LayoutEventView;
import com.vaadin.cdi.itest.routecontext.LayoutEventView2;
import com.vaadin.cdi.itest.routecontext.LayoutScopedPlainView;
import com.vaadin.cdi.itest.routecontext.LayoutScopedPreservedView;
import com.vaadin.cdi.itest.routecontext.MainLayout;
import com.vaadin.cdi.itest.routecontext.MainLayoutBean;
import com.vaadin.cdi.itest.routecontext.MasterView;
import com.vaadin.cdi.itest.routecontext.PostponeView;
import com.vaadin.cdi.itest.routecontext.PreserveOnRefreshBean;
import com.vaadin.cdi.itest.routecontext.RerouteView;
import com.vaadin.cdi.itest.routecontext.RootView;

public class RouteContextTest extends AbstractCdiTest {

    private String uiId;

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("route-context",
                webArchive -> webArchive
                        .addPackage(MasterView.class.getPackage())
                        .addAsResource(new File("target/classes/META-INF")));
    }

    @Before
    public void setUp() throws Exception {
        resetCounts();
        open("");
        uiId = getText(MainLayout.UIID);
        assertConstructed(RootView.class, 1);
        assertDestroyed(RootView.class, 0);
        assertConstructed(RerouteView.class, 0);
        assertConstructed(MasterView.class, 0);
        assertConstructed(AssignedBean.class, 0);
        assertConstructed(ApartBean.class, 0);
        assertConstructed(DetailApartView.class, 0);
        assertConstructed(DetailAssignedView.class, 0);
        assertConstructed(ErrorParentView.class, 0);
        assertConstructed(ErrorHandlerView.class, 0);
    }

    @Test
    public void navigateFromRootToMasterReleasesRootInjectsEmptyBeans()
            throws IOException {
        follow(RootView.MASTER);
        assertTextEquals("", MasterView.ASSIGNED_BEAN_LABEL);

        assertConstructed(RootView.class, 1);
        assertDestroyed(RootView.class, 1);
        assertConstructed(MasterView.class, 1);
        assertDestroyed(MasterView.class, 0);
        assertConstructed(AssignedBean.class, 1);
        assertDestroyed(AssignedBean.class, 0);
        assertConstructed(ApartBean.class, 0);
        assertDestroyed(ApartBean.class, 0);
        assertConstructed(DetailApartView.class, 0);
        assertConstructed(DetailAssignedView.class, 0);
    }

    @Test
    public void navigationFromAssignedToMasterHoldsGroup() throws IOException {
        follow(RootView.MASTER);
        follow(MasterView.ASSIGNED);
        assertTextEquals("ASSIGNED", DetailAssignedView.BEAN_LABEL);

        follow(DetailAssignedView.MASTER);
        assertConstructed(MasterView.class, 1);
        assertDestroyed(MasterView.class, 0);
        assertConstructed(DetailAssignedView.class, 1);
        assertDestroyed(DetailAssignedView.class, 0);
        assertConstructed(DetailApartView.class, 0);

        assertTextEquals("ASSIGNED", MasterView.ASSIGNED_BEAN_LABEL);
    }

    @Test
    public void navigationFromApartToMasterReleasesGroup() throws IOException {
        follow(RootView.MASTER);
        follow(MasterView.APART);
        assertTextEquals("", MasterView.ASSIGNED_BEAN_LABEL);
        assertTextEquals("APART", DetailApartView.BEAN_LABEL);

        follow(DetailApartView.MASTER);
        assertConstructed(MasterView.class, 1);
        assertDestroyed(MasterView.class, 0);
        assertConstructed(DetailAssignedView.class, 0);
        assertDestroyed(DetailAssignedView.class, 0);
        assertConstructed(DetailApartView.class, 1);
        assertDestroyed(DetailApartView.class, 1);

        assertTextEquals("", MasterView.ASSIGNED_BEAN_LABEL);
    }

    @Test
    public void rerouteReleasesSource() throws IOException {
        follow(RootView.REROUTE);
        assertConstructed(RerouteView.class, 1);
        assertDestroyed(RerouteView.class, 1);

        assertRootViewIsDisplayed();
    }

    @Test
    public void postponedNavigationDoesNotCreateTarget() throws IOException {
        follow(RootView.POSTPONE);
        assertConstructed(RootView.class, 1);

        follow(PostponeView.POSTPONED_ROOT);
        assertConstructed(RootView.class, 1);
        assertDestroyed(RootView.class, 1);

        click(PostponeView.NAVIGATE);
        assertConstructed(RootView.class, 2);
        assertDestroyed(RootView.class, 1);
        assertRootViewIsDisplayed();
    }

    @Test
    public void eventObserved() {
        follow(RootView.EVENT);
        assertTextEquals("", EventView.OBSERVER_LABEL);

        click(EventView.FIRE);
        assertTextEquals("HELLO", EventView.OBSERVER_LABEL);
    }

    @Test
    public void eventObservedByLayout() {
        follow(RootView.LAYOUT_EVENT);
        assertTextEquals("", EventObserverLayout.EVENTS_COUNTER_LABEL);

        click(LayoutEventView.FIRE);
        assertTextEquals("EVENTS COUNT: 1",
                EventObserverLayout.EVENTS_COUNTER_LABEL);

        click(LayoutEventView.CHANGE_VIEW);
        assertTextEquals("Layout Event View 2", LayoutEventView2.VIEW_NAME);

        click(LayoutEventView2.FIRE);
        assertTextEquals("EVENTS COUNT: 2",
                EventObserverLayout.EVENTS_COUNTER_LABEL);

        click(LayoutEventView2.CHANGE_VIEW);
        assertTextEquals("Layout Event View 1", LayoutEventView.VIEW_NAME);

        click(LayoutEventView.FIRE);
        assertTextEquals("EVENTS COUNT: 3",
                EventObserverLayout.EVENTS_COUNTER_LABEL);
    }

    @Test
    public void errorHandlerIsScoped() throws IOException {
        follow(RootView.ERROR);
        assertConstructed(RootView.class, 1);
        assertDestroyed(RootView.class, 1);
        assertConstructed(ErrorView.class, 1);
        assertDestroyed(ErrorView.class, 1);
        assertConstructed(ErrorParentView.class, 1);
        assertDestroyed(ErrorParentView.class, 0);
        assertConstructed(ErrorHandlerView.class, 1);
        assertDestroyed(ErrorHandlerView.class, 0);

        follow(ErrorHandlerView.PARENT);
        assertConstructed(ErrorParentView.class, 1);
        assertDestroyed(ErrorParentView.class, 0);
        assertConstructed(ErrorHandlerView.class, 1);
        assertDestroyed(ErrorHandlerView.class, 0);

        follow(ErrorParentView.ROOT);
        assertConstructed(RootView.class, 2);
        assertDestroyed(RootView.class, 1);
        assertConstructed(ErrorParentView.class, 1);
        assertDestroyed(ErrorParentView.class, 1);
        assertConstructed(ErrorHandlerView.class, 1);
        assertDestroyed(ErrorHandlerView.class, 1);

        assertRootViewIsDisplayed();
    }

    @Test
    public void routeScopeDoesNotExist_injectionWithOwnerOutOfNavigationThrows_invalidViewIsNotRendered() {
        follow(MainLayout.INVALID);

        Assert.assertFalse(isElementPresent(By.id("invalid-injection")));
    }

    @Test
    public void beansWithNoOwner_preservedWithinTheSameRouteTarget_notPreservedAfterNavigation()
            throws IOException {
        follow(MainLayout.PARENT_NO_OWNER);

        assertConstructed(BeanNoOwner.class, 1);
        assertDestroyed(BeanNoOwner.class, 0);

        follow("child");

        assertDestroyed(BeanNoOwner.class, 0);

        follow("parent");

        assertConstructed(BeanNoOwner.class, 2);
        assertDestroyed(BeanNoOwner.class, 1);
    }

    @Test
    public void beanWithNoOwner_preservedWithinTheSameRoutingChain()
            throws IOException {
        follow(MainLayout.CHILD_NO_OWNER);

        assertConstructed(BeanNoOwner.class, 1);
        assertDestroyed(BeanNoOwner.class, 0);

        findElement(By.id("reset")).click();

        assertDestroyed(BeanNoOwner.class, 0);
    }

    @Test
    public void navigateToViewWhichThrows_beansInsideErrorViewArePreservedinScope()
            throws IOException {
        follow(RootView.ERROR);

        assertConstructed(CustomExceptionSubButton.class, 1);
        assertDestroyed(CustomExceptionSubButton.class, 0);

        assertConstructed(CustomExceptionSubDiv.class, 0);
        assertDestroyed(CustomExceptionSubDiv.class, 0);

        findElement(By.id("switch-content")).click();

        assertDestroyed(CustomExceptionSubButton.class, 0);
        assertConstructed(CustomExceptionSubDiv.class, 1);
        assertDestroyed(CustomExceptionSubDiv.class, 0);

        findElement(By.id("switch-content")).click();

        assertConstructed(CustomExceptionSubButton.class, 1);
        assertConstructed(CustomExceptionSubDiv.class, 1);
        assertDestroyed(CustomExceptionSubButton.class, 0);
        assertDestroyed(CustomExceptionSubDiv.class, 0);
    }

    @Test
    public void routeScopedBeanIsDestroyedOnNavigationOutOfViewAfterPreserveOnRefresh()
            throws IOException {
        follow(MainLayout.PRESERVE);

        assertConstructed(PreserveOnRefreshBean.class, 1);
        assertDestroyed(PreserveOnRefreshBean.class, 0);

        // refresh
        getDriver().get(getDriver().getCurrentUrl());

        // UI ID has to be updated: all bean creations/removals will be done
        // now within the new UI
        uiId = getText(MainLayout.UIID);

        // navigate out of the preserved view
        follow(MainLayout.PARENT_NO_OWNER);

        assertDestroyed(PreserveOnRefreshBean.class, 1);
    }

    @Test
    public void noPreserveOnRefresh_routeTargetIsRecreatedOnRefresh()
            throws IOException {
        getDriver().get(getDriver().getCurrentUrl());

        // UI ID has to be updated: all bean creations/removals will be done
        // now within the new UI
        uiId = getText(MainLayout.UIID);

        assertConstructed(RootView.class, 1);
        assertDestroyed(RootView.class, 0);
        assertRootViewIsRendered();
    }

    @Test
    public void noPreserveOnRefresh_otherUIWithSameWindowName_routeTargetIsNotShared()
            throws IOException {
        String url = getDriver().getCurrentUrl();
        String windowName = (String) executeScript("return window.name;");

        // Open a second tab and make it report the window name of the first
        // one, as a duplicated tab or a restored browser session does. Both
        // UIs are alive at the same time, so a route scope keyed by window
        // name would be shared between them.
        getDriver().switchTo().newWindow(WindowType.TAB);
        getDriver().get(url);
        executeScript("window.name = arguments[0];", windowName);
        // same origin reload, so the window name is retained
        getDriver().navigate().refresh();

        uiId = getText(MainLayout.UIID);

        assertConstructed(RootView.class, 1);
        assertDestroyed(RootView.class, 0);
        assertRootViewIsRendered();
    }

    @Test
    public void sharedLayout_navigateBetweenPreservedAndPlainChild_layoutBeanIsKept()
            throws IOException {
        follow(MainLayout.LAYOUT_PRESERVED);
        String beanData = getText(LayoutScopedPreservedView.LAYOUT_BEAN_LABEL);

        assertConstructed(MainLayoutBean.class, 1);
        assertDestroyed(MainLayoutBean.class, 0);

        // the layout instance is reused, so the beans it owns must not be
        // recreated even if the navigation chain is not preserved anymore
        follow(MainLayout.LAYOUT_PLAIN);

        Assert.assertEquals(beanData,
                getText(LayoutScopedPlainView.LAYOUT_BEAN_LABEL));
        assertConstructed(MainLayoutBean.class, 1);
        assertDestroyed(MainLayoutBean.class, 0);

        // ... and the other way around
        follow(MainLayout.LAYOUT_PRESERVED);

        Assert.assertEquals(beanData,
                getText(LayoutScopedPreservedView.LAYOUT_BEAN_LABEL));
        assertConstructed(MainLayoutBean.class, 1);
        assertDestroyed(MainLayoutBean.class, 0);
    }

    private void assertRootViewIsRendered() {
        Assert.assertNotNull(
                "The root view is expected to be rendered in the current UI",
                findElement(By.linkText(RootView.MASTER)));
    }

    @Test
    public void preserveOnRefresh_beanIsNotDestroyed() throws IOException {
        follow(MainLayout.PRESERVE);

        assertConstructed(PreserveOnRefreshBean.class, 1);
        assertDestroyed(PreserveOnRefreshBean.class, 0);

        String beanData = findElement(By.id("preserve-on-refresh")).getText();

        // refresh
        getDriver().get(getDriver().getCurrentUrl());

        // check that the bean has not been removed in the previous UI
        assertDestroyed(PreserveOnRefreshBean.class, 0);

        // UI ID has to be updated: all bean creations/removals will be done
        // now within the new UI
        uiId = getText(MainLayout.UIID);

        // the bean should not be destroyed with the new UI as well
        assertDestroyed(PreserveOnRefreshBean.class, 0);

        Assert.assertEquals(beanData,
                findElement(By.id("preserve-on-refresh")).getText());
    }

    private void assertRootViewIsDisplayed() {
        assertTextEquals(uiId, MainLayout.UIID);
    }

    private void assertConstructed(Class beanClass, int count)
            throws IOException {
        Assert.assertEquals(count,
                getCount(beanClass.getSimpleName() + "C" + uiId));
    }

    private void assertDestroyed(Class beanClass, int count)
            throws IOException {
        Assert.assertEquals(count,
                getCount(beanClass.getSimpleName() + "D" + uiId));
    }

}
