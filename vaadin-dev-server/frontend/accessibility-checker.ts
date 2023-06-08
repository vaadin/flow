import {html, css, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';

import {RuleDetails} from "accessibility-checker/lib/api/IEngine";
// @ts-ignore
import {runAccessibilityCheck} from "./accessibility-checker-lib.js";

@customElement('accessibility-checker')
export class AccessibilityChecker extends LitElement {
    static styles = css`p { color: pink }`;

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
        return html`
            <button @click=${this.runTests}>Run accessibility check</button>
            ${this.report
            ? html`

                        <ul>
                            ${this.report.map(
                (item) => this.renderItem(item))}
                        </ul>
          `
            : html`
            <p>empty</p>
          `}
        `;
    }


    renderItem(issue:RuleDetails) {
        console.error("renderItem()");
        let minIssue = {
            message: issue.message,
            snippet: issue.snippet,
            value: issue.value,
            reasonId: issue.reasonId,
            ruleId: issue.ruleId
        };
        return html`<li>ISSUE: ${minIssue.ruleId}, ${minIssue.reasonId}, ${minIssue.message}, </li>
        `;
    }
}
