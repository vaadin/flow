import {html, css, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';

import {RuleDetails} from "accessibility-checker/lib/api/IEngine";
// @ts-ignore
import {runAccessibilityCheck} from "./accessibility-checker-lib.js";
import {ComponentReference, getComponents} from "./component-util";
import {Connection} from "./connection";
import {injectGlobalCss} from "./theme-editor/styles";

injectGlobalCss(css`
  .vaadin-accessibility-checker-highlight {
    outline: solid 2px #9e2cc6;
    outline-offset: 3px;
  }
`);

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

        .icon {
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

        .result, .section {
            border-bottom: 1px solid #3C3C3C;
            padding: 0.75rem;
            margin-left: -0.75rem;
            margin-right: -0.75rem;
            /*width: calc(100% + 2*(0.75rem));*/
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .result:hover {
            cursor: pointer;
            background: rgba(0,0,0,0.1);
            transition: background 0.2s;
        }

        .detail-header {
            padding-top: 0;
            justify-content: space-between;
            gap: 10px;
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

        .warning-message {
            display: flex;
            line-height: 1.2;
        }

        .warning-message .icon {
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
            font-size: var(--dev-tools-text-color-active);
            line-height: 1;
            white-space: nowrap;
            background-color: rgba(255, 255, 255, 0.12);
            color: var(--dev-tools-text-color);
            font-weight: 600;
            padding: 0.25rem 0.375rem;
            border-radius: 0.25rem;
            cursor: pointer;
        }

        .text-field {
            background: #3C3C3C;
            border: none;
            padding: 0.2rem;
            border-radius: 4px;
          color: var(--dev-tools-text-color-active);
        }

        h3.small-heading {
          opacity: 0.7;
          font-weight: normal;
          font-size: var(--dev-tools-font-size);
          margin-top: 0;
        }
      
        .detail h2.component:not(:empty) {
          text-transform: lowercase;
          font-size: var(--dev-tools-font-size);
          flex-grow: 1;
        }

      .nav-button {
        all: initial;
        font-family: inherit;
        font-size: calc( var(--dev-tools-font-size-small) * 1);
        line-height: 1;
        white-space: nowrap;
        color: var(--dev-tools-text-color-active);
        font-weight: 600;
        padding: 0.25rem 0.375rem;
        border-radius: 0.25rem;
        cursor: pointer;
      }

      .nav-button .icon {
        width: 12px;
        height: 9px;
      }

      .lower-case {
        text-transform: lowercase;
      }
      .detail-actionbar {
        background: #3C3C3C;
        margin-inline: -0.75rem;
        padding: 0.75rem;
        display: flex;
        gap: 10px;
      }
      .expand {
        flex-grow:1;
      }
    `;

    @property()
    report?: RuleDetails[];

    @property()
    detail?: RuleDetails;

    @property()
    indexDetail?: number;

    @property()
    labeltext = "";

    @state()
    private element: HTMLElement | null = null;

    private frontendConnection?: Connection;


    async runTests() {
        const accessibilityCheckResult = await runAccessibilityCheck(document);
        // Remove passing issues
        accessibilityCheckResult.results = accessibilityCheckResult.results.filter(
            (issues: any) => issues.value[1] !== "PASS"
        );
        this.report = accessibilityCheckResult.results;
    }

    openIde(node:Node) {
        const element = node.parentElement;
        const componentList = getComponents(element!);
        const component = componentList[componentList.length - 1];
        const serializableComponentRef: ComponentReference = { nodeId: component.nodeId, uiId: component.uiId };
        this.frontendConnection!.sendShowComponentCreateLocation(serializableComponentRef);
    }

    backToList() {
        if (this.indexDetail && this.report) {
            this.resetHighlight(this.report[this.indexDetail].node.parentElement);
        }
        this.indexDetail = undefined;
    }

    back() {
        if (this.indexDetail && this.report) {
            this.resetHighlight(this.report[this.indexDetail].node.parentElement);
        }
        if (this.indexDetail) {
            this.indexDetail--;
        } else {
            if (this.report) {
                this.indexDetail = this.report.length - 1;
            }
        }

        if (this.indexDetail && this.report) {
            this.element = this.report[this.indexDetail].node.parentElement;
            this.highlight(this.element);
        }
    }
    next() {
        if (this.indexDetail && this.report) {
            this.resetHighlight(this.report[this.indexDetail].node.parentElement);
        }
        if (this.indexDetail !== undefined && this.report && this.indexDetail < this.report.length - 1) {
            this.indexDetail++;
        } else {
            this.indexDetail = 0;
        }

        if (this.indexDetail && this.report) {
            this.element = this.report[this.indexDetail].node.parentElement;
            this.highlight(this.element);
        }
    }

    private highlight(element: HTMLElement | null) {
        if (element) {
            element.classList.add('vaadin-accessibility-checker-highlight');
        }
    }

    private resetHighlight(element: HTMLElement | null) {
        if (element) {
            element.classList.remove('vaadin-accessibility-checker-highlight');
        }
    }
    setLabel(node:Node) {
        const element = node.parentElement;
        (element as any).label = this.labeltext;
    }

    setAriaLabel(node:Node) {
        const element = node.parentElement;

        (element as any).accessibleName = this.labeltext;
    }

    disconnectedCallback() {
        super.disconnectedCallback();

        this.resetHighlight(this.element);
    }

    render() {
        if (this.indexDetail !== undefined) {
            if (this.report) {
                return this.renderDetail(this.report![this.indexDetail]);
            } else {
                return html``;
            }
        } else {
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
                      ${this.report.map((item, index) => this.renderItem(item, index))}
                  </ul>
            `
                    : html`
                        Click "Run check" to start the accessibility assessment.
                        <button class="button" @click=${this.runTests}>Run Check</button>
                    `}
        `;

        }
    }


    renderItem(issue:RuleDetails, index:number) {
        const componentList = getComponents(issue.node.parentElement!);
        const component = componentList[componentList.length - 1];
        return html`<li class="result" @click="${() => {
            this.indexDetail = index;
            if (this.report) {
                this.element = this.report[this.indexDetail].node.parentElement;
                this.highlight(this.element);
            }
        }
        }">
            <p class="text">
                <span class="component">${component?.element?.tagName}</span>
                <span class="warning-message">

                    ${issue.value[0] == "VIOLATION" ?
                    html`
                    <!--violation icon-->
                    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                        <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM15.25 13.5L13.5 15.25L10 11.75L6.5 15.25L4.75 13.5L8.25 10L4.75 6.5L6.5 4.75L10 8.25L13.5 4.75L15.25 6.5L11.75 10L15.25 13.5Z" fill="#FF3A49"/>
                    </svg>` : ``}

                    ${issue.value[0] == "RECOMMENDATION" ?
                    html`
                    <!--need review icon-->
                    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="18" viewBox="0 0 20 18" fill="none">
                    <path d="M10 0.25L0 17.75H20L10 0.25ZM10 15.25C9.25 15.25 8.75 14.75 8.75 14C8.75 13.25 9.25 12.75 10 12.75C10.75 12.75 11.25 13.25 11.25 14C11.25 14.75 10.75 15.25 10 15.25ZM8.75 11.5V6.5H11.25V11.5H8.75Z" fill="#FFDB7D"/>
                    </svg>` : ``}

                    ${issue.value[0] == "INFORMATION" ?
                    html`
                    <!--enhancement icon-->
                    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                        <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM11.25 16.25H8.75V7.5H11.25V16.25ZM11.25 6.25H8.75V3.75H11.25V6.25Z" fill="#57A1F8"/>
                    </svg>` : ``}

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

        const componentList = getComponents(issue.node.parentElement!);
        const component = componentList[componentList.length - 1];
        return html`<div class="detail">
            <div class="section detail-header">
                <h2 class="component">${component?.element?.tagName ? html`${component.element.tagName}` : html`Global issue`}</h2>
                ${(component?.element) ? html`<button class="button" @click="${() => this.openIde(issue.node)}">Open in IDE</button>` : html``}
            </div>

            <div class="detail-actionbar">
                <button class="nav-button" @click="${() => this.backToList()}">
                    <svg class="icon"  width="20" height="15" viewBox="0 0 20 15" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M0 6.875L7.5 0.75V4.5C7.5 4.5 8.875 4.5 10 4.5C20 4.5 20 14.5 20 14.5C20 14.5 18.75 9.5 10.25 9.5C8.875 9.5 8 9.5 7.5 9.5V13.125L0 6.875H0Z" fill="white"/>
                    </svg> Back to list</button>
                <span class="expand"></span>
                <button class="nav-button" @click="${() => this.back()}">
                    <svg class="icon" width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M2.99517 6.90911L5.68317 9.9091C5.99517 10.2691 5.97117 10.8451 5.58717 11.1811C5.22717 11.4931 4.65117 11.4691 4.33917 11.0851L0.33117 6.59711C0.0191693 6.23711 0.0191694 5.73311 0.33117 5.39711L4.33917 0.909127C4.65117 0.525128 5.22717 0.501128 5.58717 0.837127C5.97117 1.14913 5.99517 1.72512 5.68317 2.08512L2.99517 5.10912L11.0112 5.10912C11.4912 5.10912 11.8992 5.49311 11.8992 5.99711C11.8992 6.50111 11.4912 6.90911 11.0112 6.90911L2.99517 6.90911Z" fill="white"/>
                    </svg>
                    Previous</button>
                <button class="nav-button" @click="${() => this.next()}">Next
                    <svg class="icon" width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M9.00483 5.09089L6.31683 2.0909C6.00483 1.7309 6.02883 1.1549 6.41283 0.818902C6.77283 0.506903 7.34883 0.530903 7.66083 0.914902L11.6688 5.40289C11.9808 5.76289 11.9808 6.26689 11.6688 6.60288L7.66083 11.0909C7.34883 11.4749 6.77283 11.4989 6.41283 11.1629C6.02883 10.8509 6.00483 10.2749 6.31683 9.91488L9.00483 6.89088L0.98883 6.89088C0.50883 6.89088 0.10083 6.50689 0.10083 6.00289C0.10083 5.49889 0.50883 5.09089 0.98883 5.09089L9.00483 5.09089Z" fill="white"/>
                    </svg>
                </button>
            </div>

            <div class="section">
                <div>
                    ${component?.element?.tagName ? html`<h3 class="small-heading lower-case">${component.element.tagName}</h3>` : html``}
                    <span class="warning-message">
                        ${issue.value[0] == "VIOLATION" ?
                        html`
                        <!--violation icon-->
                        <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                            <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM15.25 13.5L13.5 15.25L10 11.75L6.5 15.25L4.75 13.5L8.25 10L4.75 6.5L6.5 4.75L10 8.25L13.5 4.75L15.25 6.5L11.75 10L15.25 13.5Z" fill="#FF3A49"/>
                        </svg>` : ``}
    
                        ${issue.value[0] == "RECOMMENDATION" ?
                        html`
                        <!--need review icon-->
                        <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="18" viewBox="0 0 20 18" fill="none">
                        <path d="M10 0.25L0 17.75H20L10 0.25ZM10 15.25C9.25 15.25 8.75 14.75 8.75 14C8.75 13.25 9.25 12.75 10 12.75C10.75 12.75 11.25 13.25 11.25 14C11.25 14.75 10.75 15.25 10 15.25ZM8.75 11.5V6.5H11.25V11.5H8.75Z" fill="#FFDB7D"/>
                        </svg>` : ``}
    
                        ${issue.value[0] == "INFORMATION" ?
                        html`
                        <!--enhancement icon-->
                        <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                            <path d="M10 0C4.5 0 0 4.5 0 10C0 15.5 4.5 20 10 20C15.5 20 20 15.5 20 10C20 4.5 15.5 0 10 0ZM11.25 16.25H8.75V7.5H11.25V16.25ZM11.25 6.25H8.75V3.75H11.25V6.25Z" fill="#57A1F8"/>
                        </svg>` : ``}
    
                        <span>
                            ${issue.ruleId}
                        </span>
                    </span>
                </div>
            </div>

            ${(issue.ruleId == "input_label_visible" || issue.ruleId == "input_label_exists") ? html`
                <div class="section">
                    <div>
                        <h3 class="small-heading">Fix issue</h3>
                        <label for="input-label">Enter a label and set either a label or an invisible (Aria) label</label>
                        <input class="text-field" id="input-label" @change=${this._labelUpdated} placeholder="Type label here">
                        <button class="button" @click="${() => this.setLabel(issue.node)}">set label</button>
                        <button class="button" @click="${() => this.setAriaLabel(issue.node)}">set aria label</button>
                    </div>
                </div>

                <div class="section">
                    <div>
                        <h3 class="small-heading">Why this is important?</h3>
                        ${(issue.ruleId == "input_label_visible") ? html`
                        <p>Visible labels are essential so that people using voice control know what to say. This allows them to easily navigate to interactive elements on the screen.</p>` 
                                : html`Associating a meaningful label with every UI control allows the browser and assistive technology to expose and announce the control to a user. Associating a visible label also provides a larger clickable area.`
            }
                    </div>
                </div>` : html``
            }

            
            
            <div class="section">
                <div>
                    <h3 class="small-heading">HTML Snippet</h3>
                    <p>${issue.snippet}</p>
                </div>
            </div>
        </div>
        `;
    }


    _labelUpdated(e: Event) {
        this.labeltext = (e.target as HTMLInputElement).value;
    }

}
