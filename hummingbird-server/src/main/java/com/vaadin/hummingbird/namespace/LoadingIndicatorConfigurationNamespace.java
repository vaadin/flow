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
package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.LoadingIndicatorConfiguration;

/**
 * Namespace for storing configuration for the loading indicator.
 *
 * @author Vaadin
 * @since
 */
public class LoadingIndicatorConfigurationNamespace extends MapNamespace
        implements LoadingIndicatorConfiguration {
    public static final String FIRST_DELAY_KEY = "first";
    public static final int FIRST_DELAY_DEFAULT = 300;
    public static final String SECOND_DELAY_KEY = "second";
    public static final int SECOND_DELAY_DEFAULT = 1500;
    public static final String THIRD_DELAY_KEY = "third";
    public static final int THIRD_DELAY_DEFAULT = 5000;

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public LoadingIndicatorConfigurationNamespace(StateNode node) {
        super(node);
    }

    @Override
    public void setFirstDelay(int firstDelay) {
        put(FIRST_DELAY_KEY, firstDelay);
    }

    @Override
    public int getFirstDelay() {
        if (!contains(FIRST_DELAY_KEY)) {
            return FIRST_DELAY_DEFAULT;
        }
        return (int) get(FIRST_DELAY_KEY);
    }

    @Override
    public void setSecondDelay(int secondDelay) {
        put(SECOND_DELAY_KEY, secondDelay);
    }

    @Override
    public int getSecondDelay() {
        if (!contains(SECOND_DELAY_KEY)) {
            return SECOND_DELAY_DEFAULT;
        }
        return (int) get(SECOND_DELAY_KEY);
    }

    @Override
    public void setThirdDelay(int thirdDelay) {
        put(THIRD_DELAY_KEY, thirdDelay);
    }

    @Override
    public int getThirdDelay() {
        if (!contains(THIRD_DELAY_KEY)) {
            return THIRD_DELAY_DEFAULT;
        }
        return (int) get(THIRD_DELAY_KEY);
    }
}
