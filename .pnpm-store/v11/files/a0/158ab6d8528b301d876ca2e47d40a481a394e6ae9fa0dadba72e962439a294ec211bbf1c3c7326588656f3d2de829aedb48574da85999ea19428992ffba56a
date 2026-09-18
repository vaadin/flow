"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function getAnalysisDocument(analysis, url) {
    const result = analysis.getDocument(url);
    if (result.successful) {
        return result.value;
    }
    if (result.error) {
        const message = `Unable to get document ${url}: ${result.error.message}`;
        throw new Error(message);
    }
    throw new Error(`Unable to get document ${url}`);
}
exports.getAnalysisDocument = getAnalysisDocument;
function assertIsHtmlDocument(doc) {
    if (doc.kinds.has('html-document')) {
        return doc;
    }
    else {
        throw new Error(`Document wasn't an HTML document, it's a: ${[...doc.kinds]}`);
    }
}
exports.assertIsHtmlDocument = assertIsHtmlDocument;
function assertIsJsDocument(doc) {
    if (doc.kinds.has('js-document')) {
        return doc;
    }
    else {
        throw new Error(`Document wasn't an JS document, it's a: ${[...doc.kinds]}`);
    }
}
exports.assertIsJsDocument = assertIsJsDocument;
//# sourceMappingURL=analyzer-utils.js.map