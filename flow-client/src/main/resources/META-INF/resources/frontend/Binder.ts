/* tslint:disable:max-classes-per-file */

const keySymbol = Symbol('key');
const parentSymbol = Symbol('parent');
const valueSymbol = Symbol('value');
const defaultValueSymbol = Symbol('defaultValue');
const fromStringSymbol = Symbol('fromString');
const validatorsSymbol = Symbol('validators');
const isSubmittingSymbol = Symbol('isSubmitting');
const ModelSymbol = Symbol('Model');
const fieldSymbol = Symbol('field');

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
    ...validators: Array<Validator<T>>
  ) {
    this[parentSymbol] = parent;
    this[keySymbol] = key;
    validators.forEach(validator => this[validatorsSymbol].add(validator));
  }
  toString() {
    return String(this.valueOf());
  }
  valueOf():T {
    return getValue(this);
  }
}

export interface ModelConstructor<T, M extends AbstractModel<T>> {
  createEmptyValue: () => T;

  new (parent: ModelParent<T>, key: keyof any, ...args: any[]): M;
}

export class Binder<T, M extends AbstractModel<T>> {
  model: M;
  private [defaultValueSymbol]: T;
  private [valueSymbol]: T;
  private [isSubmittingSymbol]: boolean = false;

  constructor(
    public context: Element,
    Model: ModelConstructor<T, M>,
    public onChange: (oldValue?: T) => void
  ) {
    this.reset(Model.createEmptyValue());
    this.model = new Model(this, 'value');
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

  async submitTo(endpointMethod: (value: T) => Promise<T|void>): Promise<T|void> {
    const errors = await validate(this.model);
    if (errors.length) {
      return;
    }

    this[isSubmittingSymbol] = true;
    this.update(this.value);
    try {
      return endpointMethod.call(this.context, this.value);
    } finally {
      this[isSubmittingSymbol] = false;
      this.reset(this.value);
    }
  }

  private update(oldValue: T) {
    this.onChange.call(this.context, oldValue);
  }

  get isSubmitting() {
    return this[isSubmittingSymbol];
  }
}

interface HasFromString<T> {
  [fromStringSymbol](value: string): T
}

export abstract class PrimitiveModel<T> extends AbstractModel<T> {
}

export class BooleanModel extends PrimitiveModel<boolean> implements HasFromString<boolean> {
  static createEmptyValue = Boolean;
  [fromStringSymbol] = Boolean;
}

export class NumberModel extends PrimitiveModel<number> implements HasFromString<number> {
  static createEmptyValue = Number;
  [fromStringSymbol] = Number;
}

export class StringModel extends PrimitiveModel<string> implements HasFromString<string> {
  static createEmptyValue = String;
  [fromStringSymbol] = String;
}

export class ObjectModel<T> extends AbstractModel<T> {
  static createEmptyValue() {
    const modelInstance = new this(undefined as any, defaultValueSymbol)
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
    return [] as ReadonlyArray<unknown>;
  }

  private [ModelSymbol]: ModelConstructor<T, M>;
  private models = new WeakMap<any, M>();

  constructor(
    parent: ModelParent<ReadonlyArray<T>>,
    key: keyof any,
    Model: ModelConstructor<T, M>
  ) {
    super(parent, key);
    this[ModelSymbol] = Model;
  }

  *[Symbol.iterator](): IterableIterator<M> {
    const array = getValue(this);
    const Model = this[ModelSymbol];
    for (const [i, item] of array.entries()) {
      let model = this.models.get(item);
      if (!model) {
        model = new Model(this,i);
        this.models.set(item, model);
      }
      yield model;
    }
  }
}

export type ValidationCallback<T> = (value: T) => boolean | Promise<boolean>;

export interface Validator<T> {
  validate: ValidationCallback<T>,
  message: string,
  value?: any
}

export class Required implements Validator<string> {
  message = '';
  validate = (value: any) => {
    if (typeof value === 'string' || Array.isArray(value)) {
      return value.length > 0;
    } else if (typeof value === 'number') {
      return Number.isFinite(value);
    }
    return value !== undefined;
  }
}

export function getModelValidators<T>(model: AbstractModel<T>): Set<Validator<T>> {
  return model[validatorsSymbol];
}

export async function validate<T>(model: AbstractModel<T>) {
  const errors:string[] = [];

  if (model instanceof ArrayModel) {
    for (const itemModel of model) {
      errors.push(...await validate(itemModel));
    }
    return errors;
  }

  const props = Object.getOwnPropertyNames(model)
    .filter(name => (model as any)[name] instanceof AbstractModel);
  for (const prop of props) {
    const propModel = (model as any)[prop];
    const fieldElement = propModel[fieldSymbol] as FieldElement;
    if (fieldElement) {
      const error = await fieldElement.validate();
      if (error !== undefined) {
        errors.push(error);
      }
    } else {
      errors.push(...await validate(propModel));
    }
  }

  const parent = model[parentSymbol];
  if (parent === undefined) {
    return errors;
  }

  const value = getValue(model);
  const modelValidators = getModelValidators(model);
  for (const validator of modelValidators) {
    const valid = await ((async () => validator.validate(value))());
    if (!valid) {
      errors.push(validator.message);
    }
  }
  return errors;
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

export function appendItem<T, M extends AbstractModel<T>>(model: ArrayModel<T, M>, item?: T) {
  if (!item) {
    item = model[ModelSymbol].createEmptyValue();
  }
  setValue(model, [...getValue(model), item]);
}

export function prependItem<T, M extends AbstractModel<T>>(model: ArrayModel<T, M>, item?: T) {
  if (!item) {
    item = model[ModelSymbol].createEmptyValue();
  }
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

interface Field {
  required: boolean,
  invalid: boolean,
  errorMessage: string
}
interface FieldState extends Field {
  name: string,
  value: string
}
const fieldStateMap = new WeakMap<PropertyPart, FieldState>();

interface FieldElement extends Field {
  element: Element;
  validate: () => Promise<string | undefined>;
}

class VaadinFieldElement implements FieldElement {
  constructor(public element: Element & Field) {}
  validate = async () => undefined;
  set required(value: boolean) { this.element.required = value }
  set invalid(value: boolean) { this.element.invalid = value }
  set errorMessage(value: string) { this.element.errorMessage = value }
}

class GenericFieldElement implements FieldElement {
  constructor(public element: Element) {}
  validate = async () => undefined;
  set required(value: boolean) { this.setAttribute('required', value) }
  set invalid(value: boolean) { this.setAttribute('invalid', value) }
  set errorMessage(_: string) { }
  setAttribute(key: string, val: any) {
    if (val) {
      this.element.setAttribute(key, '');
    } else {
      this.element.removeAttribute(key);
    }
  }
}

// vaadin elements have a `version` static property in the class
const isVaadinElement = (elm: Element) => (elm.constructor as any).version;

export const field = directive(<T>(
  model: AbstractModel<T>,
  effect?: (element: Element) => void
) => (part: Part) => {
  const propertyPart = part as PropertyPart;
  if (!(part instanceof PropertyPart) || propertyPart.committer.name !== '..') {
    throw new Error('Only supports ...="" syntax');
  }

  let fieldState: FieldState;
  const element = propertyPart.committer.element as HTMLInputElement & Field;

  if (!fieldStateMap.has(propertyPart)) {
    fieldState = { name: '', value: '', required: false, invalid: false, errorMessage: ''};
    fieldStateMap.set(propertyPart, fieldState);

    const fieldElement:FieldElement = (model as any)[fieldSymbol] =
      isVaadinElement(element) ? new VaadinFieldElement(element) : new GenericFieldElement(element);

    fieldElement.validate = async () => {
      fieldState.value = element.value;
      setValue(model, (model as any)[fromStringSymbol](element.value));

      const message = (await validate(model))[0];
      fieldElement.invalid = fieldState.invalid = message !== undefined;
      fieldElement.errorMessage = fieldState.errorMessage = message || '';

      if (effect !== undefined) {
        effect.call(element, element);
      }
      return message;
    };

    element.oninput = element.onchange = element.onblur = fieldElement.validate;

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

  const required = !![...getModelValidators(model)].find(val => val instanceof Required);
  if (required !== fieldState.required) {
    fieldState.required = required;
    element.required = required;
  }
});

