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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Location viewLocation = new Location("");
    private ArrayList<View> viewChain = new ArrayList<>();

    @Override
    protected void init(VaadinRequest request) {
        getRouter().initializeUI(this, request);
    }

    /**
     * Shows a view in a chain of layouts in this UI.
     *
     * @param viewLocation
     *            the location of the view relative to the servlet serving the
     *            UI, not <code>null</code>
     * @param view
     *            the view to show, not <code>null</code>
     * @param parentViews
     *            the list of parent views to wrap the view in, starting from
     *            the parent view immediately wrapping the main view, or
     *            <code>null</code> to not use any parent views
     */
    public void showView(Location viewLocation, View view,
            List<HasChildView> parentViews) {
        assert view != null;
        assert viewLocation != null;

        this.viewLocation = viewLocation;

        Element uiElement = getElement();

        // Assemble previous parent-child relationships to enable detecting
        // changes
        Map<HasChildView, View> oldChildren = new HashMap<>();
        for (int i = 0; i < viewChain.size() - 1; i++) {
            View child = viewChain.get(i);
            HasChildView parent = (HasChildView) viewChain.get(i + 1);

            oldChildren.put(parent, child);
        }

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
                    assert part instanceof HasChildView : "All parts of the chain except the first must implement "
                            + HasChildView.class.getSimpleName();
                    HasChildView parent = (HasChildView) part;
                    if (oldChildren.get(parent) != root) {
                        parent.setChildView(root);
                    }
                } else if (part instanceof HasChildView
                        && oldChildren.containsKey(part)) {
                    // Remove old child view from leaf view if it had one
                    ((HasChildView) part).setChildView(null);
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
     * Updates the page title according to the currently selected view.
     * <p>
     * <code>null</code> will keep the title as it is.
     *
     * @param title
     *            the page title to set
     */
    public void updatePageTitle(String title) {
        // null will keep title as is
        if (title != null) {
            getPage().setTitle(title);
        }
    }

    /**
     * Gets the currently active view and parent views.
     *
     * @return a list of view and parent view instances, starting from the
     *         innermost part
     */
    public List<View> getActiveViewChain() {
        return Collections.unmodifiableList(viewChain);
    }

    /**
     * Gets the location of the currently shown view. The location is relative
     * the the servlet mapping used for serving this UI.
     *
     * @return the view location, not <code>null</code>
     */
    public Location getActiveViewLocation() {
        return viewLocation;
    }

    /**
     * Updates this UI to show the view corresponding to the given location. The
     * location must be a relative URL without any ".." segments.
     *
     * @param location
     *            the location to navigate to, not <code>null</code>
     */
    public void navigateTo(String location) {
        if (location == null) {
            throw new IllegalArgumentException("Location may not be null");
        }

        try {
            URI uri = new URI(location);
            if (uri.isAbsolute()) {
                throw new IllegalArgumentException(
                        "Location cannot be absolute");
            } else if (uri.getPath().startsWith("/")) {
                throw new IllegalArgumentException("Location must be relative");
            } else if (uri.getRawPath().contains("..")) {
                throw new IllegalArgumentException(
                        "Relative location may not contain .. segments");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot parse location", e);
        }

        // Enable navigating back
        getPage().getHistory().pushState(null, location);

        getRouter().navigate(this, new Location(location));
    }

    /**
     * Gets the router used for navigating in this UI.
     *
     * @return the router, not <code>null</code>
     */
    protected Router getRouter() {
        return getSession().getService().getRouter();
    }

}
