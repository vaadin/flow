

export function runAccessibilityCheck(document) {
    const checker = new ace.Checker();
    return checker.check(document, ["WCAG_2_1"]);
}