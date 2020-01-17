package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NativeJSRequestLifecycleEventsFireIT extends ChromeBrowserTest {

    private WebElement imgClickCount;
    private WebElement reqStartCount;
    private WebElement reqRecCount;
    private WebElement vaadinDoneCount;
    private WebElement image;
    private WebElement imageMultiClick;

    @Override
    public void setup() throws Exception {
        super.setup();

        open();

        imgClickCount = findElement(By.id("imgClickCount"));
        reqStartCount = findElement(By.id("reqStartCount"));
        reqRecCount = findElement(By.id("reqRecCount"));
        vaadinDoneCount = findElement(By.id("vaadinDoneCount"));
        image = findElement(By.id("image"));
        imageMultiClick = findElement(By.id("imageMultiClick"));
    }

    @Test
    public void testRequestEventsFire_verifyEventsFireInSimpleCase() {

        assertInitialState();

        image.click();

        Assert.assertEquals("1", imgClickCount.getText());
        Assert.assertEquals("1", reqStartCount.getText());
        Assert.assertEquals("1", reqRecCount.getText());
        // The done listener will fire once after the request handling where we
        // add it and once after all pending tasks are done.
        Assert.assertEquals("2", vaadinDoneCount.getText());
    }

    @Test
    public void testRequestEventsFire_pendingEventsShouldProcessBeforeDoneIsFired() {

        assertInitialState();

        // Simulates the user clicking while a server request is pending.
        // I.e. the first click is immediate while the 3 later ones
        // are queued up by Vaadin. When the first request returns we should
        // get a request received event, however instead of firing the
        // "all processing done" event, Vaadin will send the pending
        // RPC's to the server (i.e. ApplicationConnection.isActive()
        // returns true as things are queued up)
        //
        // This checks that while the request "start" and "received" events
        // are triggered as requests are sent and received, the
        // "all pending tasks done" event is only triggered once Vaadin
        // actually has processed all pending events.
        imageMultiClick.click();

        // A total of 4 clicks should happen on the image.
        // First is immediate, while server sleeps for 500ms,
        // second click happens 10ms later and third 100ms later
        // after roughly 500ms the server returns and these two queued events
        // are sent to the server which again sleeps for 500ms.
        // After a total of 800ms, a final 4th click happens which is queued up.
        // the server returns after 500ms again (total 1000ms) but there's more
        // events queued again so a third request is sent.
        Assert.assertEquals("4", imgClickCount.getText());

        // First request is immediate, second one will contain 2 additional
        // clicks that were queued up, third will contain the last click
        Assert.assertEquals("3", reqStartCount.getText());
        Assert.assertEquals("3", reqRecCount.getText());

        // The done listener should increment by 1 during this test (initial
        // state is 1).
        //
        // The done listener will fire once after the request handling where we
        // add it and once after all pending tasks are done. Hence, if this had
        // been done without Vaadin queuing up events, then the "doneCount"
        // would be requestCount+1
        Assert.assertEquals("2", vaadinDoneCount.getText());

    }

    private void assertInitialState() {
        // Req start and received will fire before we have added the DOM
        // listeners as they are added by the server execJS...
        Assert.assertEquals("0", imgClickCount.getText());
        Assert.assertEquals("0", reqStartCount.getText());
        Assert.assertEquals("0", reqRecCount.getText());
        // The done listener will fire once after the request handling where we
        // add it...
        Assert.assertEquals("1", vaadinDoneCount.getText());
    }

}
