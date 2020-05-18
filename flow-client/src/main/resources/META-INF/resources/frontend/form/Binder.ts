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

  private [validationsSymbol]: Map<AbstractModel<any>, Map<Validator<any>, Promise<ValueError<any> | void>>> = new Map();

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

  reset(defaultValue?: T) {
    if (defaultValue !== undefined) {
      this.defaultValue = defaultValue;
    }
    this.value = this.defaultValue;
  }

  clear() {
    this.value = this[emptyValueSymbol];
  }

  async submit(): Promise<T|void>{
    if(this[onSubmitSymbol]!==undefined){
      this.submitTo(this[onSubmitSymbol]);
    }
  }

  async submitTo(endpointMethod: (value: T) => Promise<T|void>): Promise<T|void> {
    const errors =await this.validate();
    if (errors.length) {
      throw new ValidationError(errors);
    }

    this[submittingSymbol] = true;
    this.update(this.value);
    try {
      return await endpointMethod.call(this.context, this.value);
    } catch (error) {
      if (error.validationErrorData && error.validationErrorData.length) {
        const valueErrors:Array<ValueError<any>> = [];
        error.validationErrorData.forEach((data:any) => {
          const res = /Object of type '(.+)' has invalid property '(.+)' with value '(.+)', validation error: '(.+)'/.exec(data.message);
          const [property, value, message] = res ? res.splice(2) : [data.parameterName, undefined, data.message];
          valueErrors.push({ property, value, validator: new ServerValidator(message) });
        });
        error = new ValidationError(valueErrors);
      }
      throw (error);
    } finally {
      this[submittingSymbol] = false;
      this.reset(this.value);
    }
  }

  async requestValidation<NT, NM extends AbstractModel<NT>>(model: NM, validator: Validator<NT>): Promise<ValueError<NT> | void> {
    let modelValidations: Map<Validator<NT>, Promise<ValueError<NT> | void>>;
    if (this[validationsSymbol].has(model)) {
      modelValidations = this[validationsSymbol].get(model) as Map<Validator<NT>, Promise<ValueError<NT> | void>>;
    } else {
      modelValidations = new Map();
      this[validationsSymbol].set(model, modelValidations);
    }

    await this.performValidation();

    if (modelValidations.has(validator)) {
      return modelValidations.get(validator);
    }

    const promise = Promise.resolve(runValidator(model, validator));
    modelValidations.set(validator, promise);
    const valueError = await promise;

    modelValidations.delete(validator);
    if (modelValidations.size === 0) {
      this[validationsSymbol].delete(model);
    }
    if (this[validationsSymbol].size === 0) {
      this.completeValidation();
    }

    return valueError;
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
