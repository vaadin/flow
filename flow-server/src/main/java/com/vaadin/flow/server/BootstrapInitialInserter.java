package com.vaadin.flow.server;

import org.jsoup.nodes.Element;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

class BootstrapInitialInserter
        implements ClientIndexBootstrapListener {

    @Override
    public void modifyBootstrapPage(ClientIndexBootstrapPage page) {
        VaadinService vaadinService = CurrentInstance
                .get(VaadinService.class);

        if (!vaadinService.getBootstrapInitialPredicate()
                .includeInitial(page.getVaadinRequest())) {
            return;
        }

        VaadinRequest request = page.getVaadinRequest();
        request.setAttribute(
                ApplicationConstants.REQUEST_LOCATION_PARAMETER,
                request.getPathInfo());

        BootstrapHandler handler = page.getHandler();

        JsonObject initial = handler.getInitialJson(request,
                page.getVaadinResponse(), page.getVaadinSession());

        Element elm = new Element("script");
        elm.attr("initial", "");
        elm.text("window.Vaadin = {Flow : {initial: "
                + JsonUtil.stringify(initial) + "}}");
        page.getDocument().head().insertChildren(0, elm);

    }
}