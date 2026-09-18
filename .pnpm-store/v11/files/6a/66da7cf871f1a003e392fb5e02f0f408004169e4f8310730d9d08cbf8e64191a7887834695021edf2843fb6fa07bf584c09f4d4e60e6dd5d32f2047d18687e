"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
const babylon = require("babylon");
const model_1 = require("../model/model");
const document_1 = require("../parser/document");
const javascript_document_1 = require("./javascript-document");
const baseParseOptions = {
    plugins: [
        'asyncGenerators',
        'dynamicImport',
        // tslint:disable-next-line: no-any Remove this once typings updated.
        'importMeta',
        'objectRestSpread',
    ],
};
// TODO(usergenic): Move this to regular baseParseOptions declaration once
// @types/babylon has been updated to include `ranges`.
// tslint:disable-next-line: no-any
baseParseOptions['ranges'] = true;
class JavaScriptParser {
    parse(contents, url, _urlResolver, inlineInfo) {
        const isInline = !!inlineInfo;
        inlineInfo = inlineInfo || {};
        const result = parseJs(contents, url, inlineInfo.locationOffset, undefined, this.sourceType);
        if (result.type === 'failure') {
            // TODO(rictic): define and return a ParseResult instead of throwing.
            const minimalDocument = new document_1.UnparsableParsedDocument(url, contents);
            throw new model_1.WarningCarryingException(new model_1.Warning(Object.assign({ parsedDocument: minimalDocument }, result.warningish)));
        }
        return new javascript_document_1.JavaScriptDocument({
            url,
            contents,
            baseUrl: inlineInfo.baseUrl,
            ast: result.parsedFile,
            locationOffset: inlineInfo.locationOffset,
            astNode: inlineInfo.astNode,
            isInline,
            parsedAsSourceType: result.sourceType,
        });
    }
}
exports.JavaScriptParser = JavaScriptParser;
class JavaScriptModuleParser extends JavaScriptParser {
    constructor() {
        super(...arguments);
        this.sourceType = 'module';
    }
}
exports.JavaScriptModuleParser = JavaScriptModuleParser;
class JavaScriptScriptParser extends JavaScriptParser {
    constructor() {
        super(...arguments);
        this.sourceType = 'script';
    }
}
exports.JavaScriptScriptParser = JavaScriptScriptParser;
/**
 * Parse the given contents and return either an AST or a parse error as a
 * Warning. It needs the filename and the location offset to produce correct
 * warnings.
 */
function parseJs(contents, file, locationOffset, warningCode, sourceType) {
    if (!warningCode) {
        warningCode = 'parse-error';
    }
    let parsedFile;
    const parseOptions = Object.assign({ sourceFilename: file }, baseParseOptions);
    try {
        // If sourceType is not provided, we will try script first and if that
        // fails, we will try module, since failure is probably that it can't
        // parse the 'import' or 'export' syntax as a script.
        if (!sourceType) {
            try {
                sourceType = 'script';
                parsedFile = babylon.parse(contents, Object.assign({ sourceType }, parseOptions));
            }
            catch (_ignored) {
                sourceType = 'module';
                parsedFile = babylon.parse(contents, Object.assign({ sourceType }, parseOptions));
            }
        }
        else {
            parsedFile = babylon.parse(contents, Object.assign({ sourceType }, parseOptions));
        }
        return {
            type: 'success',
            sourceType: sourceType,
            parsedFile,
        };
    }
    catch (err) {
        if (err instanceof SyntaxError) {
            updateLineNumberAndColumnForError(err);
            return {
                type: 'failure',
                warningish: {
                    message: err.message.split('\n')[0],
                    severity: model_1.Severity.ERROR,
                    code: warningCode,
                    sourceRange: model_1.correctSourceRange({
                        file,
                        start: { line: err.lineNumber - 1, column: err.column - 1 },
                        end: { line: err.lineNumber - 1, column: err.column - 1 }
                    }, locationOffset),
                }
            };
        }
        throw err;
    }
}
exports.parseJs = parseJs;
/**
 * Babylon does not provide lineNumber and column values for unexpected token
 * syntax errors.  This function parses the `(line:column)` value from the
 * message of these errors and updates the error object in place.
 */
function updateLineNumberAndColumnForError(err) {
    if (typeof err.lineNumber === 'number' && typeof err.column === 'number') {
        return;
    }
    if (!err.message) {
        return;
    }
    const lineAndColumnMatch = err.message.match(/(Unexpected token.*)\(([0-9]+):([0-9]+)\)/);
    if (!lineAndColumnMatch) {
        return;
    }
    err.lineNumber = parseInt(lineAndColumnMatch[2], 10);
    err.column = parseInt(lineAndColumnMatch[3], 10) + 1;
}
//# sourceMappingURL=javascript-parser.js.map