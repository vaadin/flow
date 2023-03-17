/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.LONG_POLLING)
public class PushLargeDataLongPollingUI extends PushLargeData {

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }
}
