package com.vaadin.flow.uitest.ui.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.uitest.components.NativeRadioButtonGroup;

public class PushConfigurator extends Div {
    private NativeRadioButtonGroup<PushMode> pushMode = new NativeRadioButtonGroup<>(
            "Push mode");
    private NativeRadioButtonGroup<Transport> transport = new NativeRadioButtonGroup<>(
            "Transport");
    private NativeRadioButtonGroup<Transport> fallbackTransport = new NativeRadioButtonGroup<>(
            "Fallback");
    private Input parameter = new Input();
    private Input value = new Input();
    private NativeButton set = new NativeButton("Set");
    private Div paramValue = new Div();
    private Div vl = new Div();
    private UI ui;

    private Pre status = new Pre();

    public PushConfigurator(UI ui) {
        this.ui = ui;

        pushMode.setId("push-mode");
        transport.setId("transport");
        fallbackTransport.setId("fallback");

        parameter.setPlaceholder("Parameter");
        parameter.setId("parameter");

        value.setPlaceholder("Value");
        value.setId("value");

        set.setId("set");

        paramValue.getStyle().set("display", "block");
        status.getStyle().set("display", "block");

        construct();
        refreshStatus();
    }

    private void refreshStatus() {
        PushConfiguration pc = ui.getPushConfiguration();
        String value = "";
        List<String> names = new ArrayList<>();
        names.addAll(pc.getParameterNames());
        Collections.sort(names);
        for (String param : names) {
            value += param + ": " + pc.getParameter(param) + "\n";
        }
        status.setText(value);
    }

    private void construct() {
        for (PushMode mode : PushMode.values()) {
            pushMode.addOption(mode.name(), mode);
        }

        for (Transport transp : Transport.values()) {
            transport.addOption(transp.name(), transp);
            fallbackTransport.addOption(transp.name(), transp);
        }

        listeners();

        paramValue.add(parameter, value, set);
        status.setId("status");
        vl.add(pushMode, transport, fallbackTransport, paramValue,
                new Html("<hr></hr>"), status);
        add(vl);

    }

    private void listeners() {
        pushMode.addValueChangeListener(value -> {
            ui.getPushConfiguration().setPushMode(value);
            refreshStatus();
        });

        transport.addValueChangeListener(value -> {
            ui.getPushConfiguration().setTransport(value);
            refreshStatus();
        });

        fallbackTransport.addValueChangeListener(value -> {
            ui.getPushConfiguration().setFallbackTransport(value);
            refreshStatus();
        });

        set.addClickListener(event -> {
            ui.getPushConfiguration().setParameter(parameter.getValue(),
                    value.getValue());
            refreshStatus();
        });
    }
}