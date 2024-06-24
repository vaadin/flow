/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.WEBSOCKET)
public class SendMultibyteCharactersWebSocketUI
        extends SendMultibyteCharactersUI {

}
