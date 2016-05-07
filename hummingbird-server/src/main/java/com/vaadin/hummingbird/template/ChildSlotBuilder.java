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
package com.vaadin.hummingbird.template;

import java.util.Optional;

/**
 * Builder for {@link ChildSlotNode}.
 *
 * @author Vaadin Ltd
 */
public class ChildSlotBuilder implements TemplateNodeBuilder {

    @Override
    public TemplateNode build(TemplateNode parent) {
        assert parent instanceof AbstractElementTemplateNode : "Child slot parent must be an instance of "
                + AbstractElementTemplateNode.class;

        verifyOnlyChildNode(parent);

        return new ChildSlotNode((AbstractElementTemplateNode) parent);
    }

    private static void verifyOnlyChildNode(TemplateNode parent) {
        TemplateNode root = findRootNode(parent);

        if (ChildSlotNode.find(root).isPresent()) {
            throw new TemplateParseException(
                    "There are multiple @child@ slots in the template");
        }
    }

    private static TemplateNode findRootNode(TemplateNode parent) {
        TemplateNode node = parent;
        while (true) {
            Optional<TemplateNode> maybeParent = node.getParent();
            if (maybeParent.isPresent()) {
                node = maybeParent.get();
            } else {
                return node;
            }
        }
    }
}
