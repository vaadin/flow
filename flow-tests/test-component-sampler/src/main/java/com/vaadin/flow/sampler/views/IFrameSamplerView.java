/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.sampler.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the IFrame component and its features.
 */
@Route(value = "iframe", layout = SamplerMainLayout.class)
@PageTitle("IFrame Sampler")
public class IFrameSamplerView extends Div {

    public IFrameSamplerView() {
        setId("iframe-sampler");

        add(new H1("IFrame Component"));
        add(new Paragraph("The IFrame component embeds another document within the page."));

        add(createSection("Basic IFrame",
            "Simple iframe with source URL.",
            createBasicIFrameDemo()));

        add(createSection("IFrame with srcdoc",
            "Inline HTML content in iframe using srcdoc.",
            createSrcdocDemo()));

        add(createSection("IFrame Sizing",
            "IFrames with different sizes.",
            createSizingDemo()));

        add(createSection("IFrame Sandbox",
            "Secure iframe with sandbox attribute.",
            createSandboxDemo()));
    }

    private Div createSection(String title, String description, Div content) {
        Div section = new Div();
        section.getStyle()
            .set("margin-bottom", "40px")
            .set("padding", "20px")
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px");

        H2 sectionTitle = new H2(title);
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph desc = new Paragraph(description);
        desc.getStyle().set("color", "#666");

        section.add(sectionTitle, desc, new Hr(), content);
        return section;
    }

    private Div createBasicIFrameDemo() {
        Div demo = new Div();
        demo.setId("basic-iframe");

        IFrame iframe = new IFrame();
        iframe.setId("basic-iframe-element");
        iframe.setName("basic-frame");
        iframe.setSrcdoc("<html><body style='margin:0;display:flex;justify-content:center;align-items:center;height:100%;background:#f5f5f5;font-family:Arial;'>" +
            "<div style='text-align:center;'><h3>Embedded Content</h3><p>This is an IFrame with srcdoc</p></div></body></html>");
        iframe.getStyle()
            .set("width", "100%")
            .set("height", "200px")
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px");

        demo.add(iframe);
        return demo;
    }

    private Div createSrcdocDemo() {
        Div demo = new Div();
        demo.setId("srcdoc-iframe");

        // Interactive HTML content
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 20px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        min-height: calc(100vh - 40px);
                    }
                    h2 { margin-top: 0; }
                    .counter {
                        font-size: 48px;
                        font-weight: bold;
                        text-align: center;
                        margin: 20px 0;
                    }
                    button {
                        padding: 10px 20px;
                        margin: 5px;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 16px;
                    }
                    .btn-primary { background: white; color: #667eea; }
                    .btn-secondary { background: rgba(255,255,255,0.2); color: white; }
                    .controls { text-align: center; }
                </style>
            </head>
            <body>
                <h2>Interactive Counter</h2>
                <div class="counter" id="counter">0</div>
                <div class="controls">
                    <button class="btn-primary" onclick="increment()">+</button>
                    <button class="btn-secondary" onclick="reset()">Reset</button>
                    <button class="btn-primary" onclick="decrement()">-</button>
                </div>
                <script>
                    let count = 0;
                    function increment() { count++; update(); }
                    function decrement() { count--; update(); }
                    function reset() { count = 0; update(); }
                    function update() { document.getElementById('counter').textContent = count; }
                </script>
            </body>
            </html>
            """;

        IFrame iframe = new IFrame();
        iframe.setId("srcdoc-iframe-element");
        iframe.setSrcdoc(htmlContent);
        iframe.getStyle()
            .set("width", "100%")
            .set("height", "250px")
            .set("border", "none")
            .set("border-radius", "8px")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");

        Paragraph note = new Paragraph("This iframe contains a fully interactive counter with its own HTML, CSS, and JavaScript.");
        note.getStyle().set("font-size", "0.9em").set("color", "#666").set("font-style", "italic").set("margin-top", "15px");

        demo.add(iframe, note);
        return demo;
    }

    private Div createSizingDemo() {
        Div demo = new Div();
        demo.setId("sizing-iframe");
        demo.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
            .set("gap", "20px");

        String[] sizes = {"Small", "Medium", "Large"};
        String[] heights = {"100px", "150px", "200px"};
        String[] colors = {"#e8f5e9", "#e3f2fd", "#fff3e0"};

        for (int i = 0; i < sizes.length; i++) {
            Div container = new Div();

            IFrame iframe = new IFrame();
            iframe.setId("size-iframe-" + (i + 1));
            iframe.setSrcdoc("<html><body style='margin:0;display:flex;justify-content:center;align-items:center;height:100%;background:" +
                colors[i] + ";font-family:Arial;'><span>" + sizes[i] + " IFrame</span></body></html>");
            iframe.getStyle()
                .set("width", "100%")
                .set("height", heights[i])
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px");

            Div label = new Div(sizes[i] + " (" + heights[i] + ")");
            label.getStyle()
                .set("text-align", "center")
                .set("margin-top", "8px")
                .set("font-size", "0.85em")
                .set("color", "#666");

            container.add(iframe, label);
            demo.add(container);
        }

        return demo;
    }

    private Div createSandboxDemo() {
        Div demo = new Div();
        demo.setId("sandbox-iframe");

        String restrictedContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 20px;
                        background: #fafafa;
                    }
                    .warning {
                        background: #ffecb3;
                        border: 1px solid #ffc107;
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 15px;
                    }
                    .info {
                        background: #e3f2fd;
                        border: 1px solid #2196f3;
                        padding: 15px;
                        border-radius: 8px;
                    }
                </style>
            </head>
            <body>
                <div class="warning">
                    <strong>Sandboxed IFrame</strong><br>
                    This iframe has restricted capabilities.
                </div>
                <div class="info">
                    <strong>Restrictions:</strong>
                    <ul>
                        <li>No scripts (unless allow-scripts)</li>
                        <li>No form submission (unless allow-forms)</li>
                        <li>No popups (unless allow-popups)</li>
                        <li>No top navigation (unless allow-top-navigation)</li>
                    </ul>
                </div>
            </body>
            </html>
            """;

        IFrame sandboxedFrame = new IFrame();
        sandboxedFrame.setId("sandboxed-iframe");
        sandboxedFrame.setSrcdoc(restrictedContent);
        sandboxedFrame.getElement().setAttribute("sandbox", "allow-same-origin");
        sandboxedFrame.getStyle()
            .set("width", "100%")
            .set("height", "280px")
            .set("border", "2px solid #ff9800")
            .set("border-radius", "8px");

        Div controls = new Div();
        controls.getStyle().set("margin-top", "15px").set("display", "flex").set("gap", "10px");

        NativeButton allowScripts = new NativeButton("Allow Scripts", e -> {
            sandboxedFrame.getElement().setAttribute("sandbox", "allow-same-origin allow-scripts");
        });
        allowScripts.setId("allow-scripts-btn");

        NativeButton restrictAll = new NativeButton("Restrict All", e -> {
            sandboxedFrame.getElement().setAttribute("sandbox", "");
        });
        restrictAll.setId("restrict-all-btn");

        NativeButton defaultSandbox = new NativeButton("Default Sandbox", e -> {
            sandboxedFrame.getElement().setAttribute("sandbox", "allow-same-origin");
        });
        defaultSandbox.setId("default-sandbox-btn");

        controls.add(allowScripts, restrictAll, defaultSandbox);

        Paragraph explanation = new Paragraph(
            "The sandbox attribute restricts what the iframe content can do. " +
            "Use the buttons above to change the sandbox restrictions.");
        explanation.getStyle()
            .set("font-size", "0.9em")
            .set("color", "#666")
            .set("margin-top", "10px");

        demo.add(sandboxedFrame, controls, explanation);
        return demo;
    }
}
