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

import {Tag} from 'doctrine';
import {formatComment, indent, quotePropertyName} from './formatting';
import {Node} from './index';
import {anyType, ParamType, Type} from './types';

/** An AST node that can appear directly in a document or namespace. */
export type Declaration = GlobalNamespace|Namespace|Class|Interface|Function|
    ConstValue|Import|Export|TypeAssignment;

export class GlobalNamespace {
  readonly kind = 'globalNamespace';
  members: Declaration[];

  constructor(members?: Declaration[]) {
    this.members = members || [];
  }

  * traverse(): Iterable<Node> {
    for (const m of this.members) {
      yield* m.traverse();
    }
    yield this;
  }

  serialize(depth: number = 0): string {
    const i = indent(depth);
    let out = `${i}`;
    if (depth === 0) {
      out += 'declare ';
    }
    out += `global {\n`;
    for (const member of this.members) {
      out += '\n' + member.serialize(depth + 1);
    }
    out += `${i}}\n`;
    return out;
  }
}

export class Namespace {
  readonly kind = 'namespace';
  name: string;
  description: string;
  style: 'namespace'|'module';
  members: Declaration[];

  constructor(data: {
    name: string,
    description?: string,
    members?: Declaration[],
    style?: 'namespace'|'module'
  }) {
    this.name = data.name;
    this.description = data.description || '';
    this.members = data.members || [];
    this.style = data.style || 'namespace';
  }

  * traverse(): Iterable<Node> {
    for (const m of this.members) {
      yield* m.traverse();
    }
    yield this;
  }

  serialize(depth: number = 0): string {
    let out = '';
    if (this.description) {
      out += formatComment(this.description, depth);
    }
    const i = indent(depth);
    out += i;
    if (depth === 0) {
      out += 'declare ';
    }
    let name = this.name;
    if (this.style === 'module') {
      // module names can have syntax that's invalid for namespaces,
      // like 'goog:foo' or './bar.js'
      name = `'${name}'`;
    }
    out += `${this.style} ${name} {\n`;
    for (const member of this.members) {
      out += '\n' + member.serialize(depth + 1);
    }
    out += `${i}}\n`;
    return out;
  }
}

export class Class {
  readonly kind = 'class';
  name: string;
  description: string;
  extends: string;
  mixins: string[];
  properties: Property[];
  methods: Method[];
  constructorMethod?: Method;

  constructor(data: {
    name: string,
    description?: string,
    extends?: string,
    mixins?: string[],
    properties?: Property[],
    methods?: Method[],
    constructorMethod?: Method
  }) {
    this.name = data.name;
    this.description = data.description || '';
    this.extends = data.extends || '';
    this.mixins = data.mixins || [];
    this.properties = data.properties || [];
    this.methods = data.methods || [];
    this.constructorMethod = data.constructorMethod;
  }

  * traverse(): Iterable<Node> {
    for (const p of this.properties) {
      yield* p.traverse();
    }
    if (this.constructorMethod) {
      yield* this.constructorMethod.traverse();
    }
    for (const m of this.methods) {
      yield* m.traverse();
    }
    yield this;
  }

  serialize(depth: number = 0): string {
    let out = '';
    const i = indent(depth);
    if (this.description) {
      out += formatComment(this.description, depth);
    }
    out += i;
    if (depth === 0) {
      out += 'declare ';
    }
    out += `class ${this.name}`;

    if (this.mixins.length) {
      const i2 = indent(depth + 1);
      out += ' extends';
      for (const mixin of this.mixins) {
        out += `\n${i2}${mixin}(`;
      }
      out += `\n${i2}${this.extends || 'Object'}`;
      out += ')'.repeat(this.mixins.length);

    } else if (this.extends) {
      out += ' extends ' + this.extends;
    }

    out += ' {\n';
    for (const property of this.properties) {
      out += property.serialize(depth + 1);
    }
    if (this.constructorMethod) {
      out += this.constructorMethod.serialize(depth + 1);
    }
    for (const method of this.methods) {
      out += method.serialize(depth + 1);
    }
    if (!out.endsWith('\n')) {
      out += '\n';
    }
    out += `${i}}\n`;
    return out;
  }
}

export class Interface {
  readonly kind = 'interface';
  name: string;
  description: string;
  extends: string[];
  properties: Property[];
  methods: Method[];

  constructor(data: {
    name: string,
    description?: string,
    extends?: string[],
    properties?: Property[],
    methods?: Method[]
  }) {
    this.name = data.name;
    this.description = data.description || '';
    this.extends = data.extends || [];
    this.properties = data.properties || [];
    this.methods = data.methods || [];
  }

  * traverse(): Iterable<Node> {
    for (const p of this.properties) {
      yield* p.traverse();
    }
    for (const m of this.methods) {
      yield* m.traverse();
    }
    yield this;
  }

  serialize(depth: number = 0): string {
    let out = '';
    const i = indent(depth);
    if (this.description) {
      out += formatComment(this.description, depth);
    }
    out += i;
    out += `interface ${this.name}`;
    if (this.extends.length) {
      out += ' extends ' + this.extends.join(', ');
    }
    out += ' {\n';
    for (const property of this.properties) {
      out += property.serialize(depth + 1);
    }
    for (const method of this.methods) {
      out += method.serialize(depth + 1);
    }
    if (!out.endsWith('\n')) {
      out += '\n';
    }
    out += `${i}}\n`;
    return out;
  }
}

export abstract class FunctionLike {
  kind?: 'method'|'function';
  name: string;
  description: string;
  params: ParamType[];
  templateTypes: string[];
  returns?: Type;
  returnsDescription: string;
  isStatic: boolean;
  ignoreTypeCheck: boolean;

  constructor(data: {
    name: string,
    description?: string,
    params?: ParamType[],
    templateTypes?: string[],
    returns?: Type,
    returnsDescription?: string,
    isStatic?: boolean,
    ignoreTypeCheck?: boolean,
  }) {
    this.name = data.name;
    this.description = data.description || '';
    this.params = data.params || [];
    this.returns = data.returns || anyType;
    this.templateTypes = data.templateTypes || [];
    this.returnsDescription = data.returnsDescription || '';
    this.isStatic = data.isStatic || false;
    this.ignoreTypeCheck = data.ignoreTypeCheck || false;
  }

  serialize(depth: number = 0): string {
    let out = '';
    const i = indent(depth);

    const annotations = [];
    for (const p of this.params) {
      if (p.description) {
        annotations.push(`@param ${p.name} ${p.description}`);
      }
    }
    if (this.returnsDescription) {
      annotations.push(`@returns ${this.returnsDescription}`);
    }

    let combinedDescription = this.description;
    if (annotations.length > 0) {
      if (combinedDescription) {
        combinedDescription += '\n\n';
      }
      combinedDescription += annotations.join('\n');
    }
    if (combinedDescription) {
      out += '\n' + formatComment(combinedDescription, depth);
    }
    if (this.ignoreTypeCheck) {
      out += indent(depth) + '// @ts-ignore\n';
    }

    out += i;
    if (depth === 0) {
      out += 'declare ';
    }
    if (this.kind === 'method' && this.isStatic) {
      out += 'static ';
    }
    if (this.kind === 'function') {
      out += 'function ';
    }
    out += this.name;
    if (this.templateTypes.length > 0) {
      out += `<${this.templateTypes.join(', ')}>`;
    }
    out += '(';
    out += this.params.map((p) => p.serialize()).join(', ');
    out += `)`;
    if (this.returns) {
      out += `: ${this.returns.serialize()}`;
    }
    out += `;\n`;
    return out;
  }
}

export class Function extends FunctionLike {
  readonly kind = 'function';

  * traverse(): Iterable<Node> {
    for (const p of this.params) {
      yield* p.traverse();
    }
    if (this.returns) {
      yield* this.returns.traverse();
    }
    yield this;
  }
}

export class Method extends FunctionLike {
  readonly kind = 'method';

  * traverse(): Iterable<Node> {
    for (const p of this.params) {
      yield* p.traverse();
    }
    if (this.returns) {
      yield* this.returns.traverse();
    }
    yield this;
  }
}

export class Property {
  readonly kind = 'property';
  name: string;
  description: string;
  type: Type;
  readOnly: boolean;
  tags: Tag[] = [];

  constructor(data: {
    name: string,
    description?: string,
    type?: Type,
    readOnly?: boolean,
    tags?: Tag[],
  }) {
    this.name = data.name;
    this.description = data.description || '';
    this.type = data.type || anyType;
    this.readOnly = data.readOnly || false;
    this.tags = data.tags || [];
  }

  * traverse(): Iterable<Node> {
    yield* this.type.traverse();
    yield this;
  }

  serialize(depth: number = 0): string {
    let out = '';
    const i = indent(depth);
    const tags = this.tags
      .filter((tag) => tag.title === 'attr' || tag.title === 'attribute')
      .map((tag) => '@' + tag.title + ' ' + tag.description).join('\n');
    if (this.description) {
      out += '\n' + formatComment(tags ? this.description + '\n' + tags : this.description, depth);
    } else if (tags) {
      out += '\n' + formatComment(tags, depth);
    }
    out += i;
    if (this.readOnly) {
      out += 'readonly ';
    }
    out += `${quotePropertyName(this.name)}: ${this.type.serialize()};\n`;
    return out;
  }
}

export class ConstValue {
  readonly kind = 'constValue';
  name: string;
  type: Type;

  constructor(data: {
    name: string,
    type: Type,
  }) {
    this.name = data.name;
    this.type = data.type;
  }

  * traverse(): Iterable<Node> {
    yield* this.type.traverse();
    yield this;
  }

  serialize(depth: number = 0): string {
    return indent(depth) + (depth === 0 ? 'declare ' : '') +
        `const ${this.name}: ${this.type.serialize()};\n`;
  }
}

/**
 * The "*" token in an import or export.
 */
export const AllIdentifiers = Symbol('*');
export type AllIdentifiers = typeof AllIdentifiers;

/**
 * An identifier that is imported, possibly with a different name.
 */
export interface ImportSpecifier {
  identifier: string|AllIdentifiers;
  alias?: string;
}

/**
 * A JavaScript module import.
 */
export class Import {
  readonly kind = 'import';
  identifiers: ImportSpecifier[];
  fromModuleSpecifier: string;
  trailingComment?: string;

  constructor(data: {
    identifiers: ImportSpecifier[]; fromModuleSpecifier: string,
  }) {
    this.identifiers = data.identifiers;
    this.fromModuleSpecifier = data.fromModuleSpecifier;
  }

  * traverse(): Iterable<Node> {
    yield this;
  }

  serialize(depth: number = 0): string {
    const i = indent(depth);
    if (this.identifiers.some((i) => i.identifier === AllIdentifiers)) {
      // Namespace imports have a different form. You can also have a default
      // import, but no named imports.
      const parts = [];
      for (const identifier of this.identifiers) {
        if (identifier.identifier === 'default') {
          parts.push(identifier.alias);
        } else if (identifier.identifier === AllIdentifiers) {
          parts.push(`* as ${identifier.alias}`);
        }
      }
      return `${i}import ${parts.join(', ')} ` +
          `from '${this.fromModuleSpecifier}';` +
          `${this.trailingComment || ''}\n`;
    }

    else {
      const parts = [];
      for (const {identifier, alias} of this.identifiers) {
        if (identifier === AllIdentifiers) {
          // Can't happen, see above.
          continue;
        }
        parts.push(
            identifier +
            (alias !== undefined && alias !== identifier ? ` as ${alias}` :
                                                           ''));
      }
      return `${i}import {${parts.join(', ')}} ` +
          `from '${this.fromModuleSpecifier}';${this.trailingComment || ''}\n`;
    }
  }
}

/**
 * An identifier that is imported, possibly with a different name.
 */
export interface ExportSpecifier {
  identifier: string;
  alias?: string;
}

/**
 * A JavaScript module export.
 */
export class Export {
  readonly kind = 'export';
  identifiers: ExportSpecifier[]|AllIdentifiers;
  fromModuleSpecifier: string;
  trailingComment?: string;

  constructor(data: {
    identifiers: ExportSpecifier[]|AllIdentifiers,
    fromModuleSpecifier?: string
  }) {
    this.identifiers = data.identifiers;
    this.fromModuleSpecifier = data.fromModuleSpecifier || '';
  }

  * traverse(): Iterable<Node> {
    yield this;
  }

  serialize(depth: number = 0): string {
    const i = indent(depth);
    let out = `${i}export `;
    if (this.identifiers === AllIdentifiers) {
      out += '*';
    } else {
      const specifiers = this.identifiers.map(({identifier, alias}) => {
        return identifier +
            (alias !== undefined && alias !== identifier ? ` as ${alias}` : '');
      });
      out += `{${specifiers.join(', ')}}`;
    }
    if (this.fromModuleSpecifier !== '') {
      out += ` from '${this.fromModuleSpecifier}'`;
    }
    out += `;${this.trailingComment || ''}\n`;
    return out;
  }
}

export class TypeAssignment {
  readonly kind = 'typeAssignment';
  name: string;
  value: Type;
  constructor(data: {name: string, value: Type}) {
    this.name = data.name;
    this.value = data.value;
  }

  * traverse(): Iterable<Node> {
    yield* this.value.traverse();
    yield this;
  }

  serialize(depth: number = 0): string {
    const i = indent(depth);
    return `${i}type ${this.name} = ${this.value.serialize()};\n`;
  }
}
