"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/* A collection of documents, keyed by path */
class DocumentCollection extends Map {
    getHtmlDoc(url) {
        const result = this.get(url);
        if (result === undefined) {
            return undefined;
        }
        if (result.language !== 'html') {
            throw new Error(`Expected url ${url} to be html but it was ${result.language}`);
        }
        return result;
    }
    getJsDoc(url) {
        const result = this.get(url);
        if (result === undefined) {
            return undefined;
        }
        if (result.language !== 'js') {
            throw new Error(`Expected url ${url} to be js but it was ${result.language}`);
        }
        return result;
    }
}
exports.DocumentCollection = DocumentCollection;
//# sourceMappingURL=document-collection.js.map