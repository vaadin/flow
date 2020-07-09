import MyBazModel from './MyBazModel';
import MyEntityIdModel from './MyEntityIdModel';
import MyEntity from './MyEntity';

// @ts-ignore
import {ObjectModel,StringModel,NumberModel,ArrayModel,BooleanModel,Required,ModelType,getKeyModelSymbol} from '@vaadin/form';

// @ts-ignore
import {Email,Null,NotNull,NotEmpty,NotBlank,AssertTrue,AssertFalse,Negative,NegativeOrZero,Positive,PositiveOrZero,Size,Past,PastOrPresent,Future,FutureOrPresent,Digits,Min,Max,Pattern,DecimalMin,DecimalMax} from '@vaadin/form';

/**
 * This module is generated from com.vaadin.flow.server.connect.generator.tsmodel.TsFormEndpoint.MyEntity.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 */
export default class MyEntityModel<T extends MyEntity = MyEntity> extends MyEntityIdModel<T> {
  static createEmptyValue: () => MyEntity;

  get assertFalse(): StringModel {
    return this[getKeyModelSymbol](this, 'assertFalse', StringModel, [new AssertFalse()]);
  }

  get assertTrue(): StringModel {
    return this[getKeyModelSymbol](this, 'assertTrue', StringModel, [new AssertTrue()]);
  }

  get bar(): MyBazModel {
    return this[getKeyModelSymbol](this, 'bar', MyBazModel, []);
  }

  get baz(): ArrayModel<ModelType<MyBazModel>, MyBazModel> {
    return this[getKeyModelSymbol](this, 'baz', ArrayModel, [MyBazModel, []]);
  }

  get bool(): BooleanModel {
    return this[getKeyModelSymbol](this, 'bool', BooleanModel, []);
  }

  get children(): ArrayModel<ModelType<MyEntityModel>, MyEntityModel> {
    return this[getKeyModelSymbol](this, 'children', ArrayModel, [MyEntityModel, []]);
  }

  get decimalMax(): NumberModel {
    return this[getKeyModelSymbol](this, 'decimalMax', NumberModel, [new DecimalMax({value:"0.01", inclusive:false})]);
  }

  get decimalMin(): NumberModel {
    return this[getKeyModelSymbol](this, 'decimalMin', NumberModel, [new DecimalMin("0.01")]);
  }

  get digits(): StringModel {
    return this[getKeyModelSymbol](this, 'digits', StringModel, [new Digits({integer:5, fraction:2})]);
  }

  get email(): StringModel {
    return this[getKeyModelSymbol](this, 'email', StringModel, [new Email({message:"foo"})]);
  }

  get entityMap(): ObjectModel<{ [key: string]: ModelType<MyBazModel>; }> {
    return this[getKeyModelSymbol](this, 'entityMap', ObjectModel, []);
  }

  get entityMatrix(): ArrayModel<ModelType<ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>, ArrayModel<ModelType<MyEntityModel>, MyEntityModel>> {
    return this[getKeyModelSymbol](this, 'entityMatrix', ArrayModel, [ArrayModel, [MyEntityModel, []]]);
  }

  get foo(): StringModel {
    return this[getKeyModelSymbol](this, 'foo', StringModel, []);
  }

  get future(): StringModel {
    return this[getKeyModelSymbol](this, 'future', StringModel, [new Future()]);
  }

  get futureOrPresent(): ObjectModel {
    return this[getKeyModelSymbol](this, 'futureOrPresent', ObjectModel, [new FutureOrPresent()]);
  }

  get isNull(): StringModel {
    return this[getKeyModelSymbol](this, 'isNull', StringModel, [new Null()]);
  }

  get list(): ArrayModel<string, StringModel> {
    return this[getKeyModelSymbol](this, 'list', ArrayModel, [StringModel, [], new NotEmpty()]);
  }

  get max(): NumberModel {
    return this[getKeyModelSymbol](this, 'max', NumberModel, [new Max(2)]);
  }

  get min(): NumberModel {
    return this[getKeyModelSymbol](this, 'min', NumberModel, [new Min({value:1, message:"foo"})]);
  }

  get negative(): NumberModel {
    return this[getKeyModelSymbol](this, 'negative', NumberModel, [new Negative()]);
  }

  get negativeOrCero(): NumberModel {
    return this[getKeyModelSymbol](this, 'negativeOrCero', NumberModel, [new NegativeOrZero()]);
  }

  get notBlank(): StringModel {
    return this[getKeyModelSymbol](this, 'notBlank', StringModel, [new NotBlank()]);
  }

  get notEmpty(): StringModel {
    return this[getKeyModelSymbol](this, 'notEmpty', StringModel, [new NotEmpty(), new NotNull()]);
  }

  get notNull(): StringModel {
    return this[getKeyModelSymbol](this, 'notNull', StringModel, [new NotNull()]);
  }

  get numberMatrix(): ArrayModel<ModelType<ArrayModel<number, NumberModel>>, ArrayModel<number, NumberModel>> {
    return this[getKeyModelSymbol](this, 'numberMatrix', ArrayModel, [ArrayModel, [NumberModel, []]]);
  }

  get past(): StringModel {
    return this[getKeyModelSymbol](this, 'past', StringModel, [new Past()]);
  }

  get pastOrPresent(): ObjectModel {
    return this[getKeyModelSymbol](this, 'pastOrPresent', ObjectModel, [new PastOrPresent()]);
  }

  get pattern(): StringModel {
    return this[getKeyModelSymbol](this, 'pattern', StringModel, [new Pattern({regexp:"\\d+\\..+"})]);
  }

  get positive(): NumberModel {
    return this[getKeyModelSymbol](this, 'positive', NumberModel, [new Positive()]);
  }

  get positiveOrCero(): NumberModel {
    return this[getKeyModelSymbol](this, 'positiveOrCero', NumberModel, [new PositiveOrZero()]);
  }

  get size(): StringModel {
    return this[getKeyModelSymbol](this, 'size', StringModel, [new Size()]);
  }

  get size1(): StringModel {
    return this[getKeyModelSymbol](this, 'size1', StringModel, [new Size({min:1})]);
  }

  get stringArray(): ArrayModel<string, StringModel> {
    return this[getKeyModelSymbol](this, 'stringArray', ArrayModel, [StringModel, []]);
  }

  get stringMap(): ObjectModel<{ [key: string]: string; }> {
    return this[getKeyModelSymbol](this, 'stringMap', ObjectModel, []);
  }
}