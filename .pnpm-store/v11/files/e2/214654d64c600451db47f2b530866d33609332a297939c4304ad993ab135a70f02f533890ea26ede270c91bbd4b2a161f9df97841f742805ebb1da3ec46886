import type { primitiveES6 } from './es2015';
import es5 from './es5';
import es6 from './es6';
import es2015 from './es2015';

declare function ToPrimitive(
    input: unknown,
    hint?: StringConstructor | NumberConstructor,
): ToPrimitive.primitive;

declare namespace ToPrimitive {
    export type ES5 = typeof es5;
    export const ES5: typeof es5;

    /** @deprecated */
    export type ES6 = typeof es6;
    /** @deprecated */
    export const ES6: typeof es6;

    export type ES2015 = typeof es2015;
    export const ES2015: typeof es2015;

    export type primitive = primitiveES6 | bigint;
}

export = ToPrimitive;
