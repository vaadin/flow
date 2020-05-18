import { AbstractModel, defaultValueSymbol, ModelConstructor} from "./Models";
import { ServerValidator, validate, ValidationError, ValueError } from "./Validation";

const isSubmittingSymbol = Symbol('isSubmitting');
const valueSymbol = Symbol('value');
const onChangeSymbol = Symbol('onChange');
const onSubmitSymbol = Symbol('onSubmit');

export class Binder<T, M extends AbstractModel<T>> {
  model: M;
  private [defaultValueSymbol]: T;
  private [valueSymbol]: T;
  private [isSubmittingSymbol]: boolean = false;
  private [onChangeSymbol]: (oldValue?: T) => void;
  private [onSubmitSymbol]: (value: T) => Promise<T|void>;

  constructor(
    public context: Updatable<T>,
    Model: ModelConstructor<T, M>,
    config?: BinderConfiguration<T>
  ) {
    this[onChangeSymbol] = () => context.requestUpdate();
    if (config !== undefined) {
      if(config.onChange !== undefined){
        this[onChangeSymbol] = config.onChange;  
      }
      if(config.onSubmit !== undefined){
        this[onSubmitSymbol] = config.onSubmit;  
      }
    }
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

  async submit(): Promise<T|void>{
    if(this[onSubmitSymbol]!==undefined){
      this.submitTo(this[onSubmitSymbol]);
    }
  }

  async submitTo(endpointMethod: (value: T) => Promise<T|void>): Promise<T|void> {
    const errors = await validate(this.model);
    if (errors.length) {
      throw new ValidationError(errors);
    }

    this[isSubmittingSymbol] = true;
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
      this[isSubmittingSymbol] = false;
      this.reset(this.value);
    }
  }

  private update(oldValue: T) {
    this[onChangeSymbol].call(this.context, oldValue);
  }

  get isSubmitting() {
    return this[isSubmittingSymbol];
  }
}

export interface BinderConfiguration<T>{
  onChange?: (oldValue?: T) => void,
  onSubmit?: (value: T) => Promise<T|void>
}

export interface Updatable<T>{
  requestUpdate(name?: PropertyKey, oldValue?: T): any;
}
