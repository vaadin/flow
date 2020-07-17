/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');

// API to test
import {
  Binder,
  BinderConfiguration,
} from "../../../main/resources/META-INF/resources/frontend/form";

import {Employee, EmployeeModel, Order, OrderModel, TestEntity, TestModel} from "./TestModels";

import {customElement, LitElement} from 'lit-element';

@customElement('lit-order-view')
class LitOrderView extends LitElement {}

@customElement('lit-employee-view')
class LitEmployeeView extends LitElement {}

suite("form/Binder", () => {
  const litOrderView = document.createElement('lit-order-view') as LitOrderView;
  const requestUpdateStub = sinon.stub(litOrderView, 'requestUpdate').resolves();

  afterEach(() => {
    requestUpdateStub.reset();
  });

  test("should instantiate without type arguments", () => {
    const binder = new Binder(litOrderView, OrderModel);

    assert.isDefined(binder);
    assert.isDefined(binder.value.notes);
    assert.isDefined(binder.value.idString);
    assert.isDefined(binder.value.customer.fullName);
    assert.isDefined(binder.value.customer.idString);
  });

  test("should instantiate model", () => {
    const binder = new Binder(litOrderView, OrderModel);

    assert.instanceOf(binder.model, OrderModel);
  });

  test("should be able to create a binder with a default onchange listener", () => {
    const binder = new Binder(litOrderView, OrderModel);

    binder.for(binder.model.notes).value = "foo";

    sinon.assert.calledTwice(requestUpdateStub);
  });

  test("should be able to create a binder with a custom onchange listener", () => {
    let foo = 'bar';
    const config: BinderConfiguration<Order> = {
      onChange: () => { foo = 'baz' }
    }

    const binder = new Binder(litOrderView, OrderModel, config);

    binder.for(binder.model.notes).value = "foo";

    assert.equal(foo, 'baz');
  });

  suite("name value", () => {
    let binder: Binder<Order, OrderModel<Order>>;

    const expectedEmptyOrder: Order = {
      idString: '',
      customer: {
        idString: '',
        fullName: '',
        nickName: ''
      },
      notes: '',
      priority: 0,
      products: []
    };

    beforeEach(() => {
      binder = new Binder(
        litOrderView,
        OrderModel
      );
      requestUpdateStub.reset();
    });

    test("should have name for models", () => {
      assert.equal(binder.for(binder.model.notes).name, "notes");
      assert.equal(binder.for(binder.model.customer.fullName).name, "customer.fullName");
    });

    test("should have initial defaultValue", () => {
      assert.deepEqual(binder.defaultValue, expectedEmptyOrder);
    });

    test("should have valueOf", () => {
      assert.equal(binder.model.notes.valueOf(), "");
      assert.equal(binder.model.priority.valueOf(), 0);
    });

    test("should have toString", () => {
      assert.equal(binder.model.notes.valueOf(), "");
      assert.equal(binder.model.priority.toString(), "0");
    });

    test("should have initial value", () => {
      assert.equal(binder.value, binder.defaultValue);
      assert.equal(binder.for(binder.model).value, binder.value);
      assert.equal(binder.for(binder.model.notes).value, "");
      assert.equal(binder.for(binder.model.customer.fullName).value, "");
      assert.equal(binder.model.valueOf(), binder.value);
      assert.equal(binder.model.notes.valueOf(), "");
      assert.equal(binder.model.customer.fullName.valueOf(), "");
    });

    test("should change value on setValue", () => {
      // Sanity check: requestUpdate should not be called
      sinon.assert.notCalled(requestUpdateStub);

      binder.for(binder.model.notes).value = "foo";
      assert.equal(binder.value.notes, "foo");
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should change value on deep setValue", () => {
      sinon.assert.notCalled(requestUpdateStub);

      binder.for(binder.model.customer.fullName).value = "foo";
      assert.equal(binder.value.customer.fullName, "foo");
      sinon.assert.calledOnce(litOrderView.requestUpdate);
    });

    test("should not change defaultValue on setValue", () => {
      binder.for(binder.model.notes).value = "foo";
      binder.for(binder.model.customer.fullName).value = "foo";

      assert.equal(binder.defaultValue.notes, "");
      assert.equal(binder.defaultValue.customer.fullName, "");
    });

    test("should reset to default value", () => {
      binder.for(binder.model.notes).value = "foo";
      binder.for(binder.model.customer.fullName).value = "foo";
      requestUpdateStub.reset();

      binder.reset();

      assert.equal(binder.value.notes, "");
      assert.equal(binder.value.customer.fullName, "");
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should reset to provided value", () => {
      binder.for(binder.model.notes).value = "foo";
      binder.for(binder.model.customer.fullName).value = "foo";
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

    test("should clear value and default value", () => {
      binder.reset({
        ...expectedEmptyOrder,
        notes: "bar",
        customer: {
          ...expectedEmptyOrder.customer,
          fullName: "bar"
        }
      });
      requestUpdateStub.reset();
      assert.notDeepEqual(binder.value, expectedEmptyOrder);
      assert.notDeepEqual(binder.defaultValue, expectedEmptyOrder);

      binder.clear();

      assert.deepEqual(binder.value, expectedEmptyOrder);
      assert.deepEqual(binder.defaultValue, expectedEmptyOrder);
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should update when clearing validation", async () => {
      binder.clear();
      const binderNode = binder.for(binder.model.customer.fullName);
      await binderNode.validate();
      assert.isTrue(binderNode.invalid);
      requestUpdateStub.reset();

      binder.clear();

      assert.isFalse(binderNode.invalid);
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should not update excessively when nothing to clear", async () => {
      binder.clear();
      const binderNode = binder.for(binder.model.customer.fullName);
      await binderNode.validate();
      assert.isTrue(binderNode.invalid);
      binder.clear();
      requestUpdateStub.reset();

      binder.clear();
      sinon.assert.notCalled(requestUpdateStub);
    });

    test("should forget visits on clear", () => {
      const binderNode = binder.for(binder.model.customer.fullName);
      binderNode.visited = true;

      binder.clear();

      assert.isFalse(binderNode.visited);
    });

    test('should be able to set null to object type property', () => {
      const myBinder:Binder<TestEntity, TestModel<TestEntity>> = new Binder(document.createElement('div'), TestModel);
      myBinder.for(myBinder.model.fieldAny).value = null;
      myBinder.for(myBinder.model.fieldAny).validate();
      assert.isFalse(myBinder.invalid);
    });

    test('should be able to set undefined to object type property', () => {
      const myBinder:Binder<TestEntity, TestModel<TestEntity>> = new Binder(document.createElement('div'), TestModel);
      myBinder.for(myBinder.model.fieldAny).value = undefined;
      myBinder.for(myBinder.model.fieldAny).validate();
      assert.isFalse(myBinder.invalid);
    });
  });

  suite("optional", () => {
    let binder: Binder<Employee, EmployeeModel<Employee>>;
    const litEmployeeView = document.createElement('lit-employee-view') as LitEmployeeView;

    const expectedEmptyEmployee: Employee = {
      idString: '',
      fullName: ''
    };

    beforeEach(() => {
      binder = new Binder(
        litEmployeeView,
        EmployeeModel
      );
    });

    function getOnlyEmployeeData(e: Employee) {
      const {idString, fullName} = e;
      return {idString, fullName};
    }

    test('should not initialize optional in empty value', () => {
      const emptyValue = EmployeeModel.createEmptyValue();
      assert.isUndefined(emptyValue.supervisor);
    });

    test('should not initialize optional in binder value and default value', () => {
      assert.isUndefined(binder.defaultValue.supervisor);
      assert.deepEqual(binder.defaultValue, expectedEmptyEmployee);
      assert.isUndefined(binder.value.supervisor);
      assert.deepEqual(binder.value, expectedEmptyEmployee);
    });

    test('should initialize optional on binderNode access', () => {
      binder.for(binder.model.supervisor);

      assert.isDefined(binder.defaultValue.supervisor);
      assert.deepEqual(binder.defaultValue.supervisor, expectedEmptyEmployee);
      assert.deepEqual(getOnlyEmployeeData(binder.defaultValue), expectedEmptyEmployee);
      assert.isDefined(binder.value.supervisor);
      assert.deepEqual(binder.value.supervisor, expectedEmptyEmployee);
      assert.deepEqual(getOnlyEmployeeData(binder.value), expectedEmptyEmployee);
    });

    test('should initialize optional on deep binderNode access', () => {
      binder.for(binder.model.supervisor.supervisor);

      assert.isDefined(binder.defaultValue.supervisor!.supervisor);
      assert.deepEqual(getOnlyEmployeeData(binder.defaultValue), expectedEmptyEmployee);
      assert.deepEqual(getOnlyEmployeeData(binder.defaultValue.supervisor!), expectedEmptyEmployee);
      assert.deepEqual(getOnlyEmployeeData(binder.defaultValue.supervisor!.supervisor!), expectedEmptyEmployee);
      assert.isDefined(binder.value.supervisor!.supervisor);
      assert.deepEqual(getOnlyEmployeeData(binder.value), expectedEmptyEmployee);
      assert.deepEqual(getOnlyEmployeeData(binder.value.supervisor!), expectedEmptyEmployee);
      assert.deepEqual(getOnlyEmployeeData(binder.value.supervisor!.supervisor!), expectedEmptyEmployee);
    });

    test('should not become dirty on binderNode access', () => {
      assert.isFalse(binder.dirty);

      binder.for(binder.model.supervisor);
      assert.isFalse(binder.dirty);

      binder.for(binder.model.supervisor.supervisor);
      assert.isFalse(binder.dirty);
    });
  });
});
