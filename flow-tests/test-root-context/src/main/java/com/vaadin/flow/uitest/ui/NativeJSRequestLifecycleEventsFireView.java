package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.NativeJSRequestLifecycleEventsFireView", layout = ViewTestLayout.class)
public class NativeJSRequestLifecycleEventsFireView extends AbstractDivView {

    // Native DOM event identifiers for request related events
    private static final String VAADIN_REQUEST_START_EVENT = "vaadin-request-start";
    private static final String VAADIN_REQUEST_RECEIVED_EVENT = "vaadin-request-received";
    private static final String VAADIN_ALL_REQUEST_PROCESSING_DONE_EVENT = "vaadin-all-request-processing-done";

    public NativeJSRequestLifecycleEventsFireView() {

        Div message = new Div();
        message.setText("Image click count:");
        message.setId("message");
        add(message);

        Div imgClickCount = new Div();
        imgClickCount.setText("0");
        imgClickCount.setId("imgClickCount");
        add(imgClickCount);

        Div reqStartMessage = new Div();
        reqStartMessage.setText("Before req start fired times:");
        reqStartMessage.setId("reqStart");
        add(reqStartMessage);

        Div reqStartMessageCount = new Div();
        reqStartMessageCount.setText("0");
        reqStartMessageCount.setId("reqStartCount");
        add(reqStartMessageCount);

        Div reqRecMessage = new Div();
        reqRecMessage.setText("Before req received fired times:");
        reqRecMessage.setId("reqRec");
        add(reqRecMessage);

        Div reqRecMessageCount = new Div();
        reqRecMessageCount.setText("0");
        reqRecMessageCount.setId("reqRecCount");
        add(reqRecMessageCount);

        Div vaadinDoneMessage = new Div();
        vaadinDoneMessage.setText("Vaadin done fired times: ");
        vaadinDoneMessage.setId("vaadinDone");
        add(vaadinDoneMessage);

        Div vaadinDoneCount = new Div();
        vaadinDoneCount.setText("0");
        vaadinDoneCount.setId("vaadinDoneCount");
        add(vaadinDoneCount);

        Image image = new Image("", "IMAGE Single Click");
        image.getElement().getStyle().set("display", "block");
        image.setId("image");
        image.addClickListener(event -> {

            // Simulate slow server for test
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            imgClickCount.setText(
                    (Integer.parseInt(imgClickCount.getText()) + 1) + "");
        });
        add(image);

        Image imageMultiClick = new Image("", "IMAGE Multi Click");
        imageMultiClick.setId("imageMultiClick");
        add(imageMultiClick);

        UI ui = UI.getCurrent();
        ui.getPage().executeJs(
                "document.getElementById('imageMultiClick').addEventListener('click', e => {document.getElementById('image').click(); setTimeout( function() {document.getElementById('image').click();}, 10); setTimeout( function() {document.getElementById('image').click();}, 100); setTimeout( function() {document.getElementById('image').click();}, 800);}) ");

        // Req start and received will fire before we have added the DOM
        // listeners...
        addDOMListener(VAADIN_REQUEST_START_EVENT, "reqStartCount");
        addDOMListener(VAADIN_REQUEST_RECEIVED_EVENT, "reqRecCount");

        // The done listener will fire once after the request handling where we
        // add it...
        addDOMListener(VAADIN_ALL_REQUEST_PROCESSING_DONE_EVENT,
                "vaadinDoneCount");

        // To verify in test that the "vaadin-request-start" event has the
        // request payload in the details section
        Div requestStartDetails = new Div();
        requestStartDetails.setText("");
        requestStartDetails.setId("reqDetails");
        add(requestStartDetails);

        UI.getCurrent().getPage().executeJs(
                "document.addEventListener('vaadin-request-start', e => {document.getElementById('reqDetails').innerText = e.detail;});");

    }

    private void addDOMListener(String listenerId, String elToIncrementId) {
        UI ui = UI.getCurrent();
        ui.getPage()
                .executeJs("document.addEventListener(\"" + listenerId
                        + "\", e => document.getElementById('" + elToIncrementId
                        + "').innerText = (parseInt(document.getElementById('"
                        + elToIncrementId + "').innerText, 10) + 1) );");

    }
}
