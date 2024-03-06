/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared.communication;

import java.io.Serializable;

/**
 * Shared constants used by push.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PushConstants implements Serializable {

    /**
     * The size, in <b>bytes</b>, of the receiving buffer used by some servers.
     * <p>
     * Should not be set to a value equal to or greater than 32768 due to a
     * Jetty 9.1 issue (see #13087)
     */
    public static final int WEBSOCKET_BUFFER_SIZE = 16384;

    /**
     * The maximum size, in <b>characters</b>, of a websocket message fragment.
     * This is a conservative maximum chosen so that the size in bytes will not
     * exceed {@link PushConstants#WEBSOCKET_BUFFER_SIZE} given a UTF-8 encoded
     * message.
     */
    public static final int WEBSOCKET_FRAGMENT_SIZE = WEBSOCKET_BUFFER_SIZE / 4
            - 1;

    /**
     * The character used to mark message boundaries when messages may be split
     * into multiple fragments.
     */
    public static final char MESSAGE_DELIMITER = '|';
}
