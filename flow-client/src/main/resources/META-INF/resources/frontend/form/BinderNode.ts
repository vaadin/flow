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
import {ForEachTokenCallback} from "tslint";

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
    if (value !== this.value) {
      setValue(this.model, value);
      this.updateValidation();
    }
  }

  get defaultValue(): T {
    const modelParent = this.model[parentSymbol];
    return ('value' in modelParent)
      ? modelParent.value
      : (modelParent[binderNodeSymbol] as BinderNode<any, typeof modelParent>).defaultValue[this.model[keySymbol]];
  }

  get dirty(): boolean {
    return this.value !== this.defaultValue;
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

  async validate(): Promise<ReadonlyArray<ValueError<any>>> {
    const name = this.name;
    const descendantErrors = await this.updateValidationWithDescenants();
    if (descendantErrors.length) {
      this[errorsSymbol] = descendantErrors;
      return this[errorsSymbol];
    }

    const errors = await Promise.all(this.requestValidationWithAncestors());
    this[errorsSymbol] = errors.filter(
      valueError => valueError && valueError.property.startsWith(name)
    ) as ReadonlyArray<ValueError<any>>;
    return this[errorsSymbol];
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
      this.updateValidation();
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

  private requestValidationWithAncestors(): ReadonlyArray<Promise<ValueError<any> | void>> {
    return [
      ...this[validatorsSymbol].map(
        validator => this.binder.requestValidation(this.model, validator)
      ),
      ...(this.parent ? this.parent.requestValidationWithAncestors() : [])
    ];
  }

  private async updateValidation(): Promise<ReadonlyArray<ValueError<any>>> {
    if (this.dirty || this.visited) {
      await this.validate();
    } else {
      if (this[errorsSymbol].length) {
        this[errorsSymbol] = [];
      }
    }
  }

  private async updateValidationWithDescenants(): Promise<ReadonlyArray<ValueError<any>>> {
    const errors = await Promise.all([...this].map(childBinderNode => childBinderNode.updateValidation()));
    return (errors as any).flat();
  }
}
