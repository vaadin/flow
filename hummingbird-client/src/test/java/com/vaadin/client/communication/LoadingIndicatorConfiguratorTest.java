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
package com.vaadin.client.communication;

import static com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace.FIRST_DELAY_DEFAULT;
import static com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace.FIRST_DELAY_KEY;
import static com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace.SECOND_DELAY_DEFAULT;
import static com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace.SECOND_DELAY_KEY;
import static com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace.THIRD_DELAY_DEFAULT;
import static com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace.THIRD_DELAY_KEY;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.client.LoadingIndicator;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.hummingbird.shared.Namespaces;

public class LoadingIndicatorConfiguratorTest
        extends AbstractConfigurationTest {

    private final Registry registry = new Registry() {
        {
            set(UILifecycle.class, new UILifecycle());
            set(StateTree.class, new StateTree(this));
            set(LoadingIndicator.class, new LoadingIndicator());
        }
    };

    private MapNamespace namespace;

    private LoadingIndicator loadingIndicator;

    @Before
    public void setup() {
        StateNode rootNode = registry.getStateTree().getRootNode();
        namespace = rootNode
                .getMapNamespace(Namespaces.LOADING_INDICATOR_CONFIGURATION);
        loadingIndicator = registry.getLoadingIndicator();
        LoadingIndicatorConfigurator.observe(rootNode, loadingIndicator);
    }

    @Override
    protected MapProperty getProperty(String key) {
        return namespace.getProperty(key);
    }

    @Test
    public void defaults() {
        Assert.assertEquals(FIRST_DELAY_DEFAULT,
                loadingIndicator.getFirstDelay());
        Assert.assertEquals(SECOND_DELAY_DEFAULT,
                loadingIndicator.getSecondDelay());
        Assert.assertEquals(THIRD_DELAY_DEFAULT,
                loadingIndicator.getThirdDelay());
    }

    @Test
    public void setGetFirstDelay() {
        testInt(FIRST_DELAY_KEY, loadingIndicator::getFirstDelay);
    }

    @Test
    public void setGetSecondDelay() {
        testInt(SECOND_DELAY_KEY, loadingIndicator::getSecondDelay);
    }

    @Test
    public void setGetThirdDelay() {
        testInt(THIRD_DELAY_KEY, loadingIndicator::getThirdDelay);
    }

}
