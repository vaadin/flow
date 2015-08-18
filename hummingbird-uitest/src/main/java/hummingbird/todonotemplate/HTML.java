package hummingbird.todonotemplate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.ui.AbstractComponent;

public class HTML extends AbstractComponent {

    public HTML(String html) {
        Document d = Jsoup.parse(html);
        if (d.body().children().size() != 1) {
            throw new IllegalArgumentException(
                    "Html must have exactly one root element");
        }
        Element e = d.body().child(0);
        setElement(new com.vaadin.hummingbird.kernel.Element(e.tagName()));

        Attributes attrs = e.attributes();
        for (Attribute a : attrs.asList()) {
            getElement().setAttribute(a.getKey(), a.getValue());
        }

        getElement().setAttribute("innerHTML", e.html());
    }

    public void setInnerHtml(String innerHtml) {
        getElement().setAttribute("innerHTML", innerHtml);
    }

}
