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
  ArrayModel,
  binderNodeSymbol,
  getBinderNode,
  ItemModelSymbol,
  keySymbol,
  ModelConstructor,
  ModelValue,
  ObjectModel,
  parentSymbol,
  validatorsSymbol
} from "./Models";
import {Validator, ValueError} from "./Validation";

const errorsSymbol = Symbol('ownErrorsSymbol');
const visitedSymbol = Symbol('visited');

function getErrorPropertyName(valueError: ValueError<any>): string {
  return typeof valueError.property === 'string'
    ? valueError.property
    : getBinderNode(valueError.property).name;
}

export class BinderNode<T, M extends AbstractModel<T>> {
  private [visitedSymbol]: boolean = false;
  private [validatorsSymbol]: ReadonlyArray<Validator<T>>;
  private [errorsSymbol]?: ReadonlyArray<ValueError<T>>;
  private defaultArrayItemValue?: T;

  constructor(readonly model: M) {
    model[binderNodeSymbol] = this;
    this.initializeValue();
    this[validatorsSymbol] = model[validatorsSymbol];
  }

  get parent(): BinderNode<any, AbstractModel<any>> | undefined {
    const modelParent = this.model[parentSymbol];
    return modelParent instanceof AbstractModel
      ? getBinderNode(modelParent)
      : undefined;
  }

  get binder(): Binder<any, AbstractModel<any>> {
    return this.parent ? this.parent.binder : (this as any);
  }

  get name(): string {
    let model = this.model as AbstractModel<any>;
    const strings = [];
    while (model[parentSymbol] instanceof AbstractModel) {
      strings.unshift(String(model[keySymbol]));
      model = model[parentSymbol] as AbstractModel<any>;
    }
    return strings.join('.');
  }

  get value(): T {
    return this.parent!.value[this.model[keySymbol]];
  }

  set value(value: T) {
    this.setValueState(value);
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
    // TODO: Replace reduce() with flat() when the following issue is solved
    //  https://github.com/vaadin/flow/issues/8658
    const errors = (await Promise.all([
      ...this.requestValidationOfDescendants(),
      ...this.requestValidationWithAncestors()
    ])).reduce((acc, val) => acc.concat(val), [])
        .filter(valueError => valueError) as ReadonlyArray<ValueError<any>>;
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

    if (!itemValue) {
      itemValue = this.model[ItemModelSymbol].createEmptyValue();
    }
    this.value = (
      [...((this.value as unknown) as ReadonlyArray<IT>), itemValue] as unknown
    ) as T;
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

    if (!itemValue) {
      itemValue = this.model[ItemModelSymbol].createEmptyValue();
    }
    this.value = (
      [itemValue, ...((this.value as unknown) as ReadonlyArray<IT>)] as unknown
    ) as T;
  }

  /**
   * Remove itself from the parent array value.
   *
   * Requires the context model to be an array item reference.
   */
  removeSelf() {
    if (!(this.model[parentSymbol] instanceof ArrayModel)) {
      throw new TypeError('Model is not an array item');
    }
    const itemIndex = this.model[keySymbol] as number;
    this.parent!.value = ((this.parent!.value as ReadonlyArray<T>).filter((_, i) => i !== itemIndex));
  }

  protected clearValidation(): boolean {
    if (this[visitedSymbol]) {
      this[visitedSymbol] = false;
    }
    let needsUpdate = false;
    if (this[errorsSymbol]) {
      this[errorsSymbol] = undefined;
      needsUpdate = true;
    }
    if ([...this.getChildBinderNodes()]
      .filter(childBinderNode => childBinderNode.clearValidation())
      .length > 0) {
      needsUpdate = true;
    }
    return needsUpdate;
  }

  protected async updateValidation() {
    if (this[visitedSymbol]) {
      await this.validate();
    } else {
      if (this.dirty || this.invalid) {
        await Promise.all(
          [...this.getChildBinderNodes()].map(childBinderNode => childBinderNode.updateValidation())
        );
      }
    }
  }

  protected update(_?: T): void {
    if (this.parent) {
      this.parent.update();
    }
  }

  protected setErrorsWithDescendants(errors?: ReadonlyArray<ValueError<any>>) {
    const name = this.name;
    const relatedErrors = errors ?
      errors.filter(valueError => getErrorPropertyName(valueError).startsWith(name)) : undefined;
    this[errorsSymbol] = relatedErrors;
    for (const childBinderNode of this.getChildBinderNodes()) {
      childBinderNode.setErrorsWithDescendants(relatedErrors);
    }
  }

  private *getChildBinderNodes(): Generator<BinderNode<any, AbstractModel<any>>> {
    if (this.model instanceof ObjectModel) {
      for (const key of Object.keys(this.value)) {
        const childModel = (this.model as any)[key] as AbstractModel<any>;
        if (childModel) {
          yield getBinderNode(childModel);
        }
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

  private initializeValue() {
    // First, make sure parents have value initialized
    if (this.parent && ((this.parent.value === undefined) || (this.parent.defaultValue === undefined))) {
      this.parent.initializeValue();
    }

    let value = this.parent
      ? this.parent.value[this.model[keySymbol]]
      : undefined;

    if (value === undefined) {
      // Initialize value if necessary
      value = value !== undefined
        ? value
        : (this.model.constructor as ModelConstructor<T, M>).createEmptyValue()
      this.setValueState(value, this.defaultValue === undefined);
    }
  }

  private setValueState(value: T, keepPristine: boolean = false) {
    const modelParent = this.model[parentSymbol];
    if (modelParent instanceof ArrayModel) {
      // Value contained in array - replace array in parent
      const array = (this.parent!.value as ReadonlyArray<T>).slice();
      array[this.model[keySymbol] as number] = value;
      this.parent!.setValueState(array, keepPristine);
    } else if (modelParent instanceof ObjectModel) {
      // Value contained in object - replace object in parent
      const object = {
        ...this.parent!.value,
        [this.model[keySymbol]]: value
      };
      this.parent!.setValueState(object, keepPristine);
    } else {
      // Value contained elsewhere, probably binder - use value property setter
      const binder = modelParent as Binder<T, M>;
      if (keepPristine && !binder.dirty) {
        binder.defaultValue = value;
      }
      binder.value = value;
    }
  }
}
