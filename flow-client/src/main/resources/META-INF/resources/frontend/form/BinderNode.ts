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
  AbstractModel,
  ArrayModel,
  binderNodeSymbol,
  getBinderNode,
  getName,
  getValue,
  keySymbol, ModelSymbol,
  ObjectModel,
  parentSymbol,
  setValue,
  validatorsSymbol
} from "./Models";
import {Validator, ValueError} from "./Validation";
import { Required, Size } from "./Validators";

const errorsSymbol = Symbol('ownErrorsSymbol');
const visitedSymbol = Symbol('visited');

function getErrorPropertyName(valueError: ValueError<any>) {
  return typeof valueError.property === 'string'
    ? valueError.property
    : getName(valueError.property)
}

export class BinderNode<T, M extends AbstractModel<T>> implements BinderState<T, M> {
  private [visitedSymbol]: boolean = false;
  private [validatorsSymbol]: ReadonlyArray<Validator<T>>;
  private [errorsSymbol]?: ReadonlyArray<ValueError<T>>;
  private defaultArrayItemValue?: T;

  constructor(readonly model: M) {
    this.model[binderNodeSymbol] = this;
    this[validatorsSymbol] = model[validatorsSymbol];
  }

  get parent(): BinderNode<any, AbstractModel<any>> | undefined {
    const modelParent = this.model[parentSymbol];
    return modelParent instanceof AbstractModel
      ? modelParent[binderNodeSymbol]
      : undefined;
  }

  get binder(): Binder<any, AbstractModel<any>> {
    return this.parent ? this.parent.binder : (this as any);
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
    if (this.parent && this.parent.model instanceof ArrayModel) {
      return this.parent.defaultArrayItemValue || (
        this.parent.defaultArrayItemValue = this.parent.model[ModelSymbol].createEmptyValue()
      );
    }

    return this.parent!.defaultValue[this.model[keySymbol]];
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

  for<NM extends AbstractModel<any>>(model: NM) {
    const binderNode = getBinderNode(model);
    if (binderNode.binder !== this.binder) {
      throw new Error('Unknown binder');
    }

    return binderNode;
  }

  async validate(): Promise<ReadonlyArray<ValueError<any>>> {
    const errors = (await Promise.all([
      ...this.requestValidationOfDescendants(),
      ...this.requestValidationWithAncestors()
    ])).filter(valueError => valueError) as ReadonlyArray<ValueError<any>>;
    this.setErrorsWithDescendants(errors.length ? errors : undefined);
    this.update();
    return this.errors;
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

  get errors(): ReadonlyArray<ValueError<any>> {
    return this[errorsSymbol] || [
      ...this.getChildBinderNodes()
    ].reduce((errors, childBinderNode) => [
      ...errors,
      ...childBinderNode.errors
    ], [] as ReadonlyArray<any>);
  }

  get ownErrors() {
    const name = this.name;
    return this.errors.filter(valueError => getErrorPropertyName(valueError) === name);
  }

  get invalid() {
    return this.errors.length > 0;
  }

  get required() {
    return !!this[validatorsSymbol].find(val => {
      if (val instanceof Required) {
        return true;
      } else if (val instanceof Size) {
        const min = (val as Size).value.min;
        return min && min > 0;
      }
      return false;
    });
  }

  protected async updateValidation(): Promise<ReadonlyArray<ValueError<any>>> {
    if (this[visitedSymbol]) {
      return this.validate();
    } else {
      if (this.dirty || this.invalid) {
        await Promise.all(
          [...this.getChildBinderNodes()].map(childBinderNode => childBinderNode.updateValidation())
        );
      }
      return this.errors;
    }
  }

  protected update(_?: T): void {
    if (this.parent) {
      this.parent.update();
    }
  }

  private *getChildBinderNodes(): Generator<BinderNode<any, AbstractModel<any>>> {
    if (this.model instanceof ObjectModel) {
      for (const key of Object.keys(this.model)) {
        const childModel = (this.model as any)[key] as AbstractModel<any>;
        yield getBinderNode(childModel);
      }
    } else if (this.model instanceof ArrayModel) {
      for (const childModel of this.model) {
        yield getBinderNode(childModel);
      }
    }
  }

  private runOwnValidators(): ReadonlyArray<Promise<ValueError<any> | void>> {
    return this[validatorsSymbol].map(
      validator => this.binder.requestValidation(this.model, validator)
    );
  }

  private requestValidationOfDescendants(): ReadonlyArray<Promise<ValueError<any> | void>> {
    return [...this.getChildBinderNodes()].reduce((promises, childBinderNode) => [
        ...promises,
        ...childBinderNode.runOwnValidators(),
        ...childBinderNode.requestValidationOfDescendants()
      ], [] as ReadonlyArray<Promise<ValueError<any> | void>>);
  }

  private requestValidationWithAncestors(): ReadonlyArray<Promise<ValueError<any> | void>> {
    return [
      ...this.runOwnValidators(),
      ...(this.parent ? this.parent.requestValidationWithAncestors() : [])
    ];
  }

  private setErrorsWithDescendants(errors?: ReadonlyArray<ValueError<any>>) {
    const name = this.name;
    const relatedErrors = errors ?
      errors.filter(valueError => getErrorPropertyName(valueError).startsWith(name)) : undefined;
    this[errorsSymbol] = relatedErrors;
    for (const childBinderNode of this.getChildBinderNodes()) {
      childBinderNode.setErrorsWithDescendants(relatedErrors);
    }
  }
}
