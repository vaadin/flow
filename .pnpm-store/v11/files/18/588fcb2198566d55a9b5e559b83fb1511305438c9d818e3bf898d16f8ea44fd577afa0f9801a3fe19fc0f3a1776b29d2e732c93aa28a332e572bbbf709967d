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
import { Node } from './index';
export declare type Type = NameType | UnionType | ArrayType | FunctionType | ConstructorType | RecordType | IntersectionType | IndexableObjectType | ParamType | ParameterizedType;
export declare class NameType {
    readonly kind = "name";
    name: string;
    constructor(name: string);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class UnionType {
    readonly kind = "union";
    members: Type[];
    constructor(members: Type[]);
    traverse(): Iterable<Node>;
    /**
     * Simplify this union type:
     *
     * 1) Flatten nested unions (`foo|(bar|baz)` -> `foo|bar|baz`).
     * 2) De-duplicate identical members (`foo|bar|foo` -> `foo|bar`).
     */
    simplify(): void;
    serialize(): string;
}
export declare class ArrayType {
    readonly kind = "array";
    itemType: Type;
    constructor(itemType: Type);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class ParameterizedType {
    readonly kind = "parameterized";
    itemTypes: Type[];
    name: string;
    constructor(name: string, itemTypes: Type[]);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class FunctionType {
    readonly kind = "function";
    params: ParamType[];
    returns: Type;
    constructor(params: ParamType[], returns: Type);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class ConstructorType {
    readonly kind = "constructor";
    params: ParamType[];
    returns: NameType;
    constructor(params: ParamType[], returns: NameType);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class ParamType {
    readonly kind = "param";
    name: string;
    type: Type;
    optional: boolean;
    rest: boolean;
    description: string;
    constructor(data: {
        name: string;
        type: Type;
        optional?: boolean;
        rest?: boolean;
        description?: string;
    });
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class RecordType {
    readonly kind = "record";
    fields: ParamType[];
    constructor(fields: ParamType[]);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class IntersectionType {
    readonly kind = "intersection";
    types: Type[];
    constructor(types: Type[]);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare class IndexableObjectType {
    readonly kind = "indexableObject";
    keyType: Type;
    valueType: Type;
    constructor(keyType: Type, valueType: Type);
    traverse(): Iterable<Node>;
    serialize(): string;
}
export declare const anyType: NameType;
export declare const nullType: NameType;
export declare const undefinedType: NameType;
