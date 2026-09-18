"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
const chai_1 = require("chai");
const shady_css_1 = require("../shady-css");
const ast_iterator_1 = require("../shady-css/ast-iterator");
const fixtures = require("./fixtures");
const test_node_factory_1 = require("./test-node-factory");
chai_1.use(require('chai-subset'));
describe('Parser', () => {
    let parser;
    let nodeFactory;
    beforeEach(() => {
        parser = new shady_css_1.Parser();
        nodeFactory = new test_node_factory_1.TestNodeFactory();
    });
    describe('when parsing css', () => {
        it('can parse a basic ruleset', () => {
            chai_1.expect(parser.parse(fixtures.basicRuleset))
                .to.be.containSubset(nodeFactory.stylesheet([
                nodeFactory.ruleset('body', nodeFactory.rulelist([
                    nodeFactory.declaration('margin', nodeFactory.expression('0')),
                    nodeFactory.declaration('padding', nodeFactory.expression('0px'))
                ]))
            ]));
        });
        it('can parse at rules', () => {
            chai_1.expect(parser.parse(fixtures.atRules))
                .to.containSubset(nodeFactory.stylesheet([
                nodeFactory.atRule('import', 'url(\'foo.css\')', undefined),
                nodeFactory.atRule('font-face', '', nodeFactory.rulelist([nodeFactory.declaration('font-family', nodeFactory.expression('foo'))])),
                nodeFactory.atRule('charset', '\'foo\'', undefined)
            ]));
        });
        it('can parse keyframes', () => {
            chai_1.expect(parser.parse(fixtures.keyframes))
                .to.containSubset(nodeFactory.stylesheet([
                nodeFactory.atRule('keyframes', 'foo', nodeFactory.rulelist([
                    nodeFactory.ruleset('from', nodeFactory.rulelist([nodeFactory.declaration('fiz', nodeFactory.expression('0%'))])),
                    nodeFactory.ruleset('99.9%', nodeFactory.rulelist([
                        nodeFactory.declaration('fiz', nodeFactory.expression('100px')),
                        nodeFactory.declaration('buz', nodeFactory.expression('true'))
                    ]))
                ]))
            ]));
        });
        it('can parse custom properties', () => {
            chai_1.expect(parser.parse(fixtures.customProperties))
                .to.containSubset(nodeFactory.stylesheet([nodeFactory.ruleset(':root', nodeFactory.rulelist([
                    nodeFactory.declaration('--qux', nodeFactory.expression('vim')),
                    nodeFactory.declaration('--foo', nodeFactory.rulelist([nodeFactory.declaration('bar', nodeFactory.expression('baz'))]))
                ]))]));
        });
        it('can parse declarations with no value', () => {
            chai_1.expect(parser.parse(fixtures.declarationsWithNoValue))
                .to.containSubset(nodeFactory.stylesheet([
                nodeFactory.declaration('foo', undefined),
                nodeFactory.declaration('bar 20px', undefined),
                nodeFactory.ruleset('div', nodeFactory.rulelist([nodeFactory.declaration('baz', undefined)]))
            ]));
        });
        it('can parse minified rulelists', () => {
            chai_1.expect(parser.parse(fixtures.minifiedRuleset))
                .to.containSubset(nodeFactory.stylesheet([
                nodeFactory.ruleset('.foo', nodeFactory.rulelist([nodeFactory.declaration('bar', nodeFactory.expression('baz'))])),
                nodeFactory.ruleset('div .qux', nodeFactory.rulelist([nodeFactory.declaration('vim', nodeFactory.expression('fet'))]))
            ]));
        });
        it('can parse psuedo rulesets', () => {
            chai_1.expect(parser.parse(fixtures.psuedoRuleset))
                .to.containSubset(nodeFactory.stylesheet([nodeFactory.ruleset('.foo:bar:not(#rif)', nodeFactory.rulelist([nodeFactory.declaration('baz', nodeFactory.expression('qux'))]))]));
        });
        it('can parse rulelists with data URIs', () => {
            chai_1.expect(parser.parse(fixtures.dataUriRuleset))
                .to.containSubset(nodeFactory.stylesheet([nodeFactory.ruleset('.foo', nodeFactory.rulelist([nodeFactory.declaration('bar', nodeFactory.expression('url(qux;gib)'))]))]));
        });
        it('can parse pathological comments', () => {
            chai_1.expect(parser.parse(fixtures.pathologicalComments))
                .to.containSubset(nodeFactory.stylesheet([
                nodeFactory.ruleset('.foo', nodeFactory.rulelist([nodeFactory.declaration('bar', nodeFactory.expression('/*baz*/vim'))])),
                nodeFactory.comment('/* unclosed\n@fiz {\n  --huk: {\n    /* buz */'),
                nodeFactory.declaration('baz', nodeFactory.expression('lur')),
                nodeFactory.discarded('};'),
                nodeFactory.discarded('}'),
                nodeFactory.atRule('gak', 'wiz', undefined)
            ]));
        });
        it('disregards extra semi-colons', () => {
            chai_1.expect(parser.parse(fixtures.extraSemicolons))
                .to.containSubset(nodeFactory.stylesheet([
                nodeFactory.ruleset(':host', nodeFactory.rulelist([
                    nodeFactory.declaration('margin', nodeFactory.expression('0')),
                    nodeFactory.discarded(';;'),
                    nodeFactory.declaration('padding', nodeFactory.expression('0')),
                    nodeFactory.discarded(';'),
                    nodeFactory.discarded(';'),
                    nodeFactory.declaration('display', nodeFactory.expression('block')),
                ])),
                nodeFactory.discarded(';')
            ]));
        });
    });
    describe('when extracting ranges', () => {
        it('extracts the correct ranges for discarded nodes', () => {
            const ast = parser.parse(fixtures.extraSemicolons);
            const nodes = getNodesOfType(ast, 'discarded');
            const rangeSubStrings = Array.from(nodes).map((n) => {
                return fixtures.extraSemicolons.substring(n.range.start, n.range.end);
            });
            chai_1.expect(rangeSubStrings).to.be.deep.equal([';;', ';', ';', ';']);
        });
        it('extracts the correct ranges for expression nodes', () => {
            const ast = parser.parse(fixtures.basicRuleset);
            const nodes = getNodesOfType(ast, 'expression');
            const rangeSubStrings = Array.from(nodes).map((n) => {
                return fixtures.basicRuleset.substring(n.range.start, n.range.end);
            });
            chai_1.expect(rangeSubStrings).to.be.deep.equal(['0', '0px']);
        });
        it('extracts the correct ranges for declarations', () => {
            const expectDeclarationRanges = (cssText, expectedRangeStrings, expectedNameRangeStrings) => {
                const ast = parser.parse(cssText);
                const nodes = Array.from(getNodesOfType(ast, 'declaration'));
                const rangeStrings = nodes.map((n) => {
                    return cssText.substring(n.range.start, n.range.end);
                });
                const nameRangeStrings = nodes.map((n) => {
                    return cssText.substring(n.nameRange.start, n.nameRange.end);
                });
                chai_1.expect(rangeStrings).to.be.deep.equal(expectedRangeStrings);
                chai_1.expect(nameRangeStrings).to.be.deep.equal(expectedNameRangeStrings);
            };
            expectDeclarationRanges(fixtures.basicRuleset, ['margin: 0;', 'padding: 0px'], ['margin', 'padding']);
            expectDeclarationRanges(fixtures.atRules, ['font-family: foo;'], ['font-family']);
            expectDeclarationRanges(fixtures.keyframes, ['fiz: 0%;', 'fiz: 100px;', 'buz: true;'], ['fiz', 'fiz', 'buz']);
            expectDeclarationRanges(fixtures.customProperties, ['--qux: vim;', '--foo: {\n    bar: baz;\n  };', 'bar: baz;'], ['--qux', '--foo', 'bar']);
            expectDeclarationRanges(fixtures.extraSemicolons, [
                'margin: 0;',
                'padding: 0;',
                'display: block;',
            ], ['margin', 'padding', 'display']);
            expectDeclarationRanges(fixtures.declarationsWithNoValue, ['foo;', 'bar 20px;', 'baz;'], ['foo', 'bar 20px', 'baz']);
            expectDeclarationRanges(fixtures.minifiedRuleset, ['bar:baz', 'vim:fet;'], ['bar', 'vim']);
            expectDeclarationRanges(fixtures.psuedoRuleset, ['baz:qux'], ['baz']);
            expectDeclarationRanges(fixtures.dataUriRuleset, ['bar:url(qux;gib)'], ['bar']);
            expectDeclarationRanges(fixtures.pathologicalComments, ['bar: /*baz*/vim;', 'baz: lur;'], ['bar', 'baz']);
        });
        it('extracts the correct ranges for rulesets', () => {
            const expectRulesetRanges = (cssText, expectedRangeStrings, expectedSelectorRangeStrings) => {
                const ast = parser.parse(cssText);
                const nodes = Array.from(getNodesOfType(ast, 'ruleset'));
                const rangeStrings = nodes.map((n) => {
                    return cssText.substring(n.range.start, n.range.end);
                });
                const selectorRangeStrings = nodes.map((n) => {
                    return cssText.substring(n.selectorRange.start, n.selectorRange.end);
                });
                chai_1.expect(rangeStrings).to.be.deep.equal(expectedRangeStrings);
                chai_1.expect(selectorRangeStrings)
                    .to.be.deep.equal(expectedSelectorRangeStrings);
            };
            expectRulesetRanges(fixtures.basicRuleset, [`body {\n  margin: 0;\n  padding: 0px\n}`], ['body']);
            expectRulesetRanges(fixtures.atRules, [], []);
            expectRulesetRanges(fixtures.keyframes, [
                'from {\n    fiz: 0%;\n  }',
                '99.9% {\n    fiz: 100px;\n    buz: true;\n  }'
            ], ['from', '99.9%']);
            expectRulesetRanges(fixtures.customProperties, [':root {\n  --qux: vim;\n  --foo: {\n    bar: baz;\n  };\n}'], [':root']);
            expectRulesetRanges(fixtures.extraSemicolons, [':host {\n  margin: 0;;;\n  padding: 0;;\n  ;display: block;\n}'], [':host']);
            expectRulesetRanges(fixtures.declarationsWithNoValue, ['div {\n  baz;\n}'], ['div']);
            expectRulesetRanges(fixtures.minifiedRuleset, ['.foo{bar:baz}', 'div .qux{vim:fet;}'], ['.foo', 'div .qux']);
            expectRulesetRanges(fixtures.psuedoRuleset, ['.foo:bar:not(#rif){baz:qux}'], ['.foo:bar:not(#rif)']);
            expectRulesetRanges(fixtures.dataUriRuleset, ['.foo{bar:url(qux;gib)}'], ['.foo']);
            expectRulesetRanges(fixtures.pathologicalComments, ['.foo {\n  bar: /*baz*/vim;\n}'], ['.foo']);
        });
        it('extracts the correct ranges for rulelists', () => {
            const expectRulelistRanges = (cssText, expectedRangeStrings) => {
                const ast = parser.parse(cssText);
                const nodes = Array.from(getNodesOfType(ast, 'rulelist'));
                const rangeStrings = nodes.map((n) => {
                    return cssText.substring(n.range.start, n.range.end);
                });
                chai_1.expect(rangeStrings).to.be.deep.equal(expectedRangeStrings);
            };
            expectRulelistRanges(fixtures.basicRuleset, [`{\n  margin: 0;\n  padding: 0px\n}`]);
            expectRulelistRanges(fixtures.atRules, ['{\n  font-family: foo;\n}']);
            expectRulelistRanges(fixtures.keyframes, [
                '{\n  from {\n    fiz: 0%;\n  }\n\n  99.9% ' +
                    '{\n    fiz: 100px;\n    buz: true;\n  }\n}',
                '{\n    fiz: 0%;\n  }',
                '{\n    fiz: 100px;\n    buz: true;\n  }'
            ]);
            expectRulelistRanges(fixtures.customProperties, [
                '{\n  --qux: vim;\n  --foo: {\n    bar: baz;\n  };\n}',
                '{\n    bar: baz;\n  }'
            ]);
            expectRulelistRanges(fixtures.extraSemicolons, ['{\n  margin: 0;;;\n  padding: 0;;\n  ;display: block;\n}']);
            expectRulelistRanges(fixtures.declarationsWithNoValue, ['{\n  baz;\n}']);
            expectRulelistRanges(fixtures.minifiedRuleset, ['{bar:baz}', '{vim:fet;}']);
            expectRulelistRanges(fixtures.psuedoRuleset, ['{baz:qux}']);
            expectRulelistRanges(fixtures.dataUriRuleset, ['{bar:url(qux;gib)}']);
            expectRulelistRanges(fixtures.pathologicalComments, ['{\n  bar: /*baz*/vim;\n}']);
        });
        it('extracts the correct ranges for atRules', () => {
            const expectAtRuleRanges = (cssText, expectedRangeStrings, expectedNameRangeStrings, expectedParameterRangeStrings) => {
                const ast = parser.parse(cssText);
                const nodes = Array.from(getNodesOfType(ast, 'atRule'));
                const rangeStrings = nodes.map((n) => {
                    return cssText.substring(n.range.start, n.range.end);
                });
                const rangeNameStrings = nodes.map((n) => {
                    return cssText.substring(n.nameRange.start, n.nameRange.end);
                });
                const rangeParametersStrings = nodes.map((n) => {
                    if (!n.parametersRange) {
                        return '[no parameters]';
                    }
                    return cssText.substring(n.parametersRange.start, n.parametersRange.end);
                });
                chai_1.expect(rangeStrings).to.be.deep.equal(expectedRangeStrings);
                chai_1.expect(rangeNameStrings).to.be.deep.equal(expectedNameRangeStrings);
                chai_1.expect(rangeParametersStrings)
                    .to.be.deep.equal(expectedParameterRangeStrings);
            };
            expectAtRuleRanges(fixtures.basicRuleset, [], [], []);
            expectAtRuleRanges(fixtures.atRules, [
                `@import url('foo.css');`,
                `@font-face {\n  font-family: foo;\n}`,
                `@charset 'foo';`
            ], ['import', 'font-face', 'charset'], [`url('foo.css')`, `[no parameters]`, `'foo'`]);
            expectAtRuleRanges(fixtures.keyframes, [
                '@keyframes foo {\n  from {\n    fiz: 0%;\n  }\n\n  99.9% ' +
                    '{\n    fiz: 100px;\n    buz: true;\n  }\n}',
            ], ['keyframes'], ['foo']);
            expectAtRuleRanges(fixtures.customProperties, [], [], []);
            expectAtRuleRanges(fixtures.extraSemicolons, [], [], []);
            expectAtRuleRanges(fixtures.declarationsWithNoValue, [], [], []);
            expectAtRuleRanges(fixtures.minifiedRuleset, [], [], []);
            expectAtRuleRanges(fixtures.psuedoRuleset, [], [], []);
            expectAtRuleRanges(fixtures.dataUriRuleset, [], [], []);
            expectAtRuleRanges(fixtures.pathologicalComments, ['@gak wiz;'], ['gak'], ['wiz']);
        });
    });
});
function* getNodesOfType(node, type) {
    for (const n of ast_iterator_1.iterateOverAst(node)) {
        if (n.type === type) {
            yield n;
        }
    }
}
//# sourceMappingURL=parser-test.js.map