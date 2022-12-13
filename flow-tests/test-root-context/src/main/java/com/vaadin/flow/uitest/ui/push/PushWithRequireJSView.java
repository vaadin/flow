package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@CustomPush
@Route("com.vaadin.flow.uitest.ui.push.PushWithRequireJSView")
public class PushWithRequireJSView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        CustomPush push = getClass().getAnnotation(CustomPush.class);

        ui.getPushConfiguration().setPushMode(push.value());
        ui.getPushConfiguration().setTransport(push.transport());

        // https://cdnjs.cloudflare.com/ajax/libs/require.js/2.1.20/require.min.js
        ui.getPage().addJavaScript("/require.min.js");
    }
}
