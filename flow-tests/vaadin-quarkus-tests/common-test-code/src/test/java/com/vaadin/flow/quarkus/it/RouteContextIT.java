/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it;

import java.io.IOException;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.quarkus.it.routecontext.ApartBean;
import com.vaadin.flow.quarkus.it.routecontext.AssignedBean;
import com.vaadin.flow.quarkus.it.routecontext.BeanNoOwner;
import com.vaadin.flow.quarkus.it.routecontext.DetailApartView;
import com.vaadin.flow.quarkus.it.routecontext.DetailAssignedView;
import com.vaadin.flow.quarkus.it.routecontext.ErrorHandlerView;
import com.vaadin.flow.quarkus.it.routecontext.ErrorHandlerView.ErrorBean1;
import com.vaadin.flow.quarkus.it.routecontext.ErrorHandlerView.ErrorBean2;
import com.vaadin.flow.quarkus.it.routecontext.ErrorParentView;
import com.vaadin.flow.quarkus.it.routecontext.ErrorView;
import com.vaadin.flow.quarkus.it.routecontext.EventView;
import com.vaadin.flow.quarkus.it.routecontext.MainLayout;
import com.vaadin.flow.quarkus.it.routecontext.MasterView;
import com.vaadin.flow.quarkus.it.routecontext.PostponeView;
import com.vaadin.flow.quarkus.it.routecontext.PreserveOnRefreshBean;
import com.vaadin.flow.quarkus.it.routecontext.ProxiedView;
import com.vaadin.flow.quarkus.it.routecontext.ProxiedViewPresenter;
import com.vaadin.flow.quarkus.it.routecontext.RerouteView;
import com.vaadin.flow.quarkus.it.routecontext.RootView;

@QuarkusIntegrationTest
public class RouteContextIT extends AbstractCdiIT {

    private String uiId;

    @Override
    protected String getTestPath() {
        return "/route";
    }

    @BeforeEach
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

        Assertions.assertFalse(isElementPresent(By.id("invalid-injection")));
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

        Assertions.assertEquals(beanData,
                findElement(By.id("preserve-on-refresh")).getText());
    }

    @Test
    public void navigateToViewWhichThrows_beansInsideErrorViewArePreservedinScope()
            throws IOException {
        follow(RootView.ERROR);

        assertConstructed(ErrorBean1.class, 1);
        assertDestroyed(ErrorBean1.class, 0);

        assertConstructed(ErrorBean2.class, 0);
        assertDestroyed(ErrorBean2.class, 0);

        findElement(By.id("switch-content")).click();

        assertDestroyed(ErrorBean1.class, 0);
        assertConstructed(ErrorBean2.class, 1);
        assertDestroyed(ErrorBean2.class, 0);

        findElement(By.id("switch-content")).click();

        assertConstructed(ErrorBean1.class, 1);
        assertConstructed(ErrorBean2.class, 1);
        assertDestroyed(ErrorBean1.class, 0);
        assertDestroyed(ErrorBean2.class, 0);
    }

    @Test
    public void proxiedView_presenterObservesEventsAfterNavigation()
            throws IOException {
        follow(RootView.PROXIED);
        // The presenter should have observed the "afterNavigation" event
        // fired by the view in its afterNavigation() callback
        assertTextEquals("afterNavigation", ProxiedView.PRESENTER_LABEL);
        // Presenter bean should be constructed and NOT destroyed
        assertConstructed(ProxiedViewPresenter.class, 1);
        assertDestroyed(ProxiedViewPresenter.class, 0);
    }

    private void assertRootViewIsDisplayed() {
        assertTextEquals(uiId, MainLayout.UIID);
    }

    private void assertConstructed(Class beanClass, int count)
            throws IOException {
        Assertions.assertEquals(count,
                getCount(beanClass.getSimpleName() + "C" + uiId));
    }

    private void assertDestroyed(Class beanClass, int count)
            throws IOException {
        Assertions.assertEquals(count,
                getCount(beanClass.getSimpleName() + "D" + uiId));
    }

}
