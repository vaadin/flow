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
import {
  AbstractModel,
  appendItem,
  ArrayModel,
  binderNodeSymbol,
  getBinderNode,
  getName,
  getValue,
  ItemModelSymbol,
  keySymbol,
  ModelValue,
  ObjectModel,
  parentSymbol,
  prependItem,
  removeItem,
  setValue,
  validatorsSymbol
} from "./Models";
import {Validator, ValueError} from "./Validation";

const errorsSymbol = Symbol('ownErrorsSymbol');
const visitedSymbol = Symbol('visited');

function getErrorPropertyName(valueError: ValueError<any>) {
  return typeof valueError.property === 'string'
    ? valueError.property
    : getName(valueError.property)
}

export class BinderNode<T, M extends AbstractModel<T>> {
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
        this.parent.defaultArrayItemValue = this.parent.model[ItemModelSymbol].createEmptyValue()
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
    ])).flat().filter(valueError => valueError) as ReadonlyArray<ValueError<any>>;
    this.setErrorsWithDescendants(errors.length ? errors : undefined);
    this.update();
    return this.errors;
  }

  addValidator(validator: Validator<T>) {
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
    return this[validatorsSymbol].some(validator => validator.impliesRequired);
  }

  /**
   * Append an item to the array value.
   *
   * Requires the context model to be an array reference.
   *
   * @param itemValue optional new item value, an empty item is
   * appended if the argument is omitted
   */
  appendItem<IT extends ModelValue<M extends ArrayModel<any, infer IM> ? IM : never>>(itemValue?: IT) {
    if (!(this.model instanceof ArrayModel)) {
      throw new Error('Model is not an array');
    }

    appendItem(this.model as ArrayModel<IT, AbstractModel<IT>>, itemValue);
  }

  /**
   * Prepend an item to the array value.
   *
   * Requires the context model to be an array reference.
   *
   * @param itemValue optional new item value, an empty item is prepended if
   * the argument is omitted
   */
  prependItem<IT extends ModelValue<M extends ArrayModel<any, infer IM> ? IM : never>>(itemValue?: IT) {
    if (!(this.model instanceof ArrayModel)) {
      throw new Error('Model is not an array');
    }

    prependItem(this.model as ArrayModel<IT, AbstractModel<IT>>, itemValue);
  }

  /**
   * Remove the item from the parent array value.
   *
   * Requires the context model to be an array item reference.
   */
  removeItem() {
    removeItem(this.model);
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
        yield childModel;
      }
    }
  }

  private runOwnValidators(): ReadonlyArray<Promise<ReadonlyArray<ValueError<any>>>> {
    return this[validatorsSymbol].map(
      validator => this.binder.requestValidation(this.model, validator)
    );
  }

  private requestValidationOfDescendants(): ReadonlyArray<Promise<ReadonlyArray<ValueError<any>>>> {
    return [...this.getChildBinderNodes()].reduce((promises, childBinderNode) => [
        ...promises,
        ...childBinderNode.runOwnValidators(),
        ...childBinderNode.requestValidationOfDescendants()
      ], [] as ReadonlyArray<Promise<ReadonlyArray<ValueError<any>>>>);
  }

  private requestValidationWithAncestors(): ReadonlyArray<Promise<ReadonlyArray<ValueError<any>>>> {
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
