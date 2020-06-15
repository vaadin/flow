/* tslint:disable:max-classes-per-file */

import {BinderNode} from "./BinderNode";
import {Validator} from "./Validation";

export const ModelSymbol = Symbol('Model');
export const parentSymbol = Symbol('parent');

export const keySymbol = Symbol('key');
export const fromStringSymbol = Symbol('fromString');
export const validatorsSymbol = Symbol('validators');
export const binderNodeSymbol = Symbol('binderNode');

interface HasFromString<T> {
  [fromStringSymbol](value: string): T
}

export interface HasValue<T> {
  value: T
}

export type ModelParent<T> = AbstractModel<any> | HasValue<T>;
export type ModelValue<M extends AbstractModel<any>> = ReturnType<M["valueOf"]>;

export interface ModelConstructor<T, M extends AbstractModel<T>> {
  createEmptyValue: () => T;
  new(parent: ModelParent<T>, key: keyof any, ...args: any[]): M;
}

export abstract class AbstractModel<T> {
  static createEmptyValue(): unknown {
    return undefined;
  };

  readonly [parentSymbol]: ModelParent<T>;
  readonly [validatorsSymbol]: ReadonlyArray<Validator<T>>;

  [binderNodeSymbol]?: BinderNode<T, this>;

  private [keySymbol]: keyof any;

  constructor(
    parent: ModelParent<T>,
    key: keyof any,
    ...validators: ReadonlyArray<Validator<T>>
  ) {
    this[parentSymbol] = parent;
    this[keySymbol] = key;
    this[validatorsSymbol] = validators;
  }

  toString() {
    return String(this.valueOf());
  }
  valueOf(): T {
    return getValue(this);
  }
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
    const modelInstance = new this({value: undefined as any}, 'value')
    return Object.keys(modelInstance).reduce(
      (obj: any, key: keyof any) => {
        (obj = (obj || {}))[key] = (
          (modelInstance as any)[key].constructor as ModelConstructor<any, AbstractModel<any>>
        ).createEmptyValue();
        return obj;
      }, null);
  }
}

export class ArrayModel<T, M extends AbstractModel<T>> extends AbstractModel<ReadonlyArray<T>> {
  static createEmptyValue() {
    return [] as ReadonlyArray<unknown>;
  }

  private [ModelSymbol]: ModelConstructor<T, M>;
  private models: M[] = [];

  constructor(
    parent: ModelParent<ReadonlyArray<T>>,
    key: keyof any,
    Model: ModelConstructor<T, M>,
    ...validators: ReadonlyArray<Validator<ReadonlyArray<T>>>
  ) {
    super(parent, key, ...validators);
    this[ModelSymbol] = Model;
  }

  /**
   * Iterates the current array value and yields a binder node for every item.
   */
  *[Symbol.iterator](): IterableIterator<BinderNode<T, M>> {
    const array = getValue(this);
    const Model = this[ModelSymbol];
    if (array.length !== this.models.length) {
      this.models.length = array.length;
    }
    for (const i of array.keys()) {
      let model = this.models[i];
      if (!model) {
        model = new Model(this, i);
        this.models[i] = model;
      }
      yield getBinderNode(model);
    }
  }
}

export function getBinderNode<M extends AbstractModel<any>, T = ModelValue<M>>(model: M): BinderNode<T, M> {
  return model[binderNodeSymbol] || (
    model[binderNodeSymbol] = new BinderNode(model)
  );
}

export function getName(model: AbstractModel<any>) {
  if ('value' in model[parentSymbol]) {
    return '';
  }

  let name = String(model[keySymbol]);
  model = model[parentSymbol] as AbstractModel<any>;

  while (!('value' in model[parentSymbol])) {
    name = `${String(model[keySymbol])}.${name}`;
    model = model[parentSymbol] as AbstractModel<any>;
  }

  return name;
}

export function getValue<T>(model: AbstractModel<T>): T {
  const parent = model[parentSymbol];
  return ('value' in parent)
    ? parent.value
    : getValue(parent)[model[keySymbol]];
}

export function setValue<T>(model: AbstractModel<T>, value: T) {
  const parent = model[parentSymbol];
  if (value === getValue(model)) {
    return;
  }

  if ('value' in parent) {
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

/**
 * Append an item to the array model’s value.
 *
 * @param model the array model
 * @param itemValue optional new item value, empty item is
 * appended if omitted
 */
export function appendItem<T, M extends AbstractModel<T>>(model: ArrayModel<T, M>, itemValue?: T) {
  if (!itemValue) {
    itemValue = model[ModelSymbol].createEmptyValue();
  }
  setValue(model, [...getValue(model), itemValue]);
}

/**
 * Prepend an item to the array model’s value.
 *
 * @param model the array model
 * @param itemValue optional new item value, empty item is
 * prepended if omitted
 */
export function prependItem<T, M extends AbstractModel<T>>(model: ArrayModel<T, M>, itemValue?: T) {
  if (!itemValue) {
    itemValue = model[ModelSymbol].createEmptyValue();
  }
  setValue(model, [itemValue, ...getValue(model)]);
}

/**
 * Remove the item from its parent array.
 *
 * @param model the array item model
 */
export function removeItem<M extends AbstractModel<any>>(model: M) {
  if (!(model[parentSymbol] instanceof ArrayModel)) {
    throw new TypeError('Model is not an array item');
  }
  const arrayModel = model[parentSymbol] as ArrayModel<any, M>;
  const itemIndex = model[keySymbol] as number;
  setValue(arrayModel, getValue(arrayModel).filter((_, i) => i !== itemIndex));
}
