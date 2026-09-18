# Installation
> `npm install --save @types/content-type`

# Summary
This package contains type definitions for content-type (https://github.com/jshttp/content-type).

# Details
Files were exported from https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/content-type.
## [index.d.ts](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/content-type/index.d.ts)
````ts
export function parse(input: RequestLike | ResponseLike | string): ParsedMediaType;
export function format(obj: MediaType): string;

export interface ParsedMediaType {
    type: string;
    parameters: { [key: string]: string };
}

export interface MediaType {
    type: string;
    parameters?: { [key: string]: string } | undefined;
}

export interface RequestLike {
    headers: { [header: string]: string | string[] | undefined };
}

export interface ResponseLike {
    getHeader(name: string): number | string | string[] | undefined;
}

````

### Additional Details
 * Last updated: Sat, 07 Jun 2025 02:15:25 GMT
 * Dependencies: none

# Credits
These definitions were written by [Hiroki Horiuchi](https://github.com/horiuchi), [BendingBender](https://github.com/BendingBender), and [Sebastian Beltran](https://github.com/bjohansebas).
