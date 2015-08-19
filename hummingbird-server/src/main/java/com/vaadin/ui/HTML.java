package com.vaadin.ui;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HTML extends AbstractComponent {

    public HTML(String html) {
        Document d = Jsoup.parse(html);
        int nrChildren = d.body().children().size();
        if (nrChildren == 0) {
            d = Jsoup.parse("<span>" + html + "</span>");
        } else if (nrChildren > 1) {
            throw new IllegalArgumentException(
                    "Html cannot have more than one root element");
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
