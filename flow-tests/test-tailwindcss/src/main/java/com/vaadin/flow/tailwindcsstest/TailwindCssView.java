package com.vaadin.flow.tailwindcsstest;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.tailwindcsstest.TailwindCssView")
public class TailwindCssView extends Div {

    public TailwindCssView() {
        addClassNames("font-sans", "box-border", "w-full", "h-full", "flex", "flex-col", "items-center", "justify-center", "p-8", "bg-gray-100", "text-center");

        var main = new Main();
        main.addClassNames("p-4", "px-6", "bg-white", "rounded-lg", "shadow-lg");

        var h1 = new H1();
        h1.add("Tailwind CSS does");

        var notSpanBuiltin = new Span(" not");
        notSpanBuiltin.addClassName("hidden");

        h1.add(notSpanBuiltin);
        h1.add(" work!");

        main.add(h1);

        add(main);
    }
}
