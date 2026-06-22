package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectView.RedirectData;

public class RefreshCurrentRouteLayout implements RouterLayout {

    final static String ROUTER_LAYOUT_ID = "routerlayoutid";
    final static String LAYOUT_CREATION_COUNT_ID = "layout-creation-count";

    private Div layout = new Div();

    public RefreshCurrentRouteLayout() {
        final String uniqueId = UUID.randomUUID().toString();
        Div routerLayoutId = new Div(uniqueId);
        routerLayoutId.setId(ROUTER_LAYOUT_ID);
        layout.add(routerLayoutId);

        UI ui = UI.getCurrent();
        if (ui != null) {
            RedirectData data = ComponentUtil.getData(ui, RedirectData.class);
            if (data != null) {
                data.layoutCreationCount++;
                Div countDiv = new Div(
                        String.valueOf(data.layoutCreationCount));
                countDiv.setId(LAYOUT_CREATION_COUNT_ID);
                layout.add(countDiv);
            }
        }
    }

    @Override
    public Element getElement() {
        return layout.getElement();
    }
}
