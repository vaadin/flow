package com.vaadin.ui;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.annotations.Implemented;

/**
 * Component containing a custom HTML tree given as a string.
 * <p>
 * Must have exactly one root element
 */
@Implemented
public class HTML extends AbstractComponent {

    public HTML(String html) {
        setOuterHtml(html);
    }

    private void setOuterHtml(String outerHtml) {
        Document d = Jsoup.parse(outerHtml);
        int nrChildren = d.body().children().size();
        if (nrChildren == 0) {
            d = Jsoup.parse("<span>" + outerHtml + "</span>");
        } else if (nrChildren > 1) {
            throw new IllegalArgumentException(
                    "Html cannot have more than one root element");
        }
        Element e = d.body().child(0);
        setElement(new com.vaadin.hummingbird.kernel.Element(e.tagName()));

        Attributes attrs = e.attributes();
        for (Attribute a : attrs.asList()) {
            if (a.getKey().equals("class")) {
                getElement().addClass(a.getValue());
            } else {
                getElement().setAttribute(a.getKey(), a.getValue());
            }
        }

        getElement().setAttribute("innerHTML", e.html());

    }

    public void setInnerHtml(String innerHtml) {
        getElement().setAttribute("innerHTML", innerHtml);
    }

    public String getInnerHtml() {
        return getElement().getAttribute("innerHTML");
    }

}
