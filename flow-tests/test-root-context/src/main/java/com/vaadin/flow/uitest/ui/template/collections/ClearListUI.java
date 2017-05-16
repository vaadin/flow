package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.List;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.WebComponents;
import com.vaadin.annotations.WebComponents.PolyfillVersion;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd.
 */
@WebComponents(PolyfillVersion.V1)
public class ClearListUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        ClearListTemplate template = new ClearListTemplate();
        template.setId("template");
        add(template);
    }

    @Tag("clear-list")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/collections/ClearList.html")
    public static class ClearListTemplate
            extends PolymerTemplate<ClearListModel> {
        ClearListTemplate() {
            getModel().setMessages(
                    Arrays.asList(new Message("1"), new Message("2")));
        }

        @EventHandler
        private void clearList() {
            getModel().getMessages().clear();
        }
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
