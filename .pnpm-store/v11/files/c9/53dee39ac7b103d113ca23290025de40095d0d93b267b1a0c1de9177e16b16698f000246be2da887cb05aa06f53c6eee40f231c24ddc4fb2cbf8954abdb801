# Installation
> `npm install --save @types/babylon`

# Summary
This package contains type definitions for babylon (https://github.com/babel/babylon).

# Details
Files were exported from https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/babylon.
## [index.d.ts](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/babylon/index.d.ts)
````ts
import { Expression, File } from "babel-types";

export function parse(code: string, opts?: BabylonOptions): File;

export function parseExpression(input: string, options?: BabylonOptions): Expression;

export interface BabylonOptions {
    /**
     * By default, import and export declarations can only appear at a program's top level.
     * Setting this option to true allows them anywhere where a statement is allowed.
     */
    allowImportExportEverywhere?: boolean | undefined;

    /**
     * By default, a return statement at the top level raises an error. Set this to true to accept such code.
     */
    allowReturnOutsideFunction?: boolean | undefined;

    allowSuperOutsideMethod?: boolean | undefined;

    /**
     * Indicate the mode the code should be parsed in. Can be either "script" or "module".
     */
    sourceType?: "script" | "module" | undefined;

    /**
     * Correlate output AST nodes with their source filename. Useful when
     * generating code and source maps from the ASTs of multiple input files.
     */
    sourceFilename?: string | undefined;

    /**
     * Array containing the plugins that you want to enable.
     */
    plugins?: PluginName[] | undefined;
}

export type PluginName =
    | "estree"
    | "jsx"
    | "flow"
    | "typescript"
    | "classConstructorCall"
    | "doExpressions"
    | "objectRestSpread"
    | "decorators"
    | "classProperties"
    | "exportExtensions"
    | "asyncGenerators"
    | "functionBind"
    | "functionSent"
    | "dynamicImport";

````

### Additional Details
 * Last updated: Mon, 06 Nov 2023 22:41:04 GMT
 * Dependencies: [@types/babel-types](https://npmjs.com/package/@types/babel-types)

# Credits
These definitions were written by [Troy Gerwien](https://github.com/yortus), and [Marvin Hagemeister](https://github.com/marvinhagemeister).
