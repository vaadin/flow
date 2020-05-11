/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");

/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');

// API to test
import {
  ArrayModel,
  Binder,
  BooleanModel,
  field,
  getModelValidators,
  getName,
  getValue,
  NumberModel,
  ObjectModel,
  setValue,
  StringModel,
  validate,
  Validator
} from "../../main/resources/META-INF/resources/frontend/Binder";

import {customElement, html, LitElement, query} from 'lit-element';

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

@customElement('order-view')
class OrderView extends LitElement {}

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

  suite("validation", () => {
    let binder: Binder<Order, OrderModel<Order>>;

    beforeEach(() => {
      binder = new Binder(
        orderView,
        OrderModel,
        () => orderView.requestUpdate()
      );
    });

    test("should not have validation errors for a model without validators", () => {
      assert.isEmpty(validate(binder.model));
    });

    test("should fail validation after adding a synchronous validator", () => {
      class SyncValidator implements Validator<Order>{
        message = "foo";
        validate = () => false;
      }
      getModelValidators(binder.model).add(new SyncValidator());

      return validate(binder.model).then(errMsg => {
        assert.equal(errMsg, "foo");
      });
    });

    test("should fail validation after adding an synchronous validator", () => {
      class AsyncValidator implements Validator<Order>{
        message = "bar";
        validate = async () =>{
          await new Promise(resolve => setTimeout(resolve, 10));
          return false;
        };
      }
      getModelValidators(binder.model).add(new AsyncValidator());
      return validate(binder.model).then(errMsg => {
        assert.equal(errMsg, "bar");
      });
    });
  });

  suite('field', () => {
    @customElement('mock-text-field')
    class MockTextFieldElement extends HTMLElement {
      __value = '';

      valueSpy = sinon.spy(this, 'value', ['get', 'set']);

      setAttributeSpy = sinon.spy(this, 'setAttribute');

      get value() {
        return this.__value;
      }

      set value(value) {
        this.__value = value;
      }
    }

    @customElement('order-view-with-text-field')
    class OrderViewWithTextField extends LitElement {
      requestUpdateSpy = sinon.spy(this, 'requestUpdate');

      binder = new Binder(this, OrderModel,() => this.requestUpdate());

      @query('#notesField')
      notesField?: MockTextFieldElement;

      @query('#customerFullNameField')
      customerFullNameField?: MockTextFieldElement;

      render() {
        return html`
          <mock-text-field
           id="notesField"
           ...="${field(this.binder.model.notes)}"
           ></mock-text-field>

          <mock-text-field
           id="customerFullNameField"
           ...="${field(this.binder.model.customer.fullName)}"
           ></mock-text-field>
        `;
      }
    }

    let orderViewWithTextField: OrderViewWithTextField;

    beforeEach(async () => {
      orderViewWithTextField = document.createElement('order-view-with-text-field') as OrderViewWithTextField;
      document.body.appendChild(orderViewWithTextField);
      await orderViewWithTextField.updateComplete;
    });

    afterEach(async () => {
      document.body.removeChild(orderViewWithTextField);
    });

    test('should set name attribute', () => {
      sinon.assert.calledWith(orderViewWithTextField.notesField!.setAttributeSpy, 'name', 'notes');
      sinon.assert.calledWith(orderViewWithTextField.customerFullNameField!.setAttributeSpy, 'name', 'customer[fullName]');
    });

    test('should set value property on setValue', async() => {
      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;
      sinon.assert.calledWith(orderViewWithTextField.notesField!.valueSpy.set, 'foo');
    });

    test('should set given non-empty value on reset with argument', async() => {
      const emptyOrder = OrderModel.createEmptyValue();
      orderViewWithTextField.binder.reset({
        ...emptyOrder,
        notes: "foo",
        customer: {
          ...emptyOrder.customer,
          fullName: "bar"
        }
      });
      await orderViewWithTextField.updateComplete;

      sinon.assert.calledWith(orderViewWithTextField.notesField!.valueSpy.set, 'foo');
      sinon.assert.calledWith(orderViewWithTextField.customerFullNameField!.valueSpy.set, 'bar');
    });

    test('should set given empty value on reset with argument', async() => {
      const emptyOrder = OrderModel.createEmptyValue();
      orderViewWithTextField.binder.reset({
        ...emptyOrder,
        notes: "foo",
        customer: {
          ...emptyOrder.customer,
          fullName: "bar"
        }
      });
      await orderViewWithTextField.updateComplete;
      orderViewWithTextField.notesField!.valueSpy.set.resetHistory();

      orderViewWithTextField.binder.reset(OrderModel.createEmptyValue());
      await orderViewWithTextField.updateComplete;

      sinon.assert.calledWith(orderViewWithTextField.notesField!.valueSpy.set, '');
      sinon.assert.calledWith(orderViewWithTextField.customerFullNameField!.valueSpy.set, '');
    });

    test('should set default value on reset without argument', async() => {
      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;
      orderViewWithTextField.notesField!.valueSpy.set.resetHistory();

      orderViewWithTextField.binder.reset();
      await orderViewWithTextField.updateComplete;

      sinon.assert.calledWith(orderViewWithTextField.notesField!.valueSpy.set, '');
    });

    test('should update binder value on setValue', async() => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();

      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      sinon.assert.calledOnce(orderViewWithTextField.requestUpdateSpy);
    });

    test('should update binder value on input event', async() => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();
      orderViewWithTextField.notesField!.value = 'foo';
      orderViewWithTextField.notesField!.dispatchEvent(new CustomEvent(
        'input',
        {bubbles: true, composed: true, cancelable: false}
      ));
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      sinon.assert.calledOnce(orderViewWithTextField.requestUpdateSpy);
    });

    test('should update binder value on change event', async() => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();
      orderViewWithTextField.notesField!.value = 'foo';
      orderViewWithTextField.notesField!.dispatchEvent(new CustomEvent(
        'change',
        {bubbles: true, composed: true, cancelable: false}
      ));
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      sinon.assert.calledOnce(orderViewWithTextField.requestUpdateSpy);
    });

    test('should update binder value on blur event', async() => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();
      orderViewWithTextField.notesField!.value = 'foo';
      orderViewWithTextField.notesField!.dispatchEvent(new CustomEvent(
        'blur',
        {bubbles: true, composed: true, cancelable: false}
      ));
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      sinon.assert.calledOnce(orderViewWithTextField.requestUpdateSpy);
    });
  });
});



