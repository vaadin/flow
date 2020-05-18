/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import {Binder} from "./Binder";
import {BinderState} from "./BinderState";
import {
  AbstractModel, ArrayModel, binderNodeSymbol, getBinderNode,
  getName,
  getValue, keySymbol, ModelType, ObjectModel,
  parentSymbol, PrimitiveModel,
  setValue,
  validatorsSymbol
} from "./Models";
import {Required, Validator, ValueError} from "./Validation";

const errorsSymbol = Symbol('errors');
const visitedSymbol = Symbol('visited');

function getBinder(model: AbstractModel<any>): Binder<any, AbstractModel<any>> | undefined {
  const parent = model[parentSymbol];
  return parent instanceof AbstractModel
      ? getBinder(parent)
      : parent instanceof Binder ?
      parent : undefined;
}

export class BinderNode<T, M extends AbstractModel<T>> implements BinderState<T, M> {
  private [visitedSymbol]: boolean = false;
  private [validatorsSymbol]: ReadonlyArray<Validator<T>>;
  private [errorsSymbol]: ReadonlyArray<ValueError<any>> = [];

  readonly binder: Binder<any, AbstractModel<any>>;

  constructor(readonly model: M) {
    this.binder = getBinder(model)!;
    if (!this.binder) {
      return;
    }

    this.model = model;
    this.model[binderNodeSymbol] = this;
    this[validatorsSymbol] = model[validatorsSymbol];
  }

  get parent(): BinderNode<any, AbstractModel<any>> | undefined {
    const modelParent = this.model[parentSymbol];
    return modelParent instanceof AbstractModel
      ? modelParent[binderNodeSymbol]
      : undefined;
  }

  get name(): string {
    return getName(this.model);
  }

  get value(): T {
    return getValue(this.model);
  }

  set value(value: T) {
    setValue(this.model, value);
  }

  get defaultValue(): T {
    const modelParent = this.model[parentSymbol];
    return ('value' in modelParent)
      ? modelParent.value
      : (modelParent[binderNodeSymbol] as BinderNode<any, typeof modelParent>).defaultValue[this.model[keySymbol]];
  }

  get validators(): ReadonlyArray<Validator<T>> {
    return this[validatorsSymbol];
  }

  set validators(validators: ReadonlyArray<Validator<T>>) {
    this[validatorsSymbol] = validators;
  }

  for<NM extends AbstractModel<any>>(model: NM): BinderNode<ModelType<NM>, NM> {
    const binderNode = getBinderNode(model);
    if (binderNode.binder !== this.binder) {
      throw new Error('Unknown binder');
    }

    return binderNode;
  }

  async validate(): Promise<void> {
    const name = this.name;
    const errors = await Promise.all(this.requestValidationWithParents());
    this[errorsSymbol] = errors.filter(
      valueError => valueError && valueError.property.startsWith(name)
    ) as ReadonlyArray<ValueError<any>>;
  }

  async addValidator(validator: Validator<T>) {
    this.validators = [...this[validatorsSymbol], validator];
  }

  get visited() {
    return this[visitedSymbol];
  }

  set visited(v) {
    if (this[visitedSymbol] !== v) {
      this[visitedSymbol] = v;
      this.validate();
    }
  }

  get errors() {
    return this[errorsSymbol];
  }

  get ownErrors() {
    const name = this.name;
    return this.errors.filter(valueError => valueError.property === name);
  }

  get invalid() {
    return this[errorsSymbol].length > 0;
  }

  get required() {
    return !!this[validatorsSymbol].find(val => val instanceof Required);
  }

  *[Symbol.iterator](): IterableIterator<BinderNode<any, AbstractModel<any>>> {
    if (this.model instanceof PrimitiveModel) {
      return;
    } else if (this.model instanceof ObjectModel) {
      for (let key of Object.keys(this.model)) {
        if (String(key) === key) {
          const childModel = (this.model as any)[key] as AbstractModel<any>;
          yield this.for(childModel);
        }
      }
    } else if (this.model instanceof ArrayModel) {
      for (let childModel of this.model) {
        yield this.binder.for(childModel);
      }
    }
  }

  private requestValidationWithParents(): ReadonlyArray<Promise<ValueError<any> | void>> {
    return [
      ...this[validatorsSymbol].map(
        validator => this.binder.requestValidation(this.model, validator)
      ),
      ...(this.parent ? this.parent.requestValidationWithParents() : [])
    ];
  }
}
