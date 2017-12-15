package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.polymertemplate.EventHandler;
import com.vaadin.flow.polymertemplate.PolymerTemplate;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ClearListView", layout = ViewTestLayout.class)
@Tag("clear-list")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/ClearList.html")
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

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public interface ClearListModel extends TemplateModel {
        void setMessages(List<Message> messages);

        List<Message> getMessages();
    }
}
