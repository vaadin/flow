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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const code_printer_1 = require("../warning/code-printer");
const source_range_1 = require("./source-range");
const stable = require('stable');
class Warning {
    constructor(init) {
        /**
         * Other actions that could be taken in response to this warning.
         *
         * Each action is separate and they may be mutually exclusive. In the case
         * of edit actions they often are.
         */
        this.actions = undefined;
        ({
            message: this.message,
            sourceRange: this.sourceRange,
            severity: this.severity,
            code: this.code,
            parsedDocument: this._parsedDocument
        } = init);
        this.fix = init.fix;
        this.actions = init.actions;
        if (!this.sourceRange) {
            throw new Error(`Attempted to construct a ${this.code} ` +
                `warning without a source range.`);
        }
        if (!this._parsedDocument) {
            throw new Error(`Attempted to construct a ${this.code} ` +
                `warning without a parsed document.`);
        }
    }
    toString(options = {}) {
        const opts = Object.assign({}, defaultPrinterOptions, options);
        const colorize = opts.color ? this._severityToColorFunction(this.severity) :
            (s) => s;
        const severity = this._severityToString(colorize);
        let result = '';
        if (options.verbosity !== 'one-line') {
            const underlined = code_printer_1.underlineCode(this.sourceRange, this._parsedDocument, colorize, options.maxCodeLines);
            if (underlined) {
                result += underlined;
            }
            if (options.verbosity === 'code-only') {
                return result;
            }
            result += '\n\n';
        }
        let file = this.sourceRange.file;
        if (opts.resolver) {
            file = opts.resolver.relative(this.sourceRange.file);
        }
        result += `${file}(${this.sourceRange.start.line + 1},${this.sourceRange.start.column +
            1}) ${severity} [${this.code}] - ${this.message}\n`;
        return result;
    }
    _severityToColorFunction(severity) {
        switch (severity) {
            case Severity.ERROR:
                return chalk.red;
            case Severity.WARNING:
                return chalk.yellow;
            case Severity.INFO:
                return chalk.green;
            default:
                const never = severity;
                throw new Error(`Unknown severity value - ${never}` +
                    ` - encountered while printing warning.`);
        }
    }
    _severityToString(colorize) {
        switch (this.severity) {
            case Severity.ERROR:
                return colorize('error');
            case Severity.WARNING:
                return colorize('warning');
            case Severity.INFO:
                return colorize('info');
            default:
                const never = this.severity;
                throw new Error(`Unknown severity value - ${never} - ` +
                    `encountered while printing warning.`);
        }
    }
    toJSON() {
        return {
            code: this.code,
            message: this.message,
            severity: this.severity,
            sourceRange: this.sourceRange,
        };
    }
}
exports.Warning = Warning;
var Severity;
(function (Severity) {
    Severity[Severity["ERROR"] = 0] = "ERROR";
    Severity[Severity["WARNING"] = 1] = "WARNING";
    Severity[Severity["INFO"] = 2] = "INFO";
})(Severity = exports.Severity || (exports.Severity = {}));
// TODO(rictic): can we get rid of this class entirely?
class WarningCarryingException extends Error {
    constructor(warning) {
        super(warning.message);
        this.warning = warning;
    }
}
exports.WarningCarryingException = WarningCarryingException;
const defaultPrinterOptions = {
    verbosity: 'full',
    color: true
};
/**
 * Takes the given edits and, provided there are no overlaps, applies them to
 * the contents loadable from the given loader.
 *
 * If there are overlapping edits, then edits earlier in the array get priority
 * over later ones.
 */
function applyEdits(edits, loader) {
    return __awaiter(this, void 0, void 0, function* () {
        const result = {
            appliedEdits: [],
            incompatibleEdits: [],
            editedFiles: new Map()
        };
        const replacementsByFile = new Map();
        for (const edit of edits) {
            if (canApply(edit, replacementsByFile)) {
                result.appliedEdits.push(edit);
            }
            else {
                result.incompatibleEdits.push(edit);
            }
        }
        for (const [file, replacements] of replacementsByFile) {
            const document = yield loader(file);
            let contents = document.contents;
            /**
             * This is the important bit. We know that none of the replacements overlap,
             * so in order for their source ranges in the file to all be valid at the
             * time we apply them, we simply need to apply them starting from the end
             * of the document and working backwards to the beginning.
             *
             * To preserve ordering of insertions to the same position, we use a stable
             * sort.
             */
            stable.inplace(replacements, (a, b) => {
                const leftEdgeComp = source_range_1.comparePositionAndRange(b.range.start, a.range, true);
                if (leftEdgeComp !== 0) {
                    return leftEdgeComp;
                }
                return source_range_1.comparePositionAndRange(b.range.end, a.range, false);
            });
            for (const replacement of replacements) {
                const offsets = document.sourceRangeToOffsets(replacement.range);
                contents = contents.slice(0, offsets[0]) + replacement.replacementText +
                    contents.slice(offsets[1]);
            }
            result.editedFiles.set(file, contents);
        }
        return result;
    });
}
exports.applyEdits = applyEdits;
/**
 * We can apply an edit if none of its replacements overlap with any accepted
 * replacement.
 */
function canApply(edit, replacements) {
    for (let i = 0; i < edit.length; i++) {
        const replacement = edit[i];
        const fileLocalReplacements = replacements.get(replacement.range.file) || [];
        // TODO(rictic): binary search
        for (const acceptedReplacement of fileLocalReplacements) {
            if (!areReplacementsCompatible(replacement, acceptedReplacement)) {
                return false;
            }
        }
        // Also check consistency between multiple replacements in this edit.
        for (let j = 0; j < i; j++) {
            const acceptedReplacement = edit[j];
            if (!areReplacementsCompatible(replacement, acceptedReplacement)) {
                return false;
            }
        }
    }
    // Ok, we can be applied to the replacements, so add our replacements in.
    for (const replacement of edit) {
        if (!replacements.has(replacement.range.file)) {
            replacements.set(replacement.range.file, [replacement]);
        }
        else {
            const fileReplacements = replacements.get(replacement.range.file);
            fileReplacements.push(replacement);
        }
    }
    return true;
}
function areReplacementsCompatible(a, b) {
    if (a.range.file !== b.range.file) {
        return true;
    }
    if (areRangesEqual(a.range, b.range)) {
        // Equal ranges are compatible if the ranges are empty (i.e. the edit is an
        // insertion, not a replacement).
        return (a.range.start.column === a.range.end.column &&
            a.range.start.line === a.range.end.line);
    }
    return !(source_range_1.isPositionInsideRange(a.range.start, b.range, false) ||
        source_range_1.isPositionInsideRange(a.range.end, b.range, false) ||
        source_range_1.isPositionInsideRange(b.range.start, a.range, false) ||
        source_range_1.isPositionInsideRange(b.range.end, a.range, false));
}
function areRangesEqual(a, b) {
    return a.start.line === b.start.line && a.start.column === b.start.column &&
        a.end.line === b.end.line && a.end.column === b.end.column;
}
function makeParseLoader(analyzer, analysis) {
    return (url) => __awaiter(this, void 0, void 0, function* () {
        if (analysis) {
            const cachedResult = analysis.getDocument(url);
            if (cachedResult.successful) {
                return cachedResult.value.parsedDocument;
            }
        }
        const result = (yield analyzer.analyze([url])).getDocument(url);
        if (result.successful) {
            return result.value.parsedDocument;
        }
        let message = '';
        if (result.error) {
            message = result.error.message;
        }
        throw new Error(`Cannot load file at: ${JSON.stringify(url)}: ${message}`);
    });
}
exports.makeParseLoader = makeParseLoader;
//# sourceMappingURL=warning.js.map