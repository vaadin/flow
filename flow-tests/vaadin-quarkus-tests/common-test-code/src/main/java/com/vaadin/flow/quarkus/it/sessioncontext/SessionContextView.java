/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.sessioncontext;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.quarkus.it.Counter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.quarkus.annotation.VaadinSessionScoped;

@Route("session")
public class SessionContextView extends Div {

    public static final String SETVALUEBTN_ID = "setvalbtn";
    public static final String VALUELABEL_ID = "label";
    public static final String VALUE = "session";
    public static final String INVALIDATEBTN_ID = "invalidatebtn";
    public static final String HTTP_INVALIDATEBTN_ID = "httpinvalidatebtn";
    public static final String EXPIREBTN_ID = "expirebtn";

    @Inject
    private SessionScopedBean sessionScopedBean;

    @PostConstruct
    private void init() {
        NativeButton setBtn = new NativeButton("set");
        setBtn.addClickListener(event -> sessionScopedBean.setValue(VALUE));
        setBtn.setId(SETVALUEBTN_ID);
        add(setBtn);

        NativeButton invalidateBtn = new NativeButton("invalidate");
        invalidateBtn
                .addClickListener(event -> VaadinSession.getCurrent().close());
        invalidateBtn.setId(INVALIDATEBTN_ID);
        add(invalidateBtn);

        NativeButton httpInvalidateBtn = new NativeButton("httpinvalidate");
        httpInvalidateBtn.addClickListener(
                event -> VaadinSession.getCurrent().getSession().invalidate());
        httpInvalidateBtn.setId(HTTP_INVALIDATEBTN_ID);
        add(httpInvalidateBtn);

        NativeButton expireBtn = new NativeButton("httpexpire");
        expireBtn.addClickListener(event -> VaadinSession.getCurrent()
                .getSession().setMaxInactiveInterval(1));
        expireBtn.setId(EXPIREBTN_ID);
        add(expireBtn);

        Span label = new Span();
        label.setText(sessionScopedBean.getValue()); // bean instantiated here
        label.setId(VALUELABEL_ID);
        add(label);
    }

    @VaadinSessionScoped
    // Like other vaadin scopes,
    // Serializable is mandatory only if you want a working session
    // serialization
    public static class SessionScopedBean {
        public static final String DESTROY_COUNT = "SessionScopedBeanDestroy";

        @Inject
        Counter counter;

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @PreDestroy
        private void preDestroy() {
            counter.increment(DESTROY_COUNT);
        }
    }
}
