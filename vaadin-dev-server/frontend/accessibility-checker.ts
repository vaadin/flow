import {html, css, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';

import {RuleDetails} from "accessibility-checker/lib/api/IEngine";
// @ts-ignore
import {runAccessibilityCheck} from "./accessibility-checker-lib.js";
import {getComponents} from "./component-util";

@customElement('accessibility-checker')
export class AccessibilityChecker extends LitElement {
    static styles = css`
        p { color: pink; }
        
        .button {
            all: initial;
            font-family: inherit;
            font-size: var(--dev-tools-font-size-small);
            line-height: 1;
            white-space: nowrap;
            background-color: rgba(255, 255, 255, 0.12);
            color: var(--dev-tools-text-color);
            font-weight: 600;
            padding: 0.25rem 0.375rem;
            border-radius: 0.25rem;
        }
    `;

    @property()
    report?: RuleDetails[];

    async runTests() {
        const accessibilityCheckResult = await runAccessibilityCheck(document);
        debugger;
        //await getCompliance(document, "test");
        /*        const checker = new ace.Checker();
                const report = await checker.check(document, ["WCAG_2_1"]);*/
        // Remove passing issues
        accessibilityCheckResult.results = accessibilityCheckResult.results.filter(
            (issues: any) => issues.value[1] !== "PASS"
        );
        this.report = accessibilityCheckResult.results;
    }

    render() {
        console.error("render()");
        debugger;
        return html`
            ${this.report
            ? html`
                  <div>
                      ${this.report.length} issues
                      ${this.report.filter((issues: any) => issues.value[0] == "VIOLATION").length} violations
                      ${this.report.filter((issues: any) => issues.value[0] == "RECOMMENDATION").length} need review
                      ${this.report.filter((issues: any) => issues.value[0] == "INFORMATION").length} improvements
                      <button class="button" @click=${this.runTests}>Re-run Check</button>
                  </div>
                  <ul>
                      ${this.report.map((item) => this.renderItem(item))}
                  </ul>
            `
            : html`
                  Click "Run check" to start the accessibility assessment.
                  <button class="button" @click=${this.runTests}>Run Check</button>
            `}
        `;
    }


    renderItem(issue:RuleDetails) {
        console.error("renderItem()");
        const componentList = getComponents(issue.node.parentElement!);
        const component = componentList[componentList.length - 1];
        return html`<li>
            ${component.element?.tagName} ${issue.message}
            <button @click="">&gt;</button>
        </li>
        `;
    }
}
