/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.tailwindcsstest;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.tailwindcsstest.TailwindCssView")
public class TailwindCssView extends Div {

    public TailwindCssView() {
        addClassNames("font-sans", "box-border", "w-full", "h-full", "flex",
                "flex-col", "items-center", "justify-center", "p-8",
                "bg-gray-100", "text-center");

        var main = new Main();
        main.addClassNames("p-4", "px-6", "bg-white", "rounded-lg",
                "shadow-lg");

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
