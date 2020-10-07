package com.vaadin.flow.uitest.ui.routing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

@Push
@Route("com.vaadin.flow.uitest.ui.routing.PushRouteWildcardParameterView")
public class PushRouteWildcardParameterView extends Div
        implements HasUrlParameter<String> {

    private final ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(1);

    public static final String LABEL_ID = "label";
    private final Span label = new Span();

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        UI ui = event.getUI();
        executor.schedule(() -> {
            ui.access(() -> {
                label.setId(LABEL_ID);
                add(label);
                label.setText(parameter);
            });
        }, 100, TimeUnit.MILLISECONDS);
    }
}
