package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.ClientUpdateMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ClearListView", layout = ViewTestLayout.class)
@Tag("clear-list")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/ClearList.html")
@JsModule("ClearList.js")
public class ClearListView
        extends PolymerTemplate<ClearListView.ClearListModel> {
    public ClearListView() {
        setId("template");
        getModel()
                .setMessages(Arrays.asList(new Message("1"), new Message("2")));
    }

    @EventHandler
    private void clearList() {
        getModel().getMessages().clear();
    }

    public static class Message {
        private String text;

        public Message(String text) {
            this.text = text;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public interface ClearListModel extends TemplateModel {
        void setMessages(List<Message> messages);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<Message> getMessages();
    }
}
