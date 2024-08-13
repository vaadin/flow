package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.RefreshCurrentRouteView", layout = RefreshCurrentRouteLayout.class)
public class RefreshCurrentRouteView extends Div implements BeforeEnterObserver,
        BeforeLeaveObserver, AfterNavigationObserver {

    final static String ID = "id";
    final static String ATTACHCOUNTER_ID = "attachcounter";
    final static String DETACHCOUNTER_ID = "detachcounter";
    final static String AFTERNAVCOUNTER_ID = "afternavcounter";
    final static String BEFOREENTERCOUNTER_ID = "beforeentercounter";
    final static String BEFORELEAVECOUNTER_ID = "beforeleavecounter";
    final static String NAVIGATE_ID = "navigate";
    final static String REFRESH_ID = "refresh";
    final static String REFRESH_LAYOUTS_ID = "refreshlayouts";

    private int attach, detach, afterNav, beforeEnter, beforeLeave;
    private final Div id, attachCounter, detachCounter, afterNavCounter,
            beforeEnterCounter, beforeLeaveCounter;

    public RefreshCurrentRouteView() {
        final String uniqueId = UUID.randomUUID().toString();
        id = createCounterSpan(ID);
        id.setText(uniqueId);

        attachCounter = createCounterSpan(ATTACHCOUNTER_ID);
        detachCounter = createCounterSpan(DETACHCOUNTER_ID);
        afterNavCounter = createCounterSpan(AFTERNAVCOUNTER_ID);
        beforeEnterCounter = createCounterSpan(BEFOREENTERCOUNTER_ID);
        beforeLeaveCounter = createCounterSpan(BEFORELEAVECOUNTER_ID);

        NativeButton navigate = new NativeButton("Navigate to this view",
                e -> UI.getCurrent().navigate(getNavigationTarget()));
        navigate.setId(NAVIGATE_ID);
        add(navigate);

        NativeButton refresh = new NativeButton("Refresh this view",
                e -> UI.getCurrent().refreshCurrentRoute(false));
        refresh.setId(REFRESH_ID);
        add(refresh);

        refresh = new NativeButton("Refresh this view and layouts",
                e -> UI.getCurrent().refreshCurrentRoute(true));
        refresh.setId(REFRESH_LAYOUTS_ID);
        add(refresh);
    }

    protected String getNavigationTarget() {
        return "com.vaadin.flow.uitest.ui.RefreshCurrentRouteView";
    }

    private Div createCounterSpan(String id) {
        Div counter = new Div();
        counter.setId(id);
        counter.setText("0");
        add(counter);
        return counter;
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        attachCounter.setText(Integer.toString(++attach));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        afterNavCounter.setText(Integer.toString(++afterNav));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        beforeEnterCounter.setText(Integer.toString(++beforeEnter));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        beforeLeaveCounter.setText(Integer.toString(++beforeLeave));
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        detachCounter.setText(Integer.toString(++detach));
    }
}
