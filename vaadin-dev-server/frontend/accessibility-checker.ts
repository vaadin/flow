import {html, css, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';

import {RuleDetails} from "accessibility-checker/lib/api/IEngine";
// @ts-ignore
import {runAccessibilityCheck} from "./accessibility-checker-lib.js";
import {ComponentReference, getComponents} from "./component-util";
import {Connection} from "./connection";

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

    @property()
    detail?: RuleDetails;

    @property()
    labeltext = "";

    private frontendConnection?: Connection;


    async runTests() {
        const accessibilityCheckResult = await runAccessibilityCheck(document);
        // Remove passing issues
        accessibilityCheckResult.results = accessibilityCheckResult.results.filter(
            (issues: any) => issues.value[1] !== "PASS"
        );
        this.report = accessibilityCheckResult.results;
/*        const labelRules = accessibilityCheckResult.results.filter(
            (issues: any) => issues.ruleId == "input_label_visible"
        )
        if (labelRules.length > 0) {
            this.detail = labelRules[0];
        } else {
            delete this.detail;
        }*/
    }

    openIde(node:Node) {
        const element = node.parentElement;
        const componentList = getComponents(element!);
        const component = componentList[componentList.length - 1];
        const serializableComponentRef: ComponentReference = { nodeId: component.nodeId, uiId: component.uiId };
        this.frontendConnection!.sendShowComponentCreateLocation(serializableComponentRef);
    }

    setLabel(node:Node) {
        const element = node.parentElement;

        (element as any).label = this.labeltext;
    }

    setAriaLabel(node:Node) {
        const element = node.parentElement;

        (element as any).accessibleName = this.labeltext;
    }
    render() {

        if (this.detail) {
            return this.renderDetail(this.detail)
        } else {
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
    }


    renderItem(issue:RuleDetails) {
        console.error("renderItem()");
        const componentList = getComponents(issue.node.parentElement!);
        const component = componentList[componentList.length - 1];
        return html`<li>
            ${component?.element?.tagName} ${issue.message}
            <button @click="">&gt;</button>
        </li>
        `;
    }



    renderDetail(issue:RuleDetails) {
        let minIssue = {
            message: issue.message,
            snippet: issue.snippet,
            value: issue.value,
            reasonId: issue.reasonId,
            ruleId: issue.ruleId,
            path : issue.path["dom"],
            node: issue.node
        };
        return html`<li>
            <button @click="${() => this.openIde(minIssue.node)}">Open in IDE</button> 
            DETAILS: ${minIssue.ruleId}, ${minIssue.reasonId}, ${minIssue.message},
            <label for="input-label">Label </label>
            <input id="input-label" @change=${this._labelUpdated}>
            <button @click="${() => this.setLabel(minIssue.node)}">set label</button> 
            <button @click="${() => this.setAriaLabel(minIssue.node)}">set aria label</button>
        </li>
        `;
    }


    _labelUpdated(e: Event) {
        this.labeltext = (e.target as HTMLInputElement).value;
    }

}
