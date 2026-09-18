import type { primitiveES5 } from './es5';

declare function ToPrimitive(
    input: unknown,
    hint?: StringConstructor | NumberConstructor,
): ToPrimitive.primitiveES6;


declare namespace ToPrimitive {
    export type primitiveES6 = primitiveES5 | symbol;
    export type unknownES6 = primitiveES6 | object;
}

export = ToPrimitive;
