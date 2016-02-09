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

/**
 * Old UI state class, to be deleted asap.
 *
 * @author Vaadin
 * @since
 */
public class UIState implements Serializable {
    public LoadingIndicatorConfigurationState loadingIndicatorConfiguration = new LoadingIndicatorConfigurationState();
    public int pollInterval = -1;

    public ReconnectDialogConfigurationState reconnectDialogConfiguration = new ReconnectDialogConfigurationState();

    public static class LoadingIndicatorConfigurationState
            implements Serializable {
        public int firstDelay = 300;
        public int secondDelay = 1500;
        public int thirdDelay = 5000;
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
