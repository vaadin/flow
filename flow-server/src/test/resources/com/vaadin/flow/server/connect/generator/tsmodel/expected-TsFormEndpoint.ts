import MyBazModel from './MyBazModel';
import MyEntityIdModel from './MyEntityIdModel';
import MyEntity from './MyEntity';

// @ts-ignore
import {ObjectModel,StringModel,NumberModel,ArrayModel,BooleanModel,Required,ModelType,getPropertyModel} from '@vaadin/form';

// @ts-ignore
import {Email,Null,NotNull,NotEmpty,NotBlank,AssertTrue,AssertFalse,Negative,NegativeOrZero,Positive,PositiveOrZero,Size,Past,PastOrPresent,Future,FutureOrPresent,Digits,Min,Max,Pattern,DecimalMin,DecimalMax} from '@vaadin/form';

/**
 * This module is generated from com.vaadin.flow.server.connect.generator.tsmodel.TsFormEndpoint.MyEntity.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 */
export default class MyEntityModel<T extends MyEntity = MyEntity> extends MyEntityIdModel<T> {
  static createEmptyValue: () => MyEntity;

  get assertFalse(): StringModel {
    return this[getPropertyModel](this, 'assertFalse', StringModel, [false, new AssertFalse()]);
  }

  get assertTrue(): StringModel {
    return this[getPropertyModel](this, 'assertTrue', StringModel, [false, new AssertTrue()]);
  }

  get bar(): MyBazModel {
    return this[getPropertyModel](this, 'bar', MyBazModel, [false]);
  }

  get baz(): ArrayModel<ModelType<MyBazModel>, MyBazModel> {
    return this[getPropertyModel](this, 'baz', ArrayModel, [false, MyBazModel, [false]]);
  }

  get bool(): BooleanModel {
    return this[getPropertyModel](this, 'bool', BooleanModel, [false]);
  }

  get children(): ArrayModel<ModelType<MyEntityModel>, MyEntityModel> {
    return this[getPropertyModel](this, 'children', ArrayModel, [false, MyEntityModel, [false]]);
  }

  get decimalMax(): NumberModel {
    return this[getPropertyModel](this, 'decimalMax', NumberModel, [false, new DecimalMax({value:"0.01", inclusive:false})]);
  }

  get decimalMin(): NumberModel {
    return this[getPropertyModel](this, 'decimalMin', NumberModel, [false, new DecimalMin("0.01")]);
  }

  get digits(): StringModel {
    return this[getPropertyModel](this, 'digits', StringModel, [false, new Digits({integer:5, fraction:2})]);
  }

  get email(): StringModel {
    return this[getPropertyModel](this, 'email', StringModel, [false, new Email({message:"foo"})]);
  }

  get entityMap(): ObjectModel<{ [key: string]: ModelType<MyBazModel>; }> {
    return this[getPropertyModel](this, 'entityMap', ObjectModel, [false]);
  }

  get entityMatrix(): ArrayModel<ModelType<ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>, ArrayModel<ModelType<MyEntityModel>, MyEntityModel>> {
    return this[getPropertyModel](this, 'entityMatrix', ArrayModel, [false, ArrayModel, [false, MyEntityModel, [false]]]);
  }

  get foo(): StringModel {
    return this[getPropertyModel](this, 'foo', StringModel, [false]);
  }

  get future(): StringModel {
    return this[getPropertyModel](this, 'future', StringModel, [false, new Future()]);
  }

  get futureOrPresent(): ObjectModel {
    return this[getPropertyModel](this, 'futureOrPresent', ObjectModel, [false, new FutureOrPresent()]);
  }

  get isNull(): StringModel {
    return this[getPropertyModel](this, 'isNull', StringModel, [false, new Null()]);
  }

  get list(): ArrayModel<string, StringModel> {
    return this[getPropertyModel](this, 'list', ArrayModel, [false, StringModel, [false], new NotEmpty()]);
  }

  get max(): NumberModel {
    return this[getPropertyModel](this, 'max', NumberModel, [false, new Max(2)]);
  }

  get min(): NumberModel {
    return this[getPropertyModel](this, 'min', NumberModel, [false, new Min({value:1, message:"foo"})]);
  }

  get negative(): NumberModel {
    return this[getPropertyModel](this, 'negative', NumberModel, [false, new Negative()]);
  }

  get negativeOrCero(): NumberModel {
    return this[getPropertyModel](this, 'negativeOrCero', NumberModel, [false, new NegativeOrZero()]);
  }

  get notBlank(): StringModel {
    return this[getPropertyModel](this, 'notBlank', StringModel, [false, new NotBlank()]);
  }

  get notEmpty(): StringModel {
    return this[getPropertyModel](this, 'notEmpty', StringModel, [false, new NotEmpty(), new NotNull()]);
  }

  get notNull(): StringModel {
    return this[getPropertyModel](this, 'notNull', StringModel, [false, new NotNull()]);
  }

  get numberMatrix(): ArrayModel<ModelType<ArrayModel<number, NumberModel>>, ArrayModel<number, NumberModel>> {
    return this[getPropertyModel](this, 'numberMatrix', ArrayModel, [false, ArrayModel, [false, NumberModel, [false]]]);
  }

  get optionalEntity(): MyEntityModel {
    return this[getPropertyModel](this, 'optionalEntity', MyEntityModel, [true]);
  }

  get optionalList(): ArrayModel<string, StringModel> {
    return this[getPropertyModel](this, 'optionalList', ArrayModel, [true, StringModel, [true]]);
  }

  get optionalMatrix(): ArrayModel<ModelType<ArrayModel<string, StringModel>>, ArrayModel<string, StringModel>> {
    return this[getPropertyModel](this, 'optionalMatrix', ArrayModel, [true, ArrayModel, [false, StringModel, [true]]]);
  }

  get optionalString(): StringModel {
    return this[getPropertyModel](this, 'optionalString', StringModel, [true]);
  }

  get past(): StringModel {
    return this[getPropertyModel](this, 'past', StringModel, [false, new Past()]);
  }

  get pastOrPresent(): ObjectModel {
    return this[getPropertyModel](this, 'pastOrPresent', ObjectModel, [false, new PastOrPresent()]);
  }

  get pattern(): StringModel {
    return this[getPropertyModel](this, 'pattern', StringModel, [false, new Pattern({regexp:"\\d+\\..+"})]);
  }

  get positive(): NumberModel {
    return this[getPropertyModel](this, 'positive', NumberModel, [false, new Positive()]);
  }

  get positiveOrCero(): NumberModel {
    return this[getPropertyModel](this, 'positiveOrCero', NumberModel, [false, new PositiveOrZero()]);
  }

  get size(): StringModel {
    return this[getPropertyModel](this, 'size', StringModel, [false, new Size()]);
  }

  get size1(): StringModel {
    return this[getPropertyModel](this, 'size1', StringModel, [false, new Size({min:1})]);
  }

  get stringArray(): ArrayModel<string, StringModel> {
    return this[getPropertyModel](this, 'stringArray', ArrayModel, [false, StringModel, [false]]);
  }

  get stringMap(): ObjectModel<{ [key: string]: string; }> {
    return this[getPropertyModel](this, 'stringMap', ObjectModel, [false]);
  }
}