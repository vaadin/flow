/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("ui-scope")
public class UIScopeTarget extends Div {

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public static class InnerComponent extends Div {

        @Autowired
        private UIScopedBean bean;

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            NativeLabel label = new NativeLabel(String.valueOf(bean.getUid()));
            label.setId("inner");
            add(label);
        }
    }

    public UIScopeTarget(@Autowired UIScopedBean bean,
            @Autowired InnerComponent component,
            @Autowired ApplicationContext ctx) {
        NativeLabel label = new NativeLabel(String.valueOf(bean.getUid()));
        label.setId("main");
        add(label);

        add(component);

        UI ui = UI.getCurrent();

        AtomicBoolean detached = new AtomicBoolean(false);
        AtomicBoolean attached = new AtomicBoolean(false);

        NativeButton resynchronize = new NativeButton("Resynchronize", e -> {
            detached.set(false);
            attached.set(false);

            ui.addDetachListener(event -> {
                detached.set(true);
                event.unregisterListener();
            });
            ui.addAttachListener(event -> {
                attached.set(true);
                event.unregisterListener();
            });

            // simulate resynchronization
            ui.getInternals().getStateTree().prepareForResync();
            ui.getInternals().getDependencyList().clearPendingSendToClient();
        });
        resynchronize.setId("resynchronize");
        add(resynchronize);

        NativeButton checkStatus = new NativeButton("Check status", ev -> {
            if (detached.get()) {
                Span detachedText = new Span("UI was detached.");
                detachedText.setId("ui-was-detached");
                add(detachedText);
            }
            if (attached.get()) {
                Span attachedText = new Span("UI was attached.");
                attachedText.setId("ui-was-attached");
                add(attachedText);
            }
            label.setText(
                    String.valueOf(ctx.getBean(UIScopedBean.class).getUid()));

        });
        checkStatus.setId("status-check");
        add(checkStatus);
    }

}
