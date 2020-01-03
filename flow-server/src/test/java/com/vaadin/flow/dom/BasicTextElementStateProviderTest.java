/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.impl.BasicTextElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.TextNodeMap;

public class BasicTextElementStateProviderTest {

    @Test
    public void createStateNode_stateNodeHasRequiredElementDataFeature() {
        StateNode stateNode = BasicTextElementStateProvider
                .createStateNode("foo");
        Assert.assertTrue(stateNode.isReportedFeature(TextNodeMap.class));
    }
}
