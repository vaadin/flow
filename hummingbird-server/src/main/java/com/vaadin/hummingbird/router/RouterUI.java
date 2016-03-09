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
import com.vaadin.ui.UI;

/**
 * UI that has its contents managed by a {@link Router}.
 *
 * @since
 * @author Vaadin Ltd
 */
public class RouterUI extends UI {

    private ArrayList<View> viewChain = new ArrayList<>();

    @Override
    protected void init(VaadinRequest request) {
        request.getService().getRouter().initializeUI(this, request);
    }

    /**
     * Shows a view in a chain of layouts in this UI.
     *
     * @param view
     *            the view to show, not <code>null</code>
     * @param parentViews
     *            the list of parent views to wrap the view in, starting from
     *            the parent view immediately wrapping the main view, or
     *            <code>null</code> to not use any parent views
     */
    public void showView(View view, List<HasSubView> parentViews) {
        assert view != null;

        Element uiElement = getElement();

        viewChain = new ArrayList<>();
        viewChain.add(view);

        if (parentViews != null) {
            viewChain.addAll(parentViews);
        }

        if (viewChain.isEmpty()) {
            uiElement.removeAllChildren();
        } else {
            // Ensure the entire chain is connected
            View root = null;
            for (View part : viewChain) {
                if (root != null) {
                    assert part instanceof HasSubView : "All parts of the chain except the first must implement HasSubView";
                    ((HasSubView) part).setSubView(root);
                }
                root = part;
            }

            if (root == null) {
                throw new IllegalArgumentException(
                        "Root can't be null here since we know there's at least one item in the chain");
            }

            Element rootElement = root.getElement();

            if (!uiElement.equals(rootElement.getParent())) {
                uiElement.removeAllChildren();
                rootElement.removeFromParent();
                uiElement.appendChild(rootElement);
            }
        }
    }

    /**
     * Gets the currently active view and parent views.
     *
     * @return a list of view and parent view instances, starting from the
     *         innermost part
     */
    public List<View> getViewChain() {
        return Collections.unmodifiableList(viewChain);
    }

}
