package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.internal.PushConfigurationConstants;
import com.vaadin.flow.shared.ui.Transport;

/*
 * Note that @Push is generally not supported in this location, but instead
 * explicitly picked up by logic in the BasicPushUI constructor.
 */
@Push(transport = Transport.LONG_POLLING)
public class BasicPushLongPollingUI extends BasicPushUI {

    @Override
    public void init(VaadinRequest request) {
        super.init(request);
        // Don't use fallback so we can easier detect failures
        getPushConfiguration().setParameter(
                PushConfigurationConstants.FALLBACK_TRANSPORT_KEY, "none");
    }

}
