package hummingbird;

import java.util.Random;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

public abstract class OrderedLayouts extends UI {

    private Random r = new Random(System.currentTimeMillis());

    @Override
    public AbstractOrderedLayout getContent() {
        return (AbstractOrderedLayout) super.getContent();
    }

    @Override
    protected void init(VaadinRequest request) {
        setContent(createMainLayout());

        AbstractOrderedLayout hl1 = createLayout();
        hl1.setWidth("100%");
        updateBorder(hl1);
        getContent().addComponent(hl1);

        AbstractOrderedLayout hl2 = createLayout();
        hl2.setWidth("100%");
        hl2.setSpacing(true);
        updateBorder(hl2);
        getContent().addComponent(hl2);

        AbstractOrderedLayout hl3 = createLayout();
        hl3.setWidth("100%");
        hl3.setMargin(true);
        updateBorder(hl3);
        getContent().addComponent(hl3);

        AbstractOrderedLayout hl4 = createLayout();
        hl4.setWidth("100%");
        hl4.setMargin(true);
        hl4.setSpacing(true);
        getContent().addComponent(hl4);
        updateBorder(hl4);

        Button b = new Button("Dance");
        b.addClickListener(e -> {
            dance(hl1);
            dance(hl2);
            dance(hl3);
            dance(hl4);
        });
        getContent().addComponent(b);
    }

    protected abstract AbstractOrderedLayout createMainLayout();

    private void dance(AbstractOrderedLayout l) {
        if (r.nextInt(10) > 5) {
            l.setMargin(!l.isMargin());
        }
        if (r.nextInt(10) > 5) {
            l.setSpacing(!l.isSpacing());
        }
        for (int i = 0; i < l.getComponentCount(); i++) {
            Component c = l.getComponent(i);
            if (r.nextInt(10) > 7) {
                l.setExpandRatio(c, r.nextInt(3));
            }
        }

        updateBorder(l);
    }

    private void updateBorder(AbstractOrderedLayout l) {
        String color = "black";
        if (l.isSpacing() && l.isMargin()) {
            color = "darkgoldenrod";
        } else if (l.isSpacing()) {
            color = "green";
        } else if (l.isMargin()) {
            color = "blue";
        }

        l.getElement().setStyle("border", "3px solid " + color);

    }

    protected abstract AbstractOrderedLayout createLayout();

}
