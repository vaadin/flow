package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.router.HasDynamicTitle;
import com.vaadin.router.PageTitle;
import com.vaadin.ui.Tag;

@PageTitle("foobar")
public class DynamicTitleView extends AbstractDivView
        implements HasDynamicTitle {

    @Override
    protected void onShow() {
        getElement().removeAllChildren().appendChild(new Element(Tag.DIV)
                .setText("Dynamic").setAttribute("id", "dynamic"));
    }

    @Override
    public String getPageTitle() {
        return "dynamic title view";
    }

}
