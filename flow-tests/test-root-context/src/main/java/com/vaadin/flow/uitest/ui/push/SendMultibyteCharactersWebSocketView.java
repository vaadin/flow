/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@CustomPush(transport = Transport.WEBSOCKET)
@Route("com.vaadin.flow.uitest.ui.push.SendMultibyteCharactersWebSocketView")
public class SendMultibyteCharactersWebSocketView
        extends SendMultibyteCharactersView {

}
