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
package com.vaadin.hummingbird.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.HasContent;
import com.vaadin.ui.HasElement;
import com.vaadin.ui.UI;

/**
 * UI that has its contents managed by {@link Router}.
 *
 * @since
 * @author Vaadin Ltd
 */
public class RouterUI extends UI {

    private List<HasElement> viewChain = Collections.emptyList();

    @Override
    protected void init(VaadinRequest request) {
        request.getService().getRouter().initializeUI(this, request);
    }

    /**
     * Shows a view in a chain of layouts in this UI.
     *
     * @param view
     *            the view to show, not <code>null</code>
     * @param layouts
     *            the list of layouts to wrap the view in, starting from the
     *            innermost layout, or <code>null</code> to not use any layout
     */
    public void showView(HasElement view, List<HasContent> layouts) {
        assert view != null;

        Element uiElement = getElement();

        viewChain = new ArrayList<>();
        viewChain.add(view);

        if (layouts != null) {
            viewChain.addAll(layouts);
        }

        if (viewChain.isEmpty()) {
            uiElement.removeAllChildren();
        } else {

            // Ensure the entire chain is connected
            HasElement root = null;
            for (HasElement part : viewChain) {
                if (root != null) {
                    assert part instanceof HasContent : "All parts of the chain except the first must implement HasContent";
                    ((HasContent) part).setContent(root);
                }
                root = part;
            }

            // Not null since there's at least one item in the chain
            @SuppressWarnings("null")
            Element rootElement = root.getElement();

            if (!uiElement.equals(rootElement.getParent())) {
                uiElement.removeAllChildren();
                uiElement.appendChild(rootElement);
            }
        }
    }

    /**
     * Gets the currently active view and the layouts.
     *
     * @return a list of view and layout instances, starting from the innermost
     *         part
     */
    public List<HasElement> getViewChain() {
        return Collections.unmodifiableList(viewChain);
    }

}
