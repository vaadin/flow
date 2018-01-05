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
package com.vaadin.flow.tutorial.router;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.ContinueNavigationAction;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-lifecycle.asciidoc")
public class NavigationEvents {

    @Route("")
    @Tag("main-layout")
    @HtmlImport("frontend://com/example//MainLayout.html")
    public class MainLayout extends PolymerTemplate<TemplateModel> {

        public MainLayout() {
            SideElement side = new SideElement();
            getElement().appendChild(side.getElement());
        }
    }

    public class SideElement extends Div implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            // Handle for instance before navigation clean up
        }
    }

    @Route("no-items")
    public class NoItemsView extends Div {
        public NoItemsView() {
            setText("No items found.");
        }
    }

    @Route("blog")
    public class BlogList extends Div implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            // implementation omitted
            Object record = getItem();

            if (record == null) {
                event.rerouteTo(NoItemsView.class);
            }
        }

        private Object getItem() {
            // no-op implementation
            return null;
        }
    }

    public class SignupForm extends Div implements BeforeLeaveObserver {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            if (this.hasChanges()) {
                ContinueNavigationAction action = event.postpone();
                ConfirmDialog.build("Are you sure you want to leave this page?")
                        .ifAccept(action::proceed).show();
            }
        }

        private boolean hasChanges() {
            // no-op implementation
            return true;
        }
    }

    public class SideMenu extends Div implements AfterNavigationObserver {
        Anchor blog = new Anchor("blog", "Blog");

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            boolean active = event.getLocation().getFirstSegment()
                    .equals(blog.getHref());
            blog.getElement().getClassList().set("active", active);
        }
    }
}

class ConfirmDialog extends Component {

    public static ConfirmDialog build(String message) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.add(new Label(message));
        return dialog;
    }

    public void show() {
        open();
    }

    public ConfirmDialog ifAccept(Runnable confirmationHandler) {
        confirmationHandler.run();
        return this;
    }

    public void add(Component label) {
    }

    public void open() {

    }
}
