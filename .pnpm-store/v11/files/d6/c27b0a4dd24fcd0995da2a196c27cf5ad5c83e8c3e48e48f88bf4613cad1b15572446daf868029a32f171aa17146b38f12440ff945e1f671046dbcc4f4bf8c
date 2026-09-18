/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

import {Declaration} from './declarations';
import {formatComment} from './formatting';
import {Export, Node} from './index';

export interface TsLintDisable {
  ruleName: string;
  why: string;
}

export class Document {
  readonly kind = 'document';
  path: string;
  members: Declaration[];
  referencePaths: Set<string>;
  header: string;
  isEsModule: boolean;
  tsLintDisables: TsLintDisable[];

  constructor(data: {
    path: string,
    members?: Declaration[],
    referencePaths?: Iterable<string>,
    header?: string,
    isEsModule?: boolean, tsLintDisables: TsLintDisable[],
  }) {
    this.path = data.path;
    this.members = data.members || [];
    this.referencePaths = new Set(Array.from(data.referencePaths || []));
    this.header = data.header || '';
    this.isEsModule = data.isEsModule || false;
    this.tsLintDisables = data.tsLintDisables;
  }

  /**
   * Iterate over all nodes in the document, depth first. Includes all
   * recursive ancestors, and the document itself.
   */
  * traverse(): Iterable<Node> {
    for (const m of this.members) {
      yield* m.traverse();
    }
    yield this;
  }

  /**
   * Clean up this AST.
   */
  simplify() {
    for (const node of this.traverse()) {
      if (node.kind === 'union') {
        node.simplify();
      }
    }
  }

  serialize(): string {
    let out = '';
    if (this.header) {
      out += formatComment(this.header, 0) + '\n';
    }
    const disables = this.tsLintDisables.slice();
    for (const node of this.traverse()) {
      if (node.kind === 'name' && node.name === 'any') {
        // TODO: replace `any` with `unknown`
        disables.push({
          ruleName: 'no-any',
          why: 'describes the API as best we are able today'
        });
        break;
      }
    }
    out += '\n';
    for (const disable of disables) {
      out += `// tslint:disable:${disable.ruleName} ${disable.why}\n`;
    }
    out += '\n';
    if (this.referencePaths.size > 0) {
      for (const ref of this.referencePaths) {
        out += `/// <reference path="${ref}" />\n`;
      }
      out += '\n';
    }
    out += this.members.map((m) => m.serialize()).join('\n');
    // If these are typings for an ES module, we want to be sure that TypeScript
    // will treat them as one too, which requires at least one import or export.
    if (this.isEsModule === true &&
        !this.members.some((m) => m.kind === 'import' || m.kind === 'export')) {
      out += '\n' + (new Export({identifiers: []})).serialize();
    }
    return out;
  }
}
