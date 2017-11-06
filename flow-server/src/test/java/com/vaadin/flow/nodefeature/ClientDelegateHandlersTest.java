/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.nodefeature;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.ClientDelegate;

public class ClientDelegateHandlersTest {

    @Tag("div")
    static class NonTemplateComponentWithoutEventHandler extends Component {
    }

    @Tag("div")
    static class NonTemplateComponentWithEventHandler extends Component {

        @ClientDelegate
        public void publishedMethod1() {

        }
    }

    @Test
    public void attach_noFeature() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ClientDelegateHandlers.class);

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
        Assert.assertEquals(0,
                stateNode.getFeature(ClientDelegateHandlers.class).size());
    }

    @Test
    public void attach_noComponent() {
        StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

        StateNode stateNode = new StateNode(ComponentMapping.class,
                ClientDelegateHandlers.class);

        tree.getRootNode().getFeature(ElementChildrenList.class).add(stateNode);
        Assert.assertEquals(0,
                stateNode.getFeature(ClientDelegateHandlers.class).size());
    }

    @Test
    public void nonTemplateComponentWithEventHandler() {
        UI ui = new UI();
        NonTemplateComponentWithEventHandler component = new NonTemplateComponentWithEventHandler();
        ui.add(component);

        ClientDelegateHandlers feature = component.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        assertListFeature(feature, "publishedMethod1");
    }

    @Test
    public void nonTemplateComponentWithoutEventHandler() {
        UI ui = new UI();
        NonTemplateComponentWithoutEventHandler component = new NonTemplateComponentWithoutEventHandler();
        ui.add(component);

        ClientDelegateHandlers feature = component.getElement().getNode()
                .getFeature(ClientDelegateHandlers.class);
        assertListFeature(feature);
    }

    private void assertListFeature(SerializableNodeList<String> feature,
            String... expected) {
        Assert.assertEquals(expected.length, feature.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], feature.get(i));
        }

    }
}
