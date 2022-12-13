package com.vaadin.flow.uitest.ui.push;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class ReconnectWebsocketIT extends ReconnectTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return BasicPushWebsocketView.class;
    }

}
