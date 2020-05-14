/* tslint:disable:max-classes-per-file */

import { validate, ValidationError } from "./FormValidator";
import { AbstractModel, defaultValueSymbol, isSubmittingSymbol, ModelConstructor, valueSymbol } from "./Models";

export class Binder<T, M extends AbstractModel<T>>{
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
      throw new ValidationError(errors);
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
