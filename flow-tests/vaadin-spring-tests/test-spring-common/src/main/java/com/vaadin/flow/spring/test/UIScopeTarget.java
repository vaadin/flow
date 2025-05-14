/*
 * Copyright 2000-2025 Vaadin Ltd.
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
