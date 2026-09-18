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
const parser_1 = require("../shady-css/parser");
const stringifier_1 = require("../shady-css/stringifier");
const fixtures = require("./fixtures");
const test_node_factory_1 = require("./test-node-factory");
describe('Stringifier', () => {
    let nodeFactory;
    let stringifier;
    beforeEach(() => {
        nodeFactory = new test_node_factory_1.TestNodeFactory();
        stringifier = new stringifier_1.Stringifier();
    });
    describe('when stringifying CSS nodes', () => {
        it('can stringify an empty Stylesheet', () => {
            const cssText = stringifier.stringify(nodeFactory.stylesheet([]));
            chai_1.expect(cssText).to.be.eql('');
        });
        it('can stringify an At Rule without a Rulelist', () => {
            const cssText = stringifier.stringify(nodeFactory.atRule('foo', '("bar")'));
            chai_1.expect(cssText).to.be.eql('@foo ("bar");');
        });
        it('can stringify an At Rule with a Rulelist', () => {
            const cssText = stringifier.stringify(nodeFactory.atRule('foo', '("bar")', nodeFactory.rulelist([])));
            chai_1.expect(cssText).to.be.eql('@foo ("bar"){}');
        });
        it('can stringify Comments', () => {
            const cssText = stringifier.stringify(nodeFactory.comment('/* hi */'));
            chai_1.expect(cssText).to.be.eql('/* hi */');
        });
        it('can stringify Rulesets', () => {
            const cssText = stringifier.stringify(nodeFactory.ruleset('.fiz #buz', nodeFactory.rulelist([])));
            chai_1.expect(cssText).to.be.eql('.fiz #buz{}');
        });
        it('can stringify Declarations with Expression values', () => {
            const cssText = stringifier.stringify(nodeFactory.declaration('color', nodeFactory.expression('red')));
            chai_1.expect(cssText).to.be.eql('color:red;');
        });
        it('can stringify Declarations with Rulelist values', () => {
            const cssText = stringifier.stringify(nodeFactory.declaration('--mixin', nodeFactory.rulelist([])));
            chai_1.expect(cssText).to.be.eql('--mixin:{};');
        });
    });
    describe('when stringifying CSS ASTs', () => {
        let parser;
        beforeEach(() => {
            parser = new parser_1.Parser();
        });
        it('can stringify a basic ruleset', () => {
            const cssText = stringifier.stringify(parser.parse(fixtures.basicRuleset));
            chai_1.expect(cssText).to.be.eql('body{margin:0;padding:0px;}');
        });
        it('can stringify at rules', () => {
            const cssText = stringifier.stringify(parser.parse(fixtures.atRules));
            chai_1.expect(cssText).to.be.eql('@import url(\'foo.css\');@font-face{font-family:foo;}@charset \'foo\';');
        });
        it('can stringify keyframes', () => {
            const cssText = stringifier.stringify(parser.parse(fixtures.keyframes));
            chai_1.expect(cssText).to.be.eql('@keyframes foo{from{fiz:0%;}99.9%{fiz:100px;buz:true;}}');
        });
        it('can stringify declarations without value', () => {
            const cssText = stringifier.stringify(parser.parse(fixtures.declarationsWithNoValue));
            chai_1.expect(cssText).to.be.eql('foo;bar 20px;div{baz;}');
        });
        it('can stringify custom properties', () => {
            const cssText = stringifier.stringify(parser.parse(fixtures.customProperties));
            chai_1.expect(cssText).to.be.eql(':root{--qux:vim;--foo:{bar:baz;};}');
        });
        describe('with discarded nodes', () => {
            it('stringifies to a corrected stylesheet', () => {
                const cssText = stringifier.stringify(parser.parse(fixtures.pathologicalComments));
                chai_1.expect(cssText).to.be.eql('.foo{bar:/*baz*/vim;}/* unclosed\n@fiz {\n  --huk: {\n    /* buz */baz:lur;@gak wiz;');
            });
        });
    });
});
//# sourceMappingURL=stringifier-test.js.map