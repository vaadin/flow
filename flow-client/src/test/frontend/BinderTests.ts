const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");

/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');

// API to test
import {
  ArrayModel,
  Binder,
  NumberModel,
  ObjectModel,
  StringModel,
  getName,
  getValue,
  setValue, BooleanModel,
} from "../../main/resources/META-INF/resources/frontend/Binder";

import {LitElement} from 'lit-element';

interface IdEntity {
  idString: string;
}
class IdEntityModel<T extends IdEntity = IdEntity> extends ObjectModel<T> {
  static createEmptyValue: () => IdEntity;
  readonly idString = new StringModel(this, 'idString');
}

interface Product extends IdEntity {
  description: string;
  price: number;
  isInStock: boolean;
}
class ProductModel<T extends Product = Product> extends IdEntityModel<T> {
  static createEmptyValue: () => Product;
  readonly description = new StringModel(this, 'description');
  readonly price = new NumberModel(this, 'price');
  readonly isInStock = new BooleanModel(this, 'isInStock');
}

interface Customer extends IdEntity {
  fullName: string;
}
class CustomerModel<T extends Customer = Customer> extends IdEntityModel<T> {
  static createEmptyValue: () => Customer;
  readonly fullName = new StringModel(this, 'fullName');
}

interface Order extends IdEntity {
  customer: Customer;
  notes: string;
  products: ReadonlyArray<Product>;
}
class OrderModel<T extends Order = Order> extends IdEntityModel<T> {
  static createEmptyValue: () => Order;
  readonly customer = new CustomerModel(this, 'customer');
  readonly notes = new StringModel(this, 'notes');
  readonly products = new ArrayModel(this, 'products', ProductModel);
}

class OrderView extends LitElement {}
customElements.define('order-view', OrderView);

suite("Binder", () => {
  const orderView = document.createElement('order-view') as OrderView;
  const requestUpdateStub = sinon.stub(orderView, 'requestUpdate').resolves();

  afterEach(() => {
    requestUpdateStub.reset();
  });

  test("should instantiate without type arguments", () => {
    const binder = new Binder(orderView, OrderModel, () => orderView.requestUpdate());

    assert.isDefined(binder);
    assert.isDefined(binder.value.notes);
    assert.isDefined(binder.value.idString);
    assert.isDefined(binder.value.customer.fullName);
    assert.isDefined(binder.value.customer.idString);
  });

  test("should instantiate model", () => {
    const binder = new Binder(orderView, OrderModel, () => orderView.requestUpdate());

    assert.instanceOf(binder.model, OrderModel);
  });

  suite("name value", () => {
    let binder: Binder<Order, OrderModel<Order>>;

    const expectedEmptyOrder: Order = {
      idString: '',
      customer: {
        idString: '',
        fullName: '',
      },
      notes: '',
      products: []
    };

    beforeEach(() => {
      binder = new Binder(
        orderView,
        OrderModel,
        () => orderView.requestUpdate()
      );
      requestUpdateStub.reset();
    });

    test("should have name for models", () => {
      assert.equal(getName(binder.model.notes), "notes");
      assert.equal(getName(binder.model.customer.fullName), "customer[fullName]");
    });

    test("should have initial defaultValue", () => {
      assert.deepEqual(binder.defaultValue, expectedEmptyOrder);
    });

    test("should have initial value", () => {
      assert.equal(binder.value, binder.defaultValue);
      assert.equal(getValue(binder.model), binder.value);
      assert.equal(getValue(binder.model.notes), "");
      assert.equal(getValue(binder.model.customer.fullName), "");
    });

    test("should change value on setValue", () => {
      // Sanity check: requestUpdate should not be called
      sinon.assert.notCalled(requestUpdateStub);

      setValue(binder.model.notes, "foo");
      assert.equal(binder.value.notes, "foo");
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should change value on deep setValue", () => {
      sinon.assert.notCalled(requestUpdateStub);

      setValue(binder.model.customer.fullName, "foo");
      assert.equal(binder.value.customer.fullName, "foo");
      sinon.assert.calledOnce(orderView.requestUpdate);
    });

    test("should not change defaultValue on setValue", () => {
      setValue(binder.model.notes, "foo");
      setValue(binder.model.customer.fullName, "foo");

      assert.equal(binder.defaultValue.notes, "");
      assert.equal(binder.defaultValue.customer.fullName, "");
    });

    test("should reset to default value", () => {
      setValue(binder.model.notes, "foo");
      setValue(binder.model.customer.fullName, "foo");
      requestUpdateStub.reset();

      binder.reset();

      assert.equal(binder.value.notes, "");
      assert.equal(binder.value.customer.fullName, "");
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should reset to provided value", () => {
      setValue(binder.model.notes, "foo");
      setValue(binder.model.customer.fullName, "foo");
      requestUpdateStub.reset();

      binder.reset({
        ...expectedEmptyOrder,
        notes: "bar",
        customer: {
          ...expectedEmptyOrder.customer,
          fullName: "bar"
        }
      });

      assert.equal(binder.value.notes, "bar");
      assert.equal(binder.value.customer.fullName, "bar");
      sinon.assert.calledOnce(requestUpdateStub);
    });
  });
});

