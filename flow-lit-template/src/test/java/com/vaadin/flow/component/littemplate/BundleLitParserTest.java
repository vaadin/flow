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
package com.vaadin.flow.component.littemplate;

import java.io.IOException;

import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;

public class BundleLitParserTest {
    private String content = "\n            var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {\n                var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;\n                if (typeof Reflect === 'object' && typeof Reflect.decorate === 'function') r = Reflect.decorate(decorators, target, key, desc);\n                else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;\n                return c > 3 && r && Object.defineProperty(target, key, r), r;\n            };\n            import { html, LitElement } from 'lit';\n            import { customElement } from 'lit/decorators.js';\n            let AboutView = class AboutView extends LitElement {\n                render() {\n                    return html `<vaadin-split-layout style='width: 100%; height: 100%;'>\n                  <div style='width:400px;display:flex;flex-direction:column;'>\n                    <div style='padding:var(--lumo-space-l);flex-grow:1;'>\n                      <vaadin-form-layout>\n                        <vaadin-text-field\n                          label='First name'\n                          id='firstName'\n                        ></vaadin-text-field\n                        ><vaadin-text-field\n                          label='Last name'\n                          id='lastName'\n                        ></vaadin-text-field\n                        ><vaadin-date-picker\n                          label='Date of birth'\n                          id='dateOfBirth'\n                        ></vaadin-date-picker\n                        ><vaadin-text-field\n                          label='Occupation'\n                          id='occupation'\n                        ></vaadin-text-field\n                        ><vaadin-checkbox\n                          id='important'\n                          style='padding-top: var(--lumo-space-m);'\n                          >Important</vaadin-checkbox\n                        >\n                      </vaadin-form-layout>\n                    </div>\n                    <vaadin-horizontal-layout\n                      style='flex-wrap:wrap;width:100%;background-color:var(--lumo-contrast-5pct);padding:var(--lumo-space-s) var(--lumo-space-l);'\n                      theme='spacing'\n                    >\n                      <vaadin-button theme='primary' id='save'>Save</vaadin-button>\n                      <vaadin-button theme='tertiary' slot='' id='cancel'\n                        >Cancel</vaadin-button\n                      >\n                    </vaadin-horizontal-layout>\n                  </div>\n                </vaadin-split-layout>`;\n                }\n            };\n            AboutView = __decorate([\n                customElement('about-view')\n            ], AboutView);\n            export { AboutView };\n";

    @Test
    public void parseTemplate() {
        final Element element = BundleLitParser.parseLitTemplateElement("in.ts",
                content);

        Assert.assertEquals(
                "The html should contain 12 elements making it 13 with the expected addition of a template element",
                13, element.getAllElements().size());
        Assert.assertEquals("", "vaadin-split-layout",
                element.getElementsByTag("template").get(0).child(0).tagName());
    }

    @Test
    public void parseTemplateWithComments_commentsProperlyIgnored() {
        final Element element = BundleLitParser.parseLitTemplateElement("in.ts",
        // @formatter:off
                         "import { html, LitElement } from 'lit';\n"
                        + "\n"
                        + "export class HelloLit extends LitElement {\n"
                        + "  /* comment **/\n"
                        + "\n"
                        + "  render() {\n"+
                                 "    const athleteTimerStyles = { \n" +
                                 "      display:  this.currentAthleteMode && !this.decisionVisible ? \"grid\" : \"none\",\n" +
                                 "    }" +
                         "    return html` \n" +
                                 "<div>Some content</div>`;\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"
                        + "customElements.define('hello-lit', HelloLit);");
         // @formatter:on

        Assert.assertEquals(2, element.getAllElements().size());
        Assert.assertEquals(1, element.getElementsByTag("div").size());
    }

    @Test
    public void parseTemplate_codeInRenderBeforeHtml_templateProperlyParsed() {
        final Element element = BundleLitParser.parseLitTemplateElement("in.ts",
        // @formatter:off
                         "import { html, LitElement } from 'lit';\n"
                        + "\n"
                        + "export class HelloLit extends LitElement {\n"
                        + "\n"
                        + "  render() {\n"
                        + "    const athleteTimerStyles = { \n"
                        + "      display:  this.currentAthleteMode && !this.decisionVisible ? \"grid\" : \"none\",\n"
                        + "    }\n"
                        + "    return html`\n"
                        + "      <div>Some content</div>`;\n"
                        + "      <span class=\"timer athleteTimer\" style=\"${styleMap(athleteTimerStyles)}\">\n"
                        + "        <timer-element id=\"athleteTimer\"></timer-element>\n"
                        + "      </span>\n"
                        + "      `;"
                        + "  }\n"
                        + "}\n"
                        + "\n"
                        + "customElements.define('hello-lit', HelloLit);");
         // @formatter:on

        Assert.assertEquals(4, element.getAllElements().size());
        Assert.assertEquals(1, element.getElementsByTag("div").size());
        Assert.assertEquals(1, element.getElementsByTag("span").size());
        Assert.assertEquals(1,
                element.getElementsByTag("timer-element").size());
    }

    @Test
    public void parseTemplate_codeWithHtmlBeforeRender_templateProperlyParsed() {
        final Element element = BundleLitParser.parseLitTemplateElement("in.ts",
        // @formatter:off
                "import { html, LitElement } from 'lit';\n"
                        + "\n"
                        + "export class HelloLit extends LitElement {\n"
                        + "\n"
                        + "  helper() {\n"
                        + "    return html`<span>helper</span>`;\n"
                        + "  }\n"
                        + "  render() {\n"
                        + "    const athleteTimerStyles = { \n"
                        + "      display:  this.currentAthleteMode && !this.decisionVisible ? \"grid\" : \"none\",\n"
                        + "    }\n"
                        + "    return html`\n"
                        + "      <div>Some content</div>`;\n"
                        + "      <span class=\"timer athleteTimer\" style=\"${styleMap(athleteTimerStyles)}\">\n"
                        + "        <timer-element id=\"athleteTimer\"></timer-element>\n"
                        + "      </span>\n"
                        + "      `;"
                        + "  }\n"
                        + "}\n"
                        + "\n"
                        + "customElements.define('hello-lit', HelloLit);");
        // @formatter:on

        Assert.assertEquals(4, element.getAllElements().size());
        Assert.assertEquals(1, element.getElementsByTag("div").size());
        Assert.assertEquals(1, element.getElementsByTag("span").size());
        Assert.assertEquals(1,
                element.getElementsByTag("timer-element").size());
    }

    @Test
    public void parseTemplate_codeWithHtmlAfterRender_templateProperlyParsed() {
        final Element element = BundleLitParser.parseLitTemplateElement("in.ts",
        // @formatter:off
                "import { html, LitElement } from 'lit';\n"
                        + "\n"
                        + "export class HelloLit extends LitElement {\n"
                        + "\n"
                        + "  render() {\n"
                        + "    const athleteTimerStyles = { \n"
                        + "      display:  this.currentAthleteMode && !this.decisionVisible ? \"grid\" : \"none\",\n"
                        + "    }\n"
                        + "    return html`\n"
                        + "      <div>Some content</div>`;\n"
                        + "      <span class=\"timer athleteTimer\" style=\"${styleMap(athleteTimerStyles)}\">\n"
                        + "        <timer-element id=\"athleteTimer\"></timer-element>\n"
                        + "      </span>\n"
                        + "      `;"
                        + "  }\n"
                        + "  helper() {\n"
                        + "    return html`<span>helper</span>`;\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"
                        + "customElements.define('hello-lit', HelloLit);");
        // @formatter:on

        Assert.assertEquals(4, element.getAllElements().size());
        Assert.assertEquals(1, element.getElementsByTag("div").size());
        Assert.assertEquals(1, element.getElementsByTag("span").size());
        Assert.assertEquals(1,
                element.getElementsByTag("timer-element").size());
    }

}
