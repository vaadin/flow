package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;

import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.AFTERNAVCOUNTER_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.ATTACHCOUNTER_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.BEFOREENTERCOUNTER_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.BEFORELEAVECOUNTER_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.DETACHCOUNTER_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.NAVIGATE_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.OPEN_MODALS_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.REFRESH_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.REFRESH_LAYOUTS_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteLayout.ROUTER_LAYOUT_ID;

public class RefreshCurrentRouteIT extends AbstractStreamResourceIT {

    @Test
    public void navigateToSameRoute_ensureSameInstanceAndCorrectEventCounts() {
        open();

        final String originalId = getString(ID);
        final String originalLayoutId = getString(ROUTER_LAYOUT_ID);

        assertInitialEventCounters();

        $(NativeButtonElement.class).id(NAVIGATE_ID).click();

        Assert.assertEquals(getString(ID), originalId);
        Assert.assertEquals(getString(ROUTER_LAYOUT_ID), originalLayoutId);

        // Nav events should have happened, attach/detach should not
        Assert.assertEquals(1, getInt(ATTACHCOUNTER_ID));
        Assert.assertEquals(0, getInt(DETACHCOUNTER_ID));
        Assert.assertEquals(2, getInt(AFTERNAVCOUNTER_ID));
        Assert.assertEquals(2, getInt(BEFOREENTERCOUNTER_ID));
        Assert.assertEquals(1, getInt(BEFORELEAVECOUNTER_ID));
    }

    @Test
    public void refreshCurrentRoute_ensureNewInstanceAndCorrectEventCounts_noNewLayout() {
        open();

        final String originalId = getString(ID);
        final String originalLayoutId = getString(ROUTER_LAYOUT_ID);

        assertInitialEventCounters();

        $(NativeButtonElement.class).id(REFRESH_ID).click();

        // UUID should be new since refresh creates new instance
        Assert.assertNotEquals(getString(ID), originalId);
        // Layout UUID should be same
        Assert.assertEquals(getString(ROUTER_LAYOUT_ID), originalLayoutId);

        // Event counters should equal original values
        assertInitialEventCounters();
    }

    @Test
    public void refreshCurrentRoute_ensureNewInstanceAndCorrectEventCounts_newLayout() {
        open();

        final String originalId = getString(ID);
        final String originalLayoutId = getString(ROUTER_LAYOUT_ID);

        assertInitialEventCounters();

        $(NativeButtonElement.class).id(REFRESH_LAYOUTS_ID).click();

        // UUID should be new since refresh creates new instance
        Assert.assertNotEquals(getString(ID), originalId);
        // UUID should be new since new layout instances were requested
        Assert.assertNotEquals(getString(ROUTER_LAYOUT_ID), originalLayoutId);

        // Event counters should equal original values
        assertInitialEventCounters();
    }

    public void refreshCurrentRoute_modalComponents_newRouteAndLayout() {
        open("modal=true");

        final String originalId = getString(ID);
        final String originalLayoutId = getString(ROUTER_LAYOUT_ID);

        assertInitialEventCounters();

        waitForElementPresent(By.id("modal-1"));
        waitForElementPresent(By.id("modal-2"));
        waitForElementPresent(By.id("modal-3"));
        $(NativeButtonElement.class).id("modal-3-refresh").click();

        // UUID should be new since refresh creates new instance
        Assert.assertNotEquals(getString(ID), originalId);
        // UUID should be new since new layout instances were requested
        Assert.assertNotEquals(getString(ROUTER_LAYOUT_ID), originalLayoutId);

        // Event counters should equal original values
        assertInitialEventCounters();

        waitForElementPresent(By.id("modal-1"));
        waitForElementPresent(By.id("modal-2"));
        waitForElementPresent(By.id("modal-3"));

        $(NativeButtonElement.class).id("modal-3-close").click();
        $(NativeButtonElement.class).id("modal-2-close").click();
        $(NativeButtonElement.class).id("modal-1-refresh").click();

        waitForElementPresent(By.id("modal-1"));
        waitForElementPresent(By.id("modal-2"));
        waitForElementPresent(By.id("modal-3"));
    }

    private void assertInitialEventCounters() {
        Assert.assertEquals(1, getInt(ATTACHCOUNTER_ID));
        Assert.assertEquals(0, getInt(DETACHCOUNTER_ID));
        Assert.assertEquals(1, getInt(AFTERNAVCOUNTER_ID));
        Assert.assertEquals(1, getInt(BEFOREENTERCOUNTER_ID));
        Assert.assertEquals(0, getInt(BEFORELEAVECOUNTER_ID));
    }

    private String getString(String id) {
        waitForElementPresent(By.id(id));
        return findElement(By.id(id)).getText();
    }

    private int getInt(String id) {
        return Integer.parseInt(getString(id));
    }
}
