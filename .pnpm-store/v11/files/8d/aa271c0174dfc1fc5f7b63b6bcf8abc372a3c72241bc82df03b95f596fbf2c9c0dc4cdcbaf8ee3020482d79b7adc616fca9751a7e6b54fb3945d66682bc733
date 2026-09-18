"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
// string, MyClass, null, undefined, any
class NameType {
    constructor(name) {
        this.kind = 'name';
        this.name = name;
    }
    *traverse() {
        yield this;
    }
    serialize() {
        return this.name;
    }
}
exports.NameType = NameType;
// foo|bar
class UnionType {
    constructor(members) {
        this.kind = 'union';
        this.members = members;
    }
    *traverse() {
        for (const m of this.members) {
            yield* m.traverse();
        }
        yield this;
    }
    /**
     * Simplify this union type:
     *
     * 1) Flatten nested unions (`foo|(bar|baz)` -> `foo|bar|baz`).
     * 2) De-duplicate identical members (`foo|bar|foo` -> `foo|bar`).
     */
    simplify() {
        const flattened = [];
        for (const m of this.members) {
            if (m.kind === 'union') {
                // Note we are not recursing here, because we assume we're being called
                // via a depth-first walk, so any union members have already been
                // simplified.
                flattened.push(...m.members);
            }
            else {
                flattened.push(m);
            }
        }
        // TODO This only de-dupes Name types. We should de-dupe Arrays and
        // Functions too.
        const deduped = [];
        const names = new Set();
        let hasNull = false;
        let hasUndefined = false;
        for (const m of flattened) {
            if (m.kind === 'name') {
                if (m.name === 'null') {
                    hasNull = true;
                }
                else if (m.name === 'undefined') {
                    hasUndefined = true;
                }
                else if (!names.has(m.name)) {
                    deduped.push(m);
                    names.add(m.name);
                }
            }
            else {
                deduped.push(m);
            }
        }
        // Always put `null` and `undefined` at the end because it's more readable.
        // Preserve declared order for everything else.
        if (hasNull) {
            deduped.push(exports.nullType);
        }
        if (hasUndefined) {
            deduped.push(exports.undefinedType);
        }
        this.members = deduped;
    }
    serialize() {
        return this.members
            .map((member) => {
            let s = member.serialize();
            if (member.kind === 'function') {
                // The function syntax is ambiguous when part of a union, so add
                // parens (e.g. `() => string|null` vs `(() => string)|null`).
                s = '(' + s + ')';
            }
            return s;
        })
            .join('|');
    }
}
exports.UnionType = UnionType;
// Array<foo>
class ArrayType {
    constructor(itemType) {
        this.kind = 'array';
        this.itemType = itemType;
    }
    *traverse() {
        yield* this.itemType.traverse();
        yield this;
    }
    serialize() {
        if (this.itemType.kind === 'name') {
            // Use the concise `foo[]` syntax when the item type is simple.
            return `${this.itemType.serialize()}[]`;
        }
        else {
            // Otherwise use the `Array<foo>` syntax which is easier to read with
            // complex types (e.g. arrays of arrays).
            return `Array<${this.itemType.serialize()}>`;
        }
    }
}
exports.ArrayType = ArrayType;
// Foo<Bar>
class ParameterizedType {
    constructor(name, itemTypes) {
        this.kind = 'parameterized';
        this.name = name;
        this.itemTypes = itemTypes;
    }
    *traverse() {
        for (const itemType of this.itemTypes) {
            yield* itemType.traverse();
        }
        yield this;
    }
    serialize() {
        const types = this.itemTypes.map((t) => t.serialize());
        return `${this.name}<${types.join(', ')}>`;
    }
}
exports.ParameterizedType = ParameterizedType;
// (foo: bar) => baz
class FunctionType {
    constructor(params, returns) {
        this.kind = 'function';
        this.params = params;
        this.returns = returns;
    }
    *traverse() {
        for (const p of this.params) {
            yield* p.traverse();
        }
        yield* this.returns.traverse();
        yield this;
    }
    serialize() {
        const params = this.params.map((param) => param.serialize());
        return `(${params.join(', ')}) => ${this.returns.serialize()}`;
    }
}
exports.FunctionType = FunctionType;
// {new(foo): bar}
class ConstructorType {
    constructor(params, returns) {
        this.kind = 'constructor';
        this.params = params;
        this.returns = returns;
    }
    *traverse() {
        for (const p of this.params) {
            yield* p.traverse();
        }
        yield* this.returns.traverse();
        yield this;
    }
    serialize() {
        const params = this.params.map((param) => param.serialize());
        return `{new(${params.join(', ')}): ${this.returns.serialize()}}`;
    }
}
exports.ConstructorType = ConstructorType;
// foo: bar
class ParamType {
    constructor(data) {
        this.kind = 'param';
        this.name = data.name;
        this.type = data.type || exports.anyType;
        this.optional = data.optional || false;
        this.rest = data.rest || false;
        this.description = data.description || '';
    }
    *traverse() {
        yield* this.type.traverse();
        yield this;
    }
    serialize() {
        let out = '';
        if (this.rest) {
            out += '...';
        }
        out += this.name;
        if (this.optional) {
            out += '?';
        }
        out += ': ' + this.type.serialize();
        return out;
    }
}
exports.ParamType = ParamType;
class RecordType {
    constructor(fields) {
        this.kind = 'record';
        this.fields = fields;
    }
    *traverse() {
        for (const f of this.fields) {
            yield* f.traverse();
        }
        yield this;
    }
    serialize() {
        const fields = this.fields.map((field) => field.serialize());
        return `{${fields.join(', ')}}`;
    }
}
exports.RecordType = RecordType;
class IntersectionType {
    constructor(types) {
        this.kind = 'intersection';
        this.types = types;
    }
    *traverse() {
        for (const t of this.types) {
            yield* t.traverse();
        }
        yield this;
    }
    serialize() {
        return this.types.map((t) => t.serialize()).join(' & ');
    }
}
exports.IntersectionType = IntersectionType;
class IndexableObjectType {
    constructor(keyType, valueType) {
        this.kind = 'indexableObject';
        this.keyType = keyType;
        this.valueType = valueType;
    }
    *traverse() {
        yield* this.keyType.traverse();
        yield* this.valueType.traverse();
        yield this;
    }
    serialize() {
        return `{[key: ${this.keyType.serialize()}]: ${this.valueType.serialize()}}`;
    }
}
exports.IndexableObjectType = IndexableObjectType;
exports.anyType = new NameType('any');
exports.nullType = new NameType('null');
exports.undefinedType = new NameType('undefined');
//# sourceMappingURL=types.js.map