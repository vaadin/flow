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

        .issue-summary {
            background: #3C3C3C;
            margin: -0.75rem;
            padding: 0.75rem;
            position: sticky;
            top: -0.75rem;
            z-index: 1;
        }

        .issue-summary .icon {
            width: 14px;
            height: 14px;
            margin-right: 0.2rem;
        }

        .issue-summary > span {
            display: inline-flex;
            align-items: center;
            margin-right: 0.5rem;
            vertical-align: middle;
            margin-bottom: 0.5rem;
        }

        .result-list {
            list-style-type: none;
            padding: 0;
        }

        .result {
            border-bottom: 1px solid #3C3C3C;
            padding: 0.75rem;
            margin-left: -0.75rem;
            margin-right: -0.75rem;
            width: calc(100% + 2(0.75rem));
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .result .text {
            margin: 0;
            display: flex;
            flex-direction: column;
            flex: 1 1 auto;
            padding-right: 1rem;
        }

        .result .component:not(:empty) {
            text-transform: lowercase;
            opacity: 0.7;
            margin-bottom: 0.5rem;
        }

        .result .warning-message {
            display: flex;
            line-height: 1.2;
        }

        .result .warning-message .icon {
            flex-shrink: 0;
            width: 14px;
            height: 14px;
            margin-right: 0.5rem;
        }

        .result .arrow {
            flex-shrink: 0;
        }
        
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
            cursor: pointer;
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
        return html`
            ${this.report
                    ? html`
                  <div class="issue-summary">
                      <span>
                        ${this.report.length} issues
                      </span>

                      <span>
                          <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                              <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM15.25 13.5L13.5 15.25L10 11.75L6.5 15.25L4.75 13.5L8.25 10L4.75 6.5L6.5 4.75L10 8.25L13.5 4.75L15.25 6.5L11.75 10L15.25 13.5Z" fill="#FF3A49"/>
                          </svg>
                          ${this.report.filter((issues: any) => issues.value[0] == "VIOLATION").length}
                          violations
                      </span>

                      <span>
                          <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="18" viewBox="0 0 20 18" fill="none">
                            <path d="M10 0.25L0 17.75H20L10 0.25ZM10 15.25C9.25 15.25 8.75 14.75 8.75 14C8.75 13.25 9.25 12.75 10 12.75C10.75 12.75 11.25 13.25 11.25 14C11.25 14.75 10.75 15.25 10 15.25ZM8.75 11.5V6.5H11.25V11.5H8.75Z" fill="#FFDB7D"/>
                          </svg>
                          ${this.report.filter((issues: any) => issues.value[0] == "RECOMMENDATION").length}
                          need review
                      </span>

                      <span>
                          <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                            <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM11.25 16.25H8.75V7.5H11.25V16.25ZM11.25 6.25H8.75V3.75H11.25V6.25Z" fill="#57A1F8"/>
                          </svg>
                           ${this.report.filter((issues: any) => issues.value[0] == "INFORMATION").length}
                          improvements
                      </span>

                      <button class="button" @click=${this.runTests}>Re-run Check</button>
                  </div>
                  <ul class="result-list">
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
        return html`<li class="result">
            <p class="text">
                <span class="component">${component?.element?.tagName}</span>
                <span class="warning-message">

                    <!--violation icon-->
                    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                        <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM15.25 13.5L13.5 15.25L10 11.75L6.5 15.25L4.75 13.5L8.25 10L4.75 6.5L6.5 4.75L10 8.25L13.5 4.75L15.25 6.5L11.75 10L15.25 13.5Z" fill="#FF3A49"/>
                    </svg>

                    <!--need review icon-->
                    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="18" viewBox="0 0 20 18" fill="none">
                    <path d="M10 0.25L0 17.75H20L10 0.25ZM10 15.25C9.25 15.25 8.75 14.75 8.75 14C8.75 13.25 9.25 12.75 10 12.75C10.75 12.75 11.25 13.25 11.25 14C11.25 14.75 10.75 15.25 10 15.25ZM8.75 11.5V6.5H11.25V11.5H8.75Z" fill="#FFDB7D"/>
                    </svg>

                    <!--enhancement icon-->
                    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                        <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM11.25 16.25H8.75V7.5H11.25V16.25ZM11.25 6.25H8.75V3.75H11.25V6.25Z" fill="#57A1F8"/>
                    </svg>

                    ${issue.message}
                </span>
            </p>
            <svg class="arrow" width="9" height="14" viewBox="0 0 9 14" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path opacity="0.7" d="M0.25 13.25H2.75L9 7L2.75 0.75H0.25L6.5 7L0.25 13.25Z" fill="white"/>
            </svg>
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
