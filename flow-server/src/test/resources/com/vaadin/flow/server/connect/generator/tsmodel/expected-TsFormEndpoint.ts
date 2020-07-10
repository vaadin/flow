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
    return this[getPropertyModel](this, 'assertFalse', StringModel, [new AssertFalse()]);
  }

  get assertTrue(): StringModel {
    return this[getPropertyModel](this, 'assertTrue', StringModel, [new AssertTrue()]);
  }

  get bar(): MyBazModel {
    return this[getPropertyModel](this, 'bar', MyBazModel, []);
  }

  get baz(): ArrayModel<ModelType<MyBazModel>, MyBazModel> {
    return this[getPropertyModel](this, 'baz', ArrayModel, [MyBazModel, []]);
  }

  get bool(): BooleanModel {
    return this[getPropertyModel](this, 'bool', BooleanModel, []);
  }

  get children(): ArrayModel<ModelType<MyEntityModel>, MyEntityModel> {
    return this[getPropertyModel](this, 'children', ArrayModel, [MyEntityModel, []]);
  }

  get decimalMax(): NumberModel {
    return this[getPropertyModel](this, 'decimalMax', NumberModel, [new DecimalMax({value:"0.01", inclusive:false})]);
  }

  get decimalMin(): NumberModel {
    return this[getPropertyModel](this, 'decimalMin', NumberModel, [new DecimalMin("0.01")]);
  }

  get digits(): StringModel {
    return this[getPropertyModel](this, 'digits', StringModel, [new Digits({integer:5, fraction:2})]);
  }

  get email(): StringModel {
    return this[getPropertyModel](this, 'email', StringModel, [new Email({message:"foo"})]);
  }

  get entityMap(): ObjectModel<{ [key: string]: ModelType<MyBazModel>; }> {
    return this[getPropertyModel](this, 'entityMap', ObjectModel, []);
  }

  get entityMatrix(): ArrayModel<ModelType<ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>, ArrayModel<ModelType<MyEntityModel>, MyEntityModel>> {
    return this[getPropertyModel](this, 'entityMatrix', ArrayModel, [ArrayModel, [MyEntityModel, []]]);
  }

  get foo(): StringModel {
    return this[getPropertyModel](this, 'foo', StringModel, []);
  }

  get future(): StringModel {
    return this[getPropertyModel](this, 'future', StringModel, [new Future()]);
  }

  get futureOrPresent(): ObjectModel {
    return this[getPropertyModel](this, 'futureOrPresent', ObjectModel, [new FutureOrPresent()]);
  }

  get isNull(): StringModel {
    return this[getPropertyModel](this, 'isNull', StringModel, [new Null()]);
  }

  get list(): ArrayModel<string, StringModel> {
    return this[getPropertyModel](this, 'list', ArrayModel, [StringModel, [], new NotEmpty()]);
  }

  get max(): NumberModel {
    return this[getPropertyModel](this, 'max', NumberModel, [new Max(2)]);
  }

  get min(): NumberModel {
    return this[getPropertyModel](this, 'min', NumberModel, [new Min({value:1, message:"foo"})]);
  }

  get negative(): NumberModel {
    return this[getPropertyModel](this, 'negative', NumberModel, [new Negative()]);
  }

  get negativeOrCero(): NumberModel {
    return this[getPropertyModel](this, 'negativeOrCero', NumberModel, [new NegativeOrZero()]);
  }

  get notBlank(): StringModel {
    return this[getPropertyModel](this, 'notBlank', StringModel, [new NotBlank()]);
  }

  get notEmpty(): StringModel {
    return this[getPropertyModel](this, 'notEmpty', StringModel, [new NotEmpty(), new NotNull()]);
  }

  get notNull(): StringModel {
    return this[getPropertyModel](this, 'notNull', StringModel, [new NotNull()]);
  }

  get numberMatrix(): ArrayModel<ModelType<ArrayModel<number, NumberModel>>, ArrayModel<number, NumberModel>> {
    return this[getPropertyModel](this, 'numberMatrix', ArrayModel, [ArrayModel, [NumberModel, []]]);
  }

  get past(): StringModel {
    return this[getPropertyModel](this, 'past', StringModel, [new Past()]);
  }

  get pastOrPresent(): ObjectModel {
    return this[getPropertyModel](this, 'pastOrPresent', ObjectModel, [new PastOrPresent()]);
  }

  get pattern(): StringModel {
    return this[getPropertyModel](this, 'pattern', StringModel, [new Pattern({regexp:"\\d+\\..+"})]);
  }

  get positive(): NumberModel {
    return this[getPropertyModel](this, 'positive', NumberModel, [new Positive()]);
  }

  get positiveOrCero(): NumberModel {
    return this[getPropertyModel](this, 'positiveOrCero', NumberModel, [new PositiveOrZero()]);
  }

  get size(): StringModel {
    return this[getPropertyModel](this, 'size', StringModel, [new Size()]);
  }

  get size1(): StringModel {
    return this[getPropertyModel](this, 'size1', StringModel, [new Size({min:1})]);
  }

  get stringArray(): ArrayModel<string, StringModel> {
    return this[getPropertyModel](this, 'stringArray', ArrayModel, [StringModel, []]);
  }

  get stringMap(): ObjectModel<{ [key: string]: string; }> {
    return this[getPropertyModel](this, 'stringMap', ObjectModel, []);
  }
}