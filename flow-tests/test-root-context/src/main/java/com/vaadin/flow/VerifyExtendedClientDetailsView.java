package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.VerifyExtendedClientDetailsView")
public class VerifyExtendedClientDetailsView extends Div {

    public VerifyExtendedClientDetailsView() {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details ->{
            addSpan("screenWidth", details.getScreenWidth());
            addSpan("screenHeight", details.getScreenHeight());
            addSpan("timezoneOffset", details.getTimezoneOffset());
            addSpan("timeZoneId", details.getTimeZoneId());
            addSpan("rawTimezoneOffset", details.getRawTimezoneOffset());
            addSpan("DSTSavings", details.getDSTSavings());
            addSpan("DSTInEffect", details.isDSTInEffect());
            addSpan("currentDate", details.getCurrentDate());
            addSpan("touchDevice", details.isTouchDevice());
            addSpan("windowName", details.getWindowName());
        });
    }

    private void addSpan(String name, Object value) {
        Span span = new Span(value.toString());
        span.setId(name);
        add(span);

    }
}
