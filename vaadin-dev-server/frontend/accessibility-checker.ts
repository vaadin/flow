import {html, css, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';

import {RuleDetails} from "accessibility-checker/lib/api/IEngine";

@customElement('accessibility-checker')
export class AccessibilityChecker extends LitElement {
    static styles = css`p { color: pink }`;

    @property()
    report?: RuleDetails[];

    async runTests() {
        const accessibilityCheckResult = {
            // reference to a webdriver object if Selenium WebDriver was used for the scan
            webdriver: undefined,
            // reference to a puppeteer object if Puppeteer was used for the scan
            // Puppeteer is used for string, URL, and file scans
            puppeteer: undefined,
            report: {
                scanID: "18504e0c-fcaa-4a78-a07c-4f96e433f3e7",
                toolID: "accessibility-checker-v3.0.0",
                // Label passed to getCompliance
                label: "MyTestLabel",
                // Number of rules executed
                numExecuted: 137,
                nls: {
                    // Mapping of result.ruleId, result.reasonId to get a tokenized string for the result. Message args are result.messageArgs
                    "WCAG20_Html_HasLang": {
                        "Pass_0": "Page language detected as {0}"
                    },
                    // ...
                },
                summary: {
                    URL: "https://www.ibm.com",
                    counts: {
                        violation: 1,
                        potentialviolation: 0,
                        recommendation: 0,
                        potentialrecommendation: 0,
                        manual: 0,
                        pass: 136,
                        ignored: 0
                    },
                    scanTime: 29,
                    ruleArchive: "September 2019 Deployment (2019SeptDeploy)",
                    policies: [
                        "IBM_Accessibility"
                    ],
                    reportLevels: [
                        "violation",
                        "potentialviolation",
                        "recommendation",
                        "potentialrecommendation",
                        "manual"
                    ],
                    startScan: 1470103006149
                },
                results: [
                    {
                        // Which rule triggered?
                        "ruleId": "WCAG20_Html_HasLang",
                        // In what way did the rule trigger?
                        "reasonId": "Pass_0",
                        "value": [
                            // Is this rule based on a VIOLATION, RECOMMENDATION or INFORMATION
                            "VIOLATION",
                            // PASS, FAIL, POTENTIAL, or MANUAL
                            "FAIL"
                        ],
                        "path": {
                            // xpath
                            "dom": "/html[1]",
                            // path of ARIA roles
                            "aria": "/document[1]"
                        },
                        "ruleTime": 0,
                        // Generated message
                        "message": "Page language detected as en",
                        // Arguments to the message
                        "messageArgs": [
                            "en"
                        ],
                        "apiArgs": [],
                        // Bounding box of the element
                        "bounds": {
                            "left": 0,
                            "top": 0,
                            "height": 143,
                            "width": 800
                        },
                        // HTML snippet of the element
                        "snippet": "<html lang=\"en\">",
                        // What category is this rule?
                        "category": "Accessibility",
                        // Was this issue ignored due to a baseline?
                        "ignored": false,
                        // Summary of the value: violation, potentialviolation, recommendation, potentialrecommendation, manual, pass
                        "level": "pass"
                    },
                    // ...
                ]
            }
        };



        //await getCompliance(document, "test");
        /*        const checker = new ace.Checker();
                const report = await checker.check(document, ["WCAG_2_1"]);*/
        // Remove passing issues
        const reports = accessibilityCheckResult.report as any;
        reports.results = reports.results.filter(
            (issues: any) => issues.value[1] !== "PASS"
        );
        this.report = reports.results;
    }

    render() {
        console.error("render()");
        debugger;
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
