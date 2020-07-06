import {BinderNode} from "./BinderNode";
import {
  AbstractModel,
  HasValue,
  ModelConstructor,
  parentSymbol
} from "./Models";
import {
  runValidator,
  ServerValidator,
  ValidationError,
  Validator,
  ValueError
} from "./Validation";

import {FieldStrategy, getDefaultFieldStrategy} from "./Field";

const submittingSymbol = Symbol('submitting');
const defaultValueSymbol = Symbol('defaultValue');
const valueSymbol = Symbol('value');
const emptyValueSymbol = Symbol('emptyValue');
const onChangeSymbol = Symbol('onChange');
const onSubmitSymbol = Symbol('onSubmit');
const validationsSymbol = Symbol('validations');
const validatingSymbol = Symbol('validating');
const validationRequestSymbol = Symbol('validationRequest');

export class Binder<T, M extends AbstractModel<T>> extends BinderNode<T, M> {
  private [defaultValueSymbol]: T;
  private [valueSymbol]: T;
  private [emptyValueSymbol]: T;
  private [submittingSymbol]: boolean = false;
  private [validatingSymbol]: boolean = false;
  private [validationRequestSymbol]: Promise<void> | undefined = undefined;
  private [onChangeSymbol]: (oldValue?: T) => void;
  private [onSubmitSymbol]: (value: T) => Promise<T|void>;

  private [validationsSymbol]: Map<AbstractModel<any>, Map<Validator<any>, Promise<ReadonlyArray<ValueError<any>>>>> = new Map();

  constructor(
    public context: Element,
    Model: ModelConstructor<T, M>,
    config?: BinderConfiguration<T>
  ) {
    super(new Model({value: Model.createEmptyValue()}, 'value'));
    this[emptyValueSymbol] = (this.model[parentSymbol] as HasValue<T>).value;
    // @ts-ignore
    this.model[parentSymbol] = this;

    if (typeof (context as any).requestUpdate === 'function') {
      this[onChangeSymbol] = () => (context as any).requestUpdate();
    }
    this[onChangeSymbol] = config?.onChange || this[onChangeSymbol];
    this[onSubmitSymbol] = config?.onSubmit || this[onSubmitSymbol];
    this.reset(this[emptyValueSymbol]);
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
    this.updateValidation();
  }

  /**
   * Reset the form value to default value and clear validation errors
   *
   * @param defaultValue When present, sets the argument as the new default
   * value before resetting, otherwise the previous default is used.
   */
  reset(defaultValue: T = this[defaultValueSymbol]) {
    this.defaultValue = defaultValue;
    if (
      // Skip when no value is set yet (e. g., invoked from constructor)
      this.value
      // Clear validation state, then proceed if update is needed
      && this.clearValidation()
      // When value is dirty, another update is coming from invoking the value
      // setter below, so we skip this one to prevent duplicate updates
      && this.value === defaultValue) {
      this.update(this.value);
    }

    this.value = this.defaultValue;
  }

  clear() {
    this.reset(this[emptyValueSymbol]);
  }

  async submit(): Promise<T|void>{
    if(this[onSubmitSymbol]!==undefined){
      this.submitTo(this[onSubmitSymbol]);
    }
  }

  async submitTo(endpointMethod: (value: T) => Promise<T|void>): Promise<T|void> {
    const errors = await this.validate();
    if (errors.length) {
      throw new ValidationError(errors);
    }

    this[submittingSymbol] = true;
    this.update(this.value);
    try {
      return await endpointMethod.call(this.context, this.value);
    } catch (error) {
      if (error.validationErrorData && error.validationErrorData.length) {
        const valueErrors: Array<ValueError<any>> = [];
        error.validationErrorData.forEach((data:any) => {
          const res = /Object of type '(.+)' has invalid property '(.+)' with value '(.+)', validation error: '(.+)'/.exec(data.message);
          const [property, value, message] = res ? res.splice(2) : [data.parameterName, undefined, data.message];
          valueErrors.push({ property, value, validator: new ServerValidator(message), message });
        });
        this.setErrorsWithDescendants(valueErrors);
        error = new ValidationError(valueErrors);
      }
      throw (error);
    } finally {
      this[submittingSymbol] = false;
      this.defaultValue = this.value;
      this.update(this.value);
    }
  }

  async requestValidation<NT, NM extends AbstractModel<NT>>(model: NM, validator: Validator<NT>): Promise<ReadonlyArray<ValueError<NT>>> {
    let modelValidations: Map<Validator<NT>, Promise<ReadonlyArray<ValueError<NT>>>>;
    if (this[validationsSymbol].has(model)) {
      modelValidations = this[validationsSymbol].get(model) as Map<Validator<NT>, Promise<ReadonlyArray<ValueError<NT>>>>;
    } else {
      modelValidations = new Map();
      this[validationsSymbol].set(model, modelValidations);
    }

    await this.performValidation();

    if (modelValidations.has(validator)) {
      return modelValidations.get(validator) as Promise<ReadonlyArray<ValueError<NT>>>;
    }

    const promise = runValidator(model, validator);
    modelValidations.set(validator, promise);
    const valueErrors = await promise;

    modelValidations.delete(validator);
    if (modelValidations.size === 0) {
      this[validationsSymbol].delete(model);
    }
    if (this[validationsSymbol].size === 0) {
      this.completeValidation();
    }

    return valueErrors;
  }

  getFieldStrategy(elm: any): FieldStrategy {
    return getDefaultFieldStrategy(elm);
  }

  get submitting() {
    return this[submittingSymbol];
  }

  get validating() {
    return this[validatingSymbol];
  }

  protected performValidation(): Promise<void> | void {
    if (!this[validationRequestSymbol]) {
      this[validatingSymbol] = true;
      this[validationRequestSymbol] = Promise.resolve().then(() => {
        this[validationRequestSymbol] = undefined;
      });
    }
    return this[validationRequestSymbol];
  }

  protected completeValidation() {
    this[validatingSymbol] = false;
  }

  protected update(oldValue: T) {
    if(this[onChangeSymbol]){
      this[onChangeSymbol].call(this.context, oldValue);
    }
  }
}

export interface BinderConfiguration<T>{
  onChange?: (oldValue?: T) => void,
  onSubmit?: (value: T) => Promise<T|void>
}
