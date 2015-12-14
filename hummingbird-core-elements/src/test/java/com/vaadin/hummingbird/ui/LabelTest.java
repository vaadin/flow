package com.vaadin.hummingbird.ui;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LabelTest {

    private ValueChangeCounter counter;
    private Label l;

    @Before
    public void setup() {
        l = new Label();
        counter = new ValueChangeCounter();
    }

    @Test
    public void setText() {
        l.addValueChangeListener(counter);
        l.setContentMode(ContentMode.TEXT);
        String text = "Hello World!";
        l.setValue(text);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(1, counter.getCount());
    }

    @Test
    public void setSameTextMultipleTimes() {
        l.addValueChangeListener(counter);
        l.setContentMode(ContentMode.TEXT);
        String text = "Hello World!";
        l.setValue(text);
        l.setValue(text);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(1, counter.getCount());
    }

    @Test
    public void setHTMLInTextLabel() {
        l.addValueChangeListener(counter);
        l.setContentMode(ContentMode.TEXT);
        String text = "<b>foo</b>";
        l.setValue(text);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(1, counter.getCount());
    }

    @Test
    public void setHTML() {
        l.addValueChangeListener(counter);
        l.setContentMode(ContentMode.HTML);
        String text = "<b>foo</b>";
        l.setValue(text);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(1, counter.getCount());
    }

    @Test
    public void setPre() {
        l.addValueChangeListener(counter);
        l.setContentMode(ContentMode.PREFORMATTED);
        String text = "Pre\nformatted\ntext";
        l.setValue(text);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(1, counter.getCount());
    }

    public static class ValueChangeCounter implements ValueChangeListener {
        AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void valueChange(ValueChangeEvent event) {
            counter.incrementAndGet();
        }

        public int getCount() {
            return counter.get();
        }
    }

    @Test
    public void changeTextToPre() {
        ValueChangeCounter counter = new ValueChangeCounter();

        l.setContentMode(ContentMode.TEXT);
        String text = "Pre\nformatted\ntext";
        l.setValue(text);

        l.addValueChangeListener(counter);
        l.setContentMode(ContentMode.PREFORMATTED);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(0, counter.counter.get());
    }

    @Test
    public void changePreToHTML() {
        l.setContentMode(ContentMode.PREFORMATTED);
        String text = "Pre\nformatted\ntext";
        l.setValue(text);
        l.setContentMode(ContentMode.HTML);
        l.addValueChangeListener(counter);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(0, counter.counter.get());
    }

    @Test
    public void changeHtmlToText() {
        l.setContentMode(ContentMode.PREFORMATTED);
        String text = "Pre\nformatted\ntext";
        l.setValue(text);
        l.setContentMode(ContentMode.TEXT);
        Assert.assertEquals(text, l.getValue());
        Assert.assertEquals(0, counter.counter.get());
    }
}
