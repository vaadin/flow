/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.ui;

import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.LayoutEvents.LayoutClickNotifier;
import com.vaadin.shared.Connector;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.csslayout.CssLayoutServerRpc;

/**
 * CssLayout is a layout component that can be used in browser environment only.
 * It simply renders components and their captions into a same div element.
 * Component layout can then be adjusted with css.
 * <p>
 * In comparison to {@link HorizontalLayout} and {@link VerticalLayout}
 * <ul>
 * <li>rather similar server side api
 * <li>no spacing, alignment or expand ratios
 * <li>much simpler DOM that can be styled by skilled web developer
 * <li>no abstraction of browser differences (developer must ensure that the
 * result works properly on each browser)
 * <li>different kind of handling for relative sizes (that are set from server
 * side) (*)
 * <li>noticeably faster rendering time in some situations as we rely more on
 * the browser's rendering engine.
 * </ul>
 * <p>
 * With {@link CustomLayout} one can often achieve similar results (good looking
 * layouts with web technologies), but with CustomLayout developer needs to work
 * with fixed templates.
 * <p>
 * By extending CssLayout one can also inject some css rules straight to child
 * components using {@link #getCss(Component)}.
 *
 * <p>
 * (*) Relative sizes (set from server side) are treated bit differently than in
 * other layouts in Vaadin. In cssLayout the size is calculated relatively to
 * CSS layouts content area which is pretty much as in html and css. In other
 * layouts the size of component is calculated relatively to the "slot" given by
 * layout.
 * <p>
 * Also note that client side framework in Vaadin modifies inline style
 * properties width and height. This happens on each update to component. If one
 * wants to set component sizes with CSS, component must have undefined size on
 * server side (which is not the default for all components) and the size must
 * be defined with class styles - not by directly injecting width and height.
 *
 * @since 6.1 brought in from "FastLayouts" incubator project
 *
 */
public class CssLayout extends AbstractSimpleDOMComponentContainer
        implements LayoutClickNotifier {

    private CssLayoutServerRpc rpc = new CssLayoutServerRpc() {

        @Override
        public void layoutClick(MouseEventDetails mouseDetails,
                Connector clickedConnector) {
            fireEvent(LayoutClickEvent.createEvent(CssLayout.this, mouseDetails,
                    clickedConnector));
        }
    };

    /**
     * Constructs an empty CssLayout.
     */
    public CssLayout() {
        // registerRpc(rpc);
    }

    /**
     * Constructs a CssLayout with the given components in the given order.
     *
     * @see #addComponents(Component...)
     *
     * @param children
     *            Components to add to the container.
     */
    public CssLayout(Component... children) {
        this();
        addComponents(children);
    }

    public CssLayout(String tagName) {
        super(tagName);
    }

    @Override
    public void addLayoutClickListener(LayoutClickListener listener) {
        addListener(LayoutClickListener.class, listener);
    }

    @Override
    public void removeLayoutClickListener(LayoutClickListener listener) {
        removeListener(LayoutClickListener.class, listener);
    }

}
