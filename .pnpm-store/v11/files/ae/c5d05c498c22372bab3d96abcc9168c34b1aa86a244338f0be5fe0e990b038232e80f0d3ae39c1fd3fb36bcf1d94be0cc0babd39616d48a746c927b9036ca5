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
import { Declaration } from './declarations';
import { Node } from './index';
export interface TsLintDisable {
    ruleName: string;
    why: string;
}
export declare class Document {
    readonly kind = "document";
    path: string;
    members: Declaration[];
    referencePaths: Set<string>;
    header: string;
    isEsModule: boolean;
    tsLintDisables: TsLintDisable[];
    constructor(data: {
        path: string;
        members?: Declaration[];
        referencePaths?: Iterable<string>;
        header?: string;
        isEsModule?: boolean;
        tsLintDisables: TsLintDisable[];
    });
    /**
     * Iterate over all nodes in the document, depth first. Includes all
     * recursive ancestors, and the document itself.
     */
    traverse(): Iterable<Node>;
    /**
     * Clean up this AST.
     */
    simplify(): void;
    serialize(): string;
}
