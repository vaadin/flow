/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.BasicTypeInListView")
public class BasicTypeInListView extends AbstractDivView {

    @Tag("basic-type-list")
    @JsModule("./BasicTypeList.js")
    public static class BasicTypeList extends PolymerTemplate<ItemsModel> {

        BasicTypeList() {
            getModel().setItems(Arrays.asList("foo", "bar"));
        }

        @Override
        protected ItemsModel getModel() {
            return super.getModel();
        }
    }

    public interface ItemsModel extends TemplateModel {
        void setItems(List<String> items);

        List<String> getItems();
    }

    public BasicTypeInListView() {
        BasicTypeList list = new BasicTypeList();
        list.setId("template");
        add(list);
        add(createButton("Add an item", "add",
                event -> list.getModel().getItems().add("newItem")));
        add(createButton("Remove the first item", "remove",
                event -> list.getModel().getItems().remove(0)));
    }
}
