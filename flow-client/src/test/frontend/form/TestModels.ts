/* tslint:disable:max-classes-per-file */
// API to test
import {
  ArrayModel,
  BooleanModel,
  getPropertyModel,
  ModelConstructor,
  NumberModel,
  ObjectModel,
  Pattern,
  Positive,
  Required,
  Size,
  StringModel,
} from "../../../main/resources/META-INF/resources/frontend/form";

export interface IdEntity {
  idString: string;
}
export class IdEntityModel<T extends IdEntity = IdEntity> extends ObjectModel<T> {
  static createEmptyValue: () => IdEntity;
  get idString(): StringModel {
    return this[getPropertyModel]('idString', StringModel, [false]);
  }
}

export interface Product extends IdEntity {
  description: string;
  price: number;
  isInStock: boolean;
}
export class ProductModel<T extends Product = Product> extends IdEntityModel<T> {
  static createEmptyValue: () => Product;

  get description() {
    return this[getPropertyModel]('description', StringModel, [false, new Required()]);
  }

  get price() {
    return this[getPropertyModel]('price', NumberModel, [false, new Positive()]);
  }

  get isInStock() {
    return this[getPropertyModel]('isInStock', BooleanModel, [false]);
  }
}

interface Customer extends IdEntity {
  fullName: string;
  nickName: string;
}
export class CustomerModel<T extends Customer = Customer> extends IdEntityModel<T> {
  static createEmptyValue: () => Customer;

  get fullName() {
    return this[getPropertyModel]('fullName', StringModel, [false, new Size({min: 4}), new Required()]) as StringModel;
  }

  get nickName() {
    return this[getPropertyModel]('nickName', StringModel, [false, new Pattern("....*")]) as StringModel;
  }
}

export interface Order extends IdEntity {
  customer: Customer;
  notes: string;
  priority: number;
  products: ReadonlyArray<Product>;
}
export class OrderModel<T extends Order = Order> extends IdEntityModel<T> {
  static createEmptyValue: () => Order;

  get customer(): CustomerModel {
    return this[getPropertyModel]('customer', CustomerModel, [false, new Required()]);
  }

  get notes(): StringModel {
    return this[getPropertyModel]('notes', StringModel, [false, new Required()]);
  }

  get priority(): NumberModel {
    return this[getPropertyModel]('priority', NumberModel, [false]);
  }

  get products(): ArrayModel<Product, ProductModel> {
    return this[getPropertyModel]('products', ArrayModel as ModelConstructor<ReadonlyArray<Product>, ArrayModel<Product, ProductModel>>, [false, ProductModel, [false]]);
  }
}

export interface TestEntity {
  fieldString: string;
  fieldNumber: number;
  fieldBoolean: boolean;
  fieldObject: object;
  fieldArrayString: string[];
  fieldArrayModel: IdEntity[];
  fieldMatrixNumber: number[][];
}
export class TestModel<T extends TestEntity = TestEntity> extends ObjectModel<T> {
  static createEmptyValue: () => TestEntity;

  get fieldString() {
    return this[getPropertyModel]('fieldString', StringModel, [false]) as StringModel;
  }

  get fieldNumber() {
    return this[getPropertyModel]('fieldNumber', NumberModel, [false]) as NumberModel;
  }

  get fieldBoolean() {
    return this[getPropertyModel]('fieldBoolean', BooleanModel, [false]) as BooleanModel;
  }

  get fieldObject() {
    return this[getPropertyModel]('fieldObject', ObjectModel, [false]) as ObjectModel<object>;
  }

  get fieldArrayString() {
    return this[getPropertyModel]('fieldArrayString', ArrayModel, [false, StringModel, [false]]) as ArrayModel<string, StringModel>;
  }

  get fieldArrayModel() {
    return this[getPropertyModel]('fieldArrayModel', ArrayModel, [false, IdEntityModel, [false]]) as ArrayModel<IdEntity, IdEntityModel>;
  }

  get fieldMatrixNumber() {
    return this[getPropertyModel]('fieldMatrixNumber', ArrayModel, [false, ArrayModel, [false, NumberModel, [false, new Positive()]]]) as ArrayModel<ReadonlyArray<number>, ArrayModel<number, NumberModel>>;
  }
}
