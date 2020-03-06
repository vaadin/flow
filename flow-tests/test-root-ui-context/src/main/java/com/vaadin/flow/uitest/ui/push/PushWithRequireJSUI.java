package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;

@Push
public class PushWithRequireJSUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        Push push = getClass().getAnnotation(Push.class);

        getPushConfiguration().setPushMode(push.value());
        getPushConfiguration().setTransport(push.transport());

        // https://cdnjs.cloudflare.com/ajax/libs/require.js/2.1.20/require.min.js
        getPage().addJavaScript("require.min.js");
    }
}