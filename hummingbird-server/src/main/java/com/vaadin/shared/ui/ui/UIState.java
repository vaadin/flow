/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.shared.ui.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.shared.communication.PushMode;

/**
 * Old UI state class, to be deleted asap.
 *
 * @author Vaadin
 * @since
 */
public class UIState implements Serializable {
    public LoadingIndicatorConfigurationState loadingIndicatorConfiguration = new LoadingIndicatorConfigurationState();
    public int pollInterval = -1;

    /**
     * Configuration for the push channel
     */
    public PushConfigurationState pushConfiguration = new PushConfigurationState();
    public ReconnectDialogConfigurationState reconnectDialogConfiguration = new ReconnectDialogConfigurationState();

    public static class LoadingIndicatorConfigurationState
            implements Serializable {
        public int firstDelay = 300;
        public int secondDelay = 1500;
        public int thirdDelay = 5000;
    }

    public static class PushConfigurationState implements Serializable {
        public static final String TRANSPORT_PARAM = "transport";
        public static final String FALLBACK_TRANSPORT_PARAM = "fallbackTransport";

        public boolean alwaysUseXhrForServerRequests = false;
        public PushMode mode = PushMode.DISABLED;
        public String pushUrl = null;
        public Map<String, String> parameters = new HashMap<String, String>();

        {
            parameters.put(TRANSPORT_PARAM,
                    Transport.WEBSOCKET.getIdentifier());
            parameters.put(FALLBACK_TRANSPORT_PARAM,
                    Transport.LONG_POLLING.getIdentifier());
        }
    }

    public static class ReconnectDialogConfigurationState
            implements Serializable {
        public String dialogText = "Server connection lost, trying to reconnect...";
        public String dialogTextGaveUp = "Server connection lost.";
        public int reconnectAttempts = 10000;
        public int reconnectInterval = 5000;
        public int dialogGracePeriod = 400;
        public boolean dialogModal = false;
    }

}
