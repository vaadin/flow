/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

/**
 * System messages provider using the built-in default system messages. This
 * singleton is accessed using {@link #get()}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultSystemMessagesProvider implements SystemMessagesProvider {

    private static final DefaultSystemMessagesProvider instance = new DefaultSystemMessagesProvider();

    private DefaultSystemMessagesProvider() {
        // Singleton
    }

    @Override
    public SystemMessages getSystemMessages(
            SystemMessagesInfo systemMessagesInfo) {
        return HandlerHelper.DEFAULT_SYSTEM_MESSAGES;
    }

    /**
     * Gets the instance.
     *
     * @return the default system messages provider.
     */
    public static SystemMessagesProvider get() {
        return instance;
    }

}
