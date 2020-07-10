/* tslint:disable:max-classes-per-file */

import {BinderNode} from "./BinderNode";
import {Validator} from "./Validation";

export const ItemModelSymbol = Symbol('ItemModel');
export const parentSymbol = Symbol('parent');

export const keySymbol = Symbol('key');
export const fromStringSymbol = Symbol('fromString');
export const validatorsSymbol = Symbol('validators');
export const binderNodeSymbol = Symbol('binderNode');
export const optionalSymbol = Symbol('optional');

export const getPropertyModel = Symbol('getPropertyModel');
const properties = Symbol('properties');

interface HasFromString<T> {
  [fromStringSymbol](value: string): T;
}

export interface HasValue<T> {
  value?: T;
}

export type ModelParent<T> = AbstractModel<any> | HasValue<T>;
export type ModelValue<M extends AbstractModel<any>> = ReturnType<M["valueOf"]>;

export interface ModelConstructor<T, M extends AbstractModel<T>> {
  createEmptyValue: () => T;
  new(parent: ModelParent<T>, key: keyof any, optional: boolean, ...args: any[]): M;
}

type ModelVariableArguments<C extends ModelConstructor<any, AbstractModel<any>>> =
  C extends new (parent: ModelParent<any>, key: keyof any, ...args: infer R) => any ? R : never;

export abstract class AbstractModel<T> {
  static createEmptyValue(): unknown {
    return undefined;
  }

  readonly [parentSymbol]: ModelParent<T>;
  readonly [validatorsSymbol]: ReadonlyArray<Validator<T>>;
  readonly [optionalSymbol]: boolean;

  [binderNodeSymbol]?: BinderNode<T, this>;
  [keySymbol]: keyof any;

  constructor(
    parent: ModelParent<T>,
    key: keyof any,
    optional: boolean,
    ...validators: ReadonlyArray<Validator<T>>
  ) {
    this[parentSymbol] = parent;
    this[keySymbol] = key;
    this[optionalSymbol] = optional;
    this[validatorsSymbol] = validators;
  }

  toString() {
    return String(this.valueOf());
  }
  valueOf(): T {
    return getBinderNode(this).value;
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
    const modelInstance = new this({value: undefined}, 'value', false);
    let obj = {};
    // Iterate the model class hierarchy up to the ObjectModel, and extract
    // the property getter names from every prototypes
    for (
      let proto = Object.getPrototypeOf(modelInstance);
      proto !== ObjectModel.prototype;
      proto = Object.getPrototypeOf(proto)
    ) {
      obj = Object.getOwnPropertyNames(proto)
        .filter(propertyName => propertyName !== 'constructor')
        // Initialise the properties in the value object with empty value
        .reduce((o, propertyName) => {
          const propertyModel = (modelInstance as any)[propertyName] as AbstractModel<any>;
          // Skip initialising optional properties
          if (!propertyModel[optionalSymbol]) {
            (o as any)[propertyName] = (
              propertyModel.constructor as ModelConstructor<any, AbstractModel<any>>
            ).createEmptyValue();
          }
          return o;
        }, obj)
    }
    return obj;
  }

  private [properties]: {[name in keyof T]?: AbstractModel<T[name]>} = {};

  protected [getPropertyModel]<
    N extends keyof T,
    C extends new(parent: ModelParent<T[N]>, key: keyof any, optional: boolean, ...args: any[]) => any
  >(
    name: N,
    ValueModel: C,
    valueModelArgs: any[]
  ): InstanceType<C> {
    const [optional, ...rest] = valueModelArgs;
    return this[properties][name] !== undefined ?
      (this[properties][name] as InstanceType<C>)
      : (this[properties][name] = new ValueModel(this, name, optional, ...rest));
  }
}

export class ArrayModel<T, M extends AbstractModel<T>> extends AbstractModel<ReadonlyArray<T>> {
  static createEmptyValue() {
    return [] as ReadonlyArray<unknown>;
  }

  private readonly [ItemModelSymbol]: ModelConstructor<T, M>;
  private readonly itemModelArgs: ReadonlyArray<any>;
  private readonly itemModels: M[] = [];

  constructor(
    parent: ModelParent<ReadonlyArray<T>>,
    key: keyof any,
    optional: boolean,
    ItemModel: ModelConstructor<T, M>,
    itemModelArgs: ModelVariableArguments<typeof ItemModel>,
    ...validators: ReadonlyArray<Validator<ReadonlyArray<T>>>
  ) {
    super(parent, key, optional, ...validators);
    this[ItemModelSymbol] = ItemModel;
    this.itemModelArgs = itemModelArgs;
  }

  /**
   * Iterates the current array value and yields a binder node for every item.
   */
  *[Symbol.iterator](): IterableIterator<BinderNode<T, M>> {
    const array = getBinderNode(this).value;
    const ItemModel = this[ItemModelSymbol];
    if (array.length !== this.itemModels.length) {
      this.itemModels.length = array.length;
    }
    for (const i of array.keys()) {
      let itemModel = this.itemModels[i];
      if (!itemModel) {
        const [optional, ...rest] = this.itemModelArgs;
        itemModel = new ItemModel(this, i, optional, ...rest);
        this.itemModels[i] = itemModel;
      }
      yield getBinderNode(itemModel);
    }
  }
}

export function getBinderNode<M extends AbstractModel<any>, T = ModelValue<M>>(model: M): BinderNode<T, M> {
  return model[binderNodeSymbol] || (
    model[binderNodeSymbol] = new BinderNode(model)
  );
}
