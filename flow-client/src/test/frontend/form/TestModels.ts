/* tslint:disable:max-classes-per-file */
// API to test
import {
  ArrayModel,
  BooleanModel,
  getPropertyModelSymbol,
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
    return this[getPropertyModelSymbol]('idString', StringModel, [false]);
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
    return this[getPropertyModelSymbol]('description', StringModel, [false, new Required()]);
  }

  get price() {
    return this[getPropertyModelSymbol]('price', NumberModel, [false, new Positive()]);
  }

  get isInStock() {
    return this[getPropertyModelSymbol]('isInStock', BooleanModel, [false]);
  }
}

interface Customer extends IdEntity {
  fullName: string;
  nickName: string;
}
export class CustomerModel<T extends Customer = Customer> extends IdEntityModel<T> {
  static createEmptyValue: () => Customer;

  get fullName() {
    return this[getPropertyModelSymbol]('fullName', StringModel, [false, new Size({min: 4}), new Required()]) as StringModel;
  }

  get nickName() {
    return this[getPropertyModelSymbol]('nickName', StringModel, [false, new Pattern("....*")]) as StringModel;
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
    return this[getPropertyModelSymbol]('customer', CustomerModel, [false, new Required()]);
  }

  get notes(): StringModel {
    return this[getPropertyModelSymbol]('notes', StringModel, [false, new Required()]);
  }

  get priority(): NumberModel {
    return this[getPropertyModelSymbol]('priority', NumberModel, [false]);
  }

  get products(): ArrayModel<Product, ProductModel> {
    return this[getPropertyModelSymbol]('products', ArrayModel as ModelConstructor<ReadonlyArray<Product>, ArrayModel<Product, ProductModel>>, [false, ProductModel, [false]]);
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
  fieldAny: any;
}
export class TestModel<T extends TestEntity = TestEntity> extends ObjectModel<T> {
  static createEmptyValue: () => TestEntity;

  get fieldString() {
    return this[getPropertyModelSymbol]('fieldString', StringModel, [false]) as StringModel;
  }

  get fieldNumber() {
    return this[getPropertyModelSymbol]('fieldNumber', NumberModel, [false]) as NumberModel;
  }

  get fieldBoolean() {
    return this[getPropertyModelSymbol]('fieldBoolean', BooleanModel, [false]) as BooleanModel;
  }

  get fieldObject() {
    return this[getPropertyModelSymbol]('fieldObject', ObjectModel, [false]) as ObjectModel<object>;
  }

  get fieldArrayString() {
    return this[getPropertyModelSymbol]('fieldArrayString', ArrayModel, [false, StringModel, [false]]) as ArrayModel<string, StringModel>;
  }

  get fieldArrayModel() {
    return this[getPropertyModelSymbol]('fieldArrayModel', ArrayModel, [false, IdEntityModel, [false]]) as ArrayModel<IdEntity, IdEntityModel>;
  }

  get fieldMatrixNumber() {
    return this[getPropertyModelSymbol]('fieldMatrixNumber', ArrayModel, [false, ArrayModel, [false, NumberModel, [false, new Positive()]]]) as ArrayModel<ReadonlyArray<number>, ArrayModel<number, NumberModel>>;
  }

  get fieldAny() {
    return this[getPropertyModelSymbol]('fieldAny', ObjectModel, [false]) as ObjectModel<any>;
  }
}

export interface Employee extends IdEntity {
  fullName: string;
  supervisor?: Employee;
}
export class EmployeeModel<T extends Employee = Employee> extends IdEntityModel<T> {
  static createEmptyValue: () => Employee;

  get fullName() {
    return this[getPropertyModelSymbol]('fullName', StringModel, [false]) as StringModel;
  }

  get supervisor(): EmployeeModel {
    return this[getPropertyModelSymbol]('supervisor', EmployeeModel, [true]);
  }
}
