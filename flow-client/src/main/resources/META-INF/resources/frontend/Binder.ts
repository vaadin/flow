/* tslint:disable:max-classes-per-file */

export const objectSymbol = Symbol('object');
export const keySymbol = Symbol('key');
export const parentSymbol = Symbol('parent');
export const valueSymbol = Symbol('value');
export const defaultValueSymbol = Symbol('defaultValue');
export const fromStringSymbol = Symbol('fromString');
export const constraintsSymbol = Symbol('constraints');

// export type ChildModel<T> = AbstractModel<T[keyof T]>;
export type ModelParent<T> = AbstractModel<any> | Binder<T, AbstractModel<T>>;

// @ts-ignore
import { AttributeCommitter, AttributePart, directive, Part,  PropertyPart } from 'lit-html';
import { repeat } from 'lit-html/directives/repeat';

export abstract class AbstractModel<T> {
  private [parentSymbol]: ModelParent<T>;
  private [keySymbol]: keyof any;
  private [constraintsSymbol] = new Set<Constraint<T>>();

  constructor(
    parent: ModelParent<T>,
    key: keyof any,
    ...constraints: Array<Constraint<T>>
  ) {
    this[parentSymbol] = parent;
    this[keySymbol] = key;
    constraints.forEach(constraint => this[constraintsSymbol].add(constraint));
  }

  abstract get [defaultValueSymbol](): T;
}

export type ModelConstructor<M extends AbstractModel<T>, T> =
  new (parent: ModelParent<T>, key: keyof any,  ...args: any[]) => M;

const isSubmittingSymbol = Symbol('isSubmitting');

export class Binder<T, M extends AbstractModel<T>> {
  model: M;
  private [defaultValueSymbol]: T;
  private [valueSymbol]: T;
  private [isSubmittingSymbol]: boolean = false;

  constructor(
    public context: Element,
    Model: ModelConstructor<M, T>,
    public onChange: (oldValue: T) => void
  ) {
    this.model = new Model(this, 'value');
    this.reset(this.model[defaultValueSymbol]);
  }

  get defaultValue() {
    return this[defaultValueSymbol];
  }

  set defaultValue(newValue) {
    this[defaultValueSymbol] = newValue;
  }

  get value() {
    return this[valueSymbol];
  }

  set value(newValue) {
    if (newValue === this[valueSymbol]) {
      return;
    }

    const oldValue = this[valueSymbol];
    this[valueSymbol] = newValue;
    this.update(oldValue);
  }

  get isDirty() {
    return this[valueSymbol] !== this[defaultValueSymbol];
  }

  reset(newDefaultValue?: T) {
    if (newDefaultValue !== undefined) {
      this.defaultValue = newDefaultValue;
    }

    this.value = this.defaultValue;
  }

  async submitTo(endpointMethod: (value: T) => Promise<void>) {
    this[isSubmittingSymbol] = true;
    this.update(this.value);
    try {
      await endpointMethod.call(this.context, this.value);
    } finally {
      this[isSubmittingSymbol] = false;
      this.update(this.value);
    }
  }

  private update(oldValue: T) {
    this.onChange.call(this.context, oldValue);
  }

  get isSubmitting() {
    return this[isSubmittingSymbol];
  }
}

interface PrimitiveCompatible<T> {
  valueOf(): T;
}

interface HasFromString<T> {
  [fromStringSymbol](value: string): T
}

export function fromString<T>(
  model: AbstractModel<T> & HasFromString<T>,
  value: string
): T {
  return model[fromStringSymbol](value);
}

export abstract class PrimitiveModel<T> extends AbstractModel<T>
  implements PrimitiveCompatible<T> {
  valueOf() {
    return getValue(this);
  }
}

export class BooleanModel extends PrimitiveModel<boolean>
  implements PrimitiveCompatible<boolean>, HasFromString<boolean> {
  [fromStringSymbol] = Boolean;

  get [defaultValueSymbol]() {
    return false;
  }
}

export class NumberModel extends PrimitiveModel<number>
  implements PrimitiveCompatible<number>, HasFromString<number> {
  [fromStringSymbol] = Number;

  get [defaultValueSymbol]() {
    return 0;
  }
}

export class StringModel extends PrimitiveModel<string>
  implements PrimitiveCompatible<string>, HasFromString<string> {
  [fromStringSymbol] = String;
  toString() {
    return this.valueOf();
  }

  get [defaultValueSymbol]() {
    return '';
  }
}

export class ObjectModel<T> extends AbstractModel<T> {
  get [defaultValueSymbol]() {
    return Object.keys(this).reduce((obj: any, key: string) => {
      obj[key] = ((this as any)[key] as AbstractModel<any>)[defaultValueSymbol];
      return obj;
    }, {}) as T;
  }
}

export const ModelSymbol = Symbol('Model');

// type ReadonlyArrayItem<T> = T extends ReadonlyArray<infer I> ? I : never;
// export type ReadonlyArrayItemModel<T> = AbstractModel<ReadonlyArray<T>, number>;
// @ts-ignore
type ModelType<M extends AbstractModel<any>> = M extends AbstractModel<infer T> ? T : never;

export class ArrayModel<T, M extends AbstractModel<T>> extends AbstractModel<ReadonlyArray<T>> {
  private [ModelSymbol]: ModelConstructor<M, T>;

  constructor(
    parent: ModelParent<ReadonlyArray<T>>,
    key: keyof any,
    Model: ModelConstructor<M, T>
  ) {
    super(parent, key);
    this[ModelSymbol] = Model;
  }

  *[Symbol.iterator](): IterableIterator<M> {
    const array = getValue(this);
    const Model = this[ModelSymbol];
    // @ts-ignore
    for (const [i, _] of array.entries()) {
      yield new Model(this, i);
    }
  }

  get [defaultValueSymbol]() {
    return [] as ReadonlyArray<T>;
  }
}

export type ValidationCallback<T> = (value: T) => boolean | Promise<boolean>;

export interface Validator<T> {
  validate: ValidationCallback<T>,
  message: string,
  // Limit
  value?: any
}

export type Constraint<T> = [ValidationCallback<T>, string];

export const requiredConstraint = [(value: string | number | any[]) => {
  if (typeof value === 'string' || Array.isArray(value)) {
    return value.length > 0;
  } else if (typeof value === 'number') {
    return Number.isFinite(value);
  }
  return false;
}, 'Cannot be missing'] as Constraint<string | number | any[]>;

export function getModelConstraints<T>(model: AbstractModel<T>): Set<Constraint<T>> {
  return model[constraintsSymbol];
}

export async function validate<T>(model: AbstractModel<T>): Promise<string | undefined> {
  const parent = model[parentSymbol];
  if (parent === undefined) {
    return;
  }

  const value = getValue(model);

  const modelConstraints = getModelConstraints(model);
  for (const [callback, message] of modelConstraints) {
    const valid = await ((async () => callback(value))());
    if (!valid) {
      return message;
    }
  }

  return;
}

export function getName(model: AbstractModel<any>) {
  let name = String(model[keySymbol]);
  while (!(model[parentSymbol] instanceof Binder)) {
    model = model[parentSymbol] as AbstractModel<any>;
    name = `${String(model[keySymbol])}[${name}]`;
  }
  return name;
}

export function getValue<T>(model: AbstractModel<T>): T {
  const parent = model[parentSymbol];
  return (parent instanceof Binder)
    ? parent.value
    : getValue(parent)[model[keySymbol]];
}

export function setValue<T>(model: AbstractModel<T>, value: T) {
  const parent = model[parentSymbol];
  if (parent instanceof Binder) {
    parent.value = value;
  } else if (parent instanceof ArrayModel) {
    const array = getValue(parent).slice();
    array[model[keySymbol] as number] = value;
    setValue(parent, array);
  } else {
    setValue(parent, {
      ...getValue(parent),
      [model[keySymbol]]: value
    });
  }
}

export function appendItem<T, M extends AbstractModel<T>>(model: ArrayModel<T, M>, item: T) {
  setValue(model, [...getValue(model), item]);
}

export function prependItem<T, M extends AbstractModel<T>>(model: ArrayModel<T, M>, item: T) {
  setValue(model, [item, ...getValue(model)]);
}

export function removeItem<T, M extends AbstractModel<T>>(model: M) {
  if (!(model[parentSymbol] instanceof ArrayModel)) {
    throw new TypeError('Not an ArrayModel child');
  }
  const arrayModel = model[parentSymbol] as ArrayModel<T, M>;
  const itemIndex = model[keySymbol] as number;
  setValue(arrayModel, getValue(arrayModel).filter((_, i) => i !== itemIndex));
}

export type KeyFn<T, M extends AbstractModel<T>> = (model: M, value: T, index: number) => unknown;
export type ItemTemplate<T, M extends AbstractModel<T>> = (model: M, value: T, index: number) => unknown;

export const modelRepeat = <T, M extends AbstractModel<T>>(
  model: ArrayModel<T, M>,
  keyFnOrTemplate: KeyFn<T, M> | ItemTemplate<T, M>,
  itemTemplate?: ItemTemplate<T, M>) =>
  (itemTemplate !== undefined)
    ? repeat(
      model,
    (itemModel, index) => keyFnOrTemplate(itemModel, getValue(itemModel), index),
    (itemModel, index) => itemTemplate(itemModel, getValue(itemModel), index)
    )
    : repeat(
      model,
    (itemModel, index) => keyFnOrTemplate(itemModel, getValue(itemModel), index)
    );

interface FieldState {
  name: string,
  value: string,
  required: boolean,
  invalid: boolean,
  errorMessage: string
}
const fieldStateMap = new WeakMap<PropertyPart, FieldState>();

export const field = directive(<T>(
  model: AbstractModel<T>,
  effect?: (element: Element) => void
) => (part: Part) => {
  const propertyPart = part as PropertyPart;
  if (!(part instanceof PropertyPart) || propertyPart.committer.name !== '..') {
    throw new Error('Only supports ...="" syntax');
  }

  let fieldState: FieldState;
  const element = propertyPart.committer.element as HTMLInputElement;
  if (!fieldStateMap.has(propertyPart)) {
    fieldState = {
      name: '',
      value: '',
      required: false,
      invalid: false,
      errorMessage: ''
    };
    fieldStateMap.set(propertyPart, fieldState);
    // @ts-ignore
    element.oninput = element.onchange = (event: Event) => {
      fieldState.value = element.value;
      // @ts-ignore
      setValue(model, model[fromStringSymbol](element.value));
      validate(model).then((message?: string) => {
        fieldState.invalid = message !== undefined;
        fieldState.errorMessage = message || '';
        // @ts-ignore
        if (element.invalid !== fieldState.invalid) {
          // @ts-ignore
          element.invalid = fieldState.invalid;
        }
        // @ts-ignore
        if (element.errorMessage !== fieldState.errorMessage) {
          // @ts-ignore
          element.errorMessage = fieldState.errorMessage;
        }
      });
      if (effect !== undefined) {
        effect.call(element, element);
      }
    };
    element.checkValidity = () => !fieldState.invalid;
  } else {
    fieldState = fieldStateMap.get(propertyPart)!;
  }

  const name = getName(model);
  if (name !== fieldState.name) {
    fieldState.name = name;
    element.setAttribute('name', name);
  }

  const value = String(getValue(model));
  if (value !== fieldState.value) {
    element.value = value;
  }

  const required = (
    (model instanceof StringModel)
    || (model instanceof NumberModel)
    || (model instanceof BooleanModel)
    || (model instanceof ArrayModel)
  ) && (getModelConstraints(model) as Set<Constraint<any>>).has(requiredConstraint);
  if (required !== fieldState.required) {
    fieldState.required = required;
    element.required = required;
  }
});

