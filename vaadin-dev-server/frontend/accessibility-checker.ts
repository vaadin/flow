import {html, css, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';

import {getCompliance} from 'accessibility-checker';
import {ICheckerReport} from "accessibility-checker/lib/api/IChecker";
import {RuleDetails} from "accessibility-checker/lib/api/IEngine";

@customElement('accessibility-checker')
export class AccessibilityChecker extends LitElement {
    static styles = css`p { color: pink }`;

    @property()
    report?: RuleDetails[];

    async runTests() {
        const results = await getCompliance(document, "test");
/*        const checker = new ace.Checker();
        const report = await checker.check(document, ["WCAG_2_1"]);*/
        // Remove passing issues
        const reports = results.report;
        const test = reports as ICheckerReport
        test.results = test.results.filter(
            (issues) => issues.value[1] !== "PASS"
        );
        this.report = test.results;
    }

    render() {
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
