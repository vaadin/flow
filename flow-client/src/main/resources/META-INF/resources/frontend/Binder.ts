/* tslint:disable:max-classes-per-file */

const keySymbol = Symbol('key');
const parentSymbol = Symbol('parent');
const valueSymbol = Symbol('value');
const defaultValueSymbol = Symbol('defaultValue');
const fromStringSymbol = Symbol('fromString');
const validatorsSymbol = Symbol('validators');
const isSubmittingSymbol = Symbol('isSubmitting');
const ModelSymbol = Symbol('Model');

export type ModelParent<T> = AbstractModel<any> | Binder<T, AbstractModel<T>>;

// @ts-ignore
import { AttributeCommitter, AttributePart, directive, Part,  PropertyPart } from 'lit-html';
import { repeat } from 'lit-html/directives/repeat';

export abstract class AbstractModel<T> {
  static createEmptyValue(): unknown {
    return undefined;
  };

  private [parentSymbol]: ModelParent<T>;
  private [keySymbol]: keyof any;
  private [validatorsSymbol] = new Set<Validator<T>>();

  constructor(
    parent: ModelParent<T>,
    key: keyof any,
    // @ts-ignore — argument required for generic type argument inference
    valueConstructor: () => T,
    ...validators: Array<Validator<T>>
  ) {
    this[parentSymbol] = parent;
    this[keySymbol] = key;
    validators.forEach(validator => this[validatorsSymbol].add(validator));
  }
}

export interface ModelConstructor<T, M extends AbstractModel<T>> {
  createEmptyValue: () => T;

  new (parent: ModelParent<T>, key: keyof any, getShape: () => T, ...args: any[]): M;
}

export class Binder<T, M extends AbstractModel<T>> {
  model: M;
  private [defaultValueSymbol]: T;
  private [valueSymbol]: T;
  private [isSubmittingSymbol]: boolean = false;

  constructor(
    public context: Element,
    Model: ModelConstructor<T, M>,
    public onChange: (oldValue: T) => void
  ) {
    this.model = new Model(this, 'value', Model.createEmptyValue);
    this.reset(Model.createEmptyValue());
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

  reset(defaultValue?: T) {
    if (defaultValue !== undefined) {
      this.defaultValue = defaultValue;
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

export abstract class PrimitiveModel<T> extends AbstractModel<T>
  implements PrimitiveCompatible<T> {
  valueOf() {
    return getValue(this);
  }
}

export class BooleanModel extends PrimitiveModel<boolean>
  implements PrimitiveCompatible<boolean>, HasFromString<boolean> {
  static createEmptyValue() {
    return false;
  }

  [fromStringSymbol] = Boolean;

  constructor(parent: ModelParent<boolean>, key: keyof any) {
    super(parent, key, Boolean);
  }
}

export class NumberModel extends PrimitiveModel<number>
  implements PrimitiveCompatible<number>, HasFromString<number> {
  static createEmptyValue() {
    return 0;
  }

  [fromStringSymbol] = Number;

  constructor(parent: ModelParent<number>, key: keyof any) {
    super(parent, key, NumberModel.createEmptyValue);
  }
}

export class StringModel extends PrimitiveModel<string>
  implements PrimitiveCompatible<string>, HasFromString<string> {
  static createEmptyValue() {
    return '';
  }

  [fromStringSymbol] = String;

  constructor(parent: ModelParent<string>, key: keyof any) {
    super(parent, key, StringModel.createEmptyValue);
  }

  toString() {
    return this.valueOf();
  }
}

export class ObjectModel<T> extends AbstractModel<T> {
  static createEmptyValue() {
    const modelInstance = new this(undefined as any, defaultValueSymbol, () => {})
    return Object.keys(modelInstance).reduce(
      (obj: any, key: keyof any) => {
        obj[key] = (
          (modelInstance as any)[key].constructor as ModelConstructor<any, AbstractModel<any>>
        ).createEmptyValue();
        return obj;
      },
      {}
    );
  }

  [fromStringSymbol] = String;
}

export class ArrayModel<T, M extends AbstractModel<T>> extends AbstractModel<ReadonlyArray<T>> {
  static createEmptyValue() {
    return [];
  }

  private [ModelSymbol]: ModelConstructor<T, M>;

  constructor(
    parent: ModelParent<ReadonlyArray<T>>,
    key: keyof any,
    Model: ModelConstructor<T, M>
  ) {
    super(parent, key, ArrayModel.createEmptyValue);
    this[ModelSymbol] = Model;
  }

  *[Symbol.iterator](): IterableIterator<M> {
    const array = getValue(this);
    const Model = this[ModelSymbol];
    // @ts-ignore
    for (const [i, _] of array.entries()) {
      yield new Model(this, i, Model.createEmptyValue);
    }
  }
}

export type ValidationCallback<T> = (value: T) => boolean | Promise<boolean>;

export interface Validator<T> {
  validate: ValidationCallback<T>,
  message: string,
  // Limit
  value?: any
}

export class Required implements Validator<string> {
  message = 'invalid';
  validate = (value: any) => {
    if (typeof value === 'string' || Array.isArray(value)) {
      return value.length > 0;
    } else if (typeof value === 'number') {
      return Number.isFinite(value);
    }
    return false;
  }
}

export function getModelValidators<T>(model: AbstractModel<T>): Set<Validator<T>> {
  return model[validatorsSymbol];
}

export async function validate<T>(model: AbstractModel<T>): Promise<string | undefined> {
  const parent = model[parentSymbol];
  if (parent === undefined) {
    return;
  }
  const value = getValue(model);
  const modelValidators = getModelValidators(model);
  for (const validator of modelValidators) {
    const valid = await ((async () => validator.validate(value))());
    if (!valid) {
      return validator.message;
    }
  }
  return;
}

export function getName(model: AbstractModel<any>) {
  if (model[parentSymbol] instanceof Binder) {
    return '';
  }

  let name = String(model[keySymbol]);
  model = model[parentSymbol] as AbstractModel<any>;

  while (!(model[parentSymbol] instanceof Binder)) {
    name = `${String(model[keySymbol])}[${name}]`;
    model = model[parentSymbol] as AbstractModel<any>;
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
  ) && !![getModelValidators(model)].find(val => val instanceof Required);

  if (required !== fieldState.required) {
    fieldState.required = required;
    element.required = required;
  }
});

