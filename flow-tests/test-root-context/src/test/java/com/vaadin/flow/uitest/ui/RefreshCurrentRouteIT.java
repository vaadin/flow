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
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteView.REFRESH_ID;

public class RefreshCurrentRouteIT extends AbstractStreamResourceIT {

    @Test
    public void navigateToSameRoute_ensureSameInstanceAndCorrectEventCounts() {
        open();

        final String originalId = getString(ID);

        assertInitialEventCounters();

        $(NativeButtonElement.class).id(NAVIGATE_ID).click();

        Assert.assertEquals(getString(ID), originalId);

        // Nav events should have happened, attach/detach should not
        Assert.assertEquals(1, getInt(ATTACHCOUNTER_ID));
        Assert.assertEquals(0, getInt(DETACHCOUNTER_ID));
        Assert.assertEquals(2, getInt(AFTERNAVCOUNTER_ID));
        Assert.assertEquals(2, getInt(BEFOREENTERCOUNTER_ID));
        Assert.assertEquals(1, getInt(BEFORELEAVECOUNTER_ID));
    }

    @Test
    public void refreshCurrentRoute_ensureNewInstanceAndCorrectEventCounts() {
        open();

        final String originalId = getString(ID);

        assertInitialEventCounters();

        $(NativeButtonElement.class).id(REFRESH_ID).click();

        // UUID should be new since refresh creates new instance
        Assert.assertNotEquals(getString(ID), originalId);

        // Event counters should equal original values
        assertInitialEventCounters();
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