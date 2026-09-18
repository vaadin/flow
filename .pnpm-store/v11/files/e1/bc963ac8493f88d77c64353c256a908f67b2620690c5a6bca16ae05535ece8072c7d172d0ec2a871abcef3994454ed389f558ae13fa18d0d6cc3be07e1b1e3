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

import {Node} from './index';

// A TypeScript type expression.
export type Type = NameType|UnionType|ArrayType|FunctionType|ConstructorType|
    RecordType|IntersectionType|IndexableObjectType|ParamType|ParameterizedType;

// string, MyClass, null, undefined, any
export class NameType {
  readonly kind = 'name';
  name: string;

  constructor(name: string) {
    this.name = name;
  }

  * traverse(): Iterable<Node> {
    yield this;
  }

  serialize(): string {
    return this.name;
  }
}

// foo|bar
export class UnionType {
  readonly kind = 'union';
  members: Type[];

  constructor(members: Type[]) {
    this.members = members;
  }

  * traverse(): Iterable<Node> {
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
      } else {
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
        } else if (m.name === 'undefined') {
          hasUndefined = true;
        } else if (!names.has(m.name)) {
          deduped.push(m);
          names.add(m.name);
        }
      } else {
        deduped.push(m);
      }
    }
    // Always put `null` and `undefined` at the end because it's more readable.
    // Preserve declared order for everything else.
    if (hasNull) {
      deduped.push(nullType);
    }
    if (hasUndefined) {
      deduped.push(undefinedType);
    }
    this.members = deduped;
  }

  serialize(): string {
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

// Array<foo>
export class ArrayType {
  readonly kind = 'array';
  itemType: Type;

  constructor(itemType: Type) {
    this.itemType = itemType;
  }

  * traverse(): Iterable<Node> {
    yield* this.itemType.traverse();
    yield this;
  }

  serialize(): string {
    if (this.itemType.kind === 'name') {
      // Use the concise `foo[]` syntax when the item type is simple.
      return `${this.itemType.serialize()}[]`;
    } else {
      // Otherwise use the `Array<foo>` syntax which is easier to read with
      // complex types (e.g. arrays of arrays).
      return `Array<${this.itemType.serialize()}>`;
    }
  }
}

// Foo<Bar>
export class ParameterizedType {
  readonly kind = 'parameterized';
  itemTypes: Type[];
  name: string;

  constructor(name: string, itemTypes: Type[]) {
    this.name = name;
    this.itemTypes = itemTypes;
  }

  * traverse(): Iterable<Node> {
    for (const itemType of this.itemTypes) {
      yield* itemType.traverse();
    }
    yield this;
  }

  serialize(): string {
    const types = this.itemTypes.map((t) => t.serialize());
    return `${this.name}<${types.join(', ')}>`;
  }
}

// (foo: bar) => baz
export class FunctionType {
  readonly kind = 'function';
  params: ParamType[];
  returns: Type;

  constructor(params: ParamType[], returns: Type) {
    this.params = params;
    this.returns = returns;
  }

  * traverse(): Iterable<Node> {
    for (const p of this.params) {
      yield* p.traverse();
    }
    yield* this.returns.traverse();
    yield this;
  }

  serialize(): string {
    const params = this.params.map((param) => param.serialize());
    return `(${params.join(', ')}) => ${this.returns.serialize()}`;
  }
}

// {new(foo): bar}
export class ConstructorType {
  readonly kind = 'constructor';
  params: ParamType[];
  returns: NameType;

  constructor(params: ParamType[], returns: NameType) {
    this.params = params;
    this.returns = returns;
  }

  * traverse(): Iterable<Node> {
    for (const p of this.params) {
      yield* p.traverse();
    }
    yield* this.returns.traverse();
    yield this;
  }

  serialize(): string {
    const params = this.params.map((param) => param.serialize());
    return `{new(${params.join(', ')}): ${this.returns.serialize()}}`;
  }
}

// foo: bar
export class ParamType {
  readonly kind = 'param';
  name: string;
  type: Type;
  optional: boolean;
  rest: boolean;
  description: string;

  constructor(data: {
    name: string,
    type: Type,
    optional?: boolean,
    rest?: boolean,
    description?: string
  }) {
    this.name = data.name;
    this.type = data.type || anyType;
    this.optional = data.optional || false;
    this.rest = data.rest || false;
    this.description = data.description || '';
  }

  * traverse(): Iterable<Node> {
    yield* this.type.traverse();
    yield this;
  }

  serialize(): string {
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

export class RecordType {
  readonly kind = 'record';
  fields: ParamType[];

  constructor(fields: ParamType[]) {
    this.fields = fields;
  }

  * traverse(): Iterable<Node> {
    for (const f of this.fields) {
      yield* f.traverse();
    }
    yield this;
  }

  serialize(): string {
    const fields = this.fields.map((field) => field.serialize());
    return `{${fields.join(', ')}}`;
  }
}

export class IntersectionType {
  readonly kind = 'intersection';
  types: Type[];

  constructor(types: Type[]) {
    this.types = types;
  }

  * traverse(): Iterable<Node> {
    for (const t of this.types) {
      yield* t.traverse();
    }
    yield this;
  }

  serialize(): string {
    return this.types.map((t) => t.serialize()).join(' & ');
  }
}

export class IndexableObjectType {
  readonly kind = 'indexableObject';
  keyType: Type;
  valueType: Type;

  constructor(keyType: Type, valueType: Type) {
    this.keyType = keyType;
    this.valueType = valueType;
  }

  * traverse(): Iterable<Node> {
    yield* this.keyType.traverse();
    yield* this.valueType.traverse();
    yield this;
  }

  serialize(): string {
    return `{[key: ${this.keyType.serialize()}]: ${
        this.valueType.serialize()}}`;
  }
}

export const anyType = new NameType('any');
export const nullType = new NameType('null');
export const undefinedType = new NameType('undefined');
