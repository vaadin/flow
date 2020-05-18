/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');
import { expect } from "chai";

// API to test
import {
  appendItem,
  Binder,
  field,
  getModelValidators,
  getName,
  getValue,
  keySymbol,
  modelRepeat,
  prependItem,
  Required,
  setValue,
  validate,
  ValidationError,
  Validator,
  BinderConfiguration
} from "../../main/resources/META-INF/resources/frontend/form";

import { IdEntity, IdEntityModel,  Order, OrderModel, ProductModel } from "./BinderModels";

import { css, customElement, html, LitElement, query} from 'lit-element';

@customElement('lit-order-view')
class LitOrderView extends LitElement {}

@customElement('order-view')
class OrderView extends LitElement {
  public binder = new Binder(this, OrderModel);
  @query('#notes') public notes!: HTMLInputElement;
  @query('#fullName') public fullName!: HTMLInputElement;
  @query('#nickName') public nickName!: HTMLInputElement;
  @query('#add') public add!: Element;
  @query('#description0') public description!: HTMLInputElement;
  @query('#price0') public price!: HTMLInputElement;

  static get styles() {
    return css`input[invalid] {border: 2px solid red;}`;
  }
  render() {
    return html`
    <input id="notes" ...="${field(this.binder.model.notes)}" />
    <input id="fullName" ...="${field(this.binder.model.customer.fullName)}" />
    <input id="nickName" ...="${field(this.binder.model.customer.nickName)}" />
    <button id="add" @click=${() => appendItem(this.binder.model.products)}>+</button>
    ${modelRepeat(this.binder.model.products, (model, _product, index) => html`<div>
        <input id="description${index}" ...="${field(model.description)}" />
        <input id="price${index}" ...="${field(model.price)}">
      </div>`)}
    `;
  }
}

const sleep = async (t: number) => new Promise(resolve => setTimeout(() => resolve(), t));
const fireEvent = async (elm: Element, name: string) => {
  elm.dispatchEvent(new CustomEvent(name));
  return sleep(0);
}

suite("Binder", () => {
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

  test("should be able to create a binder with a default onchang listener", () => {
    const binder = new Binder(litOrderView, OrderModel);

    setValue(binder.model.notes, "foo");

    sinon.assert.calledTwice(requestUpdateStub);
  });

  test("should be able to create a binder with a custom onchang listener", () => {
    let foo = 'bar';
    const config: BinderConfiguration<Order> = {
      onChange: () => { foo = 'baz' }
    }

    const binder = new Binder(litOrderView, OrderModel, config);

    setValue(binder.model.notes, "foo");

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
      assert.equal(getName(binder.model.notes), "notes");
      assert.equal(getName(binder.model.customer.fullName), "customer.fullName");
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
      sinon.assert.calledOnce(litOrderView.requestUpdate);
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

    test("should clear value", () => {
      binder.reset({
        ...expectedEmptyOrder,
        notes: "bar",
        customer: {
          ...expectedEmptyOrder.customer,
          fullName: "bar"
        }
      });
      assert.notDeepEqual(binder.value, expectedEmptyOrder);

      binder.clear();

      assert.deepEqual(binder.value, expectedEmptyOrder);
    });
  });

  suite('array-model', () => {
    let binder: Binder<Order, OrderModel<Order>>;

    beforeEach(() => {
      binder = new Binder(litOrderView, OrderModel);
    });

    test("should reuse model instance for the same array item", async () => {
      const products = [
        ProductModel.createEmptyValue(),
        ProductModel.createEmptyValue()
      ]
      setValue(binder.model.products, products.slice());
      const models_1 = [...binder.model.products].slice();
      [0, 1].forEach(i => expect(models_1[i].valueOf()).to.be.equal(products[i]));

      setValue(binder.model.products, products);
      const models_2 = [...binder.model.products].slice();
      [0, 1].forEach(i => {
        expect(models_1[i]).to.be.equal(models_2[i]);
        expect(models_2[i].valueOf()).to.be.equal(products[i]);
      });
    });

    test("should reuse model instance for the same array item after it is modified", async () => {
      const products = [
        ProductModel.createEmptyValue(),
        ProductModel.createEmptyValue()
      ]
      setValue(binder.model.products, products);
      const models_1 = [...binder.model.products].slice();
      [0, 1].forEach(i => expect(models_1[i].valueOf()).to.be.equal(products[i]));

      setValue(models_1[0].description, 'foo');
      setValue(models_1[1].description, 'bar');

      setValue(binder.model.products, products.slice());
      const models_2 = [...binder.model.products].slice();
      [0, 1].forEach(i => {
        expect(models_1[i]).to.be.equal(models_2[i]);
        expect(models_2[i].valueOf()).to.be.equal(products[i]);
      });
    });

    test("should update model keySymbol when inserting items", async () => {
      const products = [
        ProductModel.createEmptyValue(),
        ProductModel.createEmptyValue()
      ]
      setValue(binder.model.products, products);

      const models_1 = [...binder.model.products].slice();
      for (let i = 0; i < models_1.length; i++) {
        expect((models_1[i] as any)[keySymbol]).to.be.equal(i)
      }

      setValue(models_1[0].description, 'foo');
      expect(binder.model.products.valueOf()[0].description).to.be.equal('foo');

      prependItem(binder.model.products);
      expect(binder.model.products.valueOf()[1].description).to.be.equal('foo');

      const models_2 = [...binder.model.products].slice();
      expect(models_2.length).to.be.equal(3);
      for (let i = 0; i < models_2.length; i++) {
        expect((models_2[i] as any)[keySymbol]).to.be.equal(i)
      }
    });
  });

  suite("validation", () => {
    let binder: Binder<Order, OrderModel<Order>>;

    beforeEach(async () => {
      binder = new Binder(
        litOrderView,
        OrderModel
      );
    });

    test("should run all validators per model", async () => {
      return validate(binder.model.customer).then(errors => {
        expect(errors.map(e => e.validator.constructor.name).sort()).to.eql([
          "Required",
          "Size"
        ]);
      });
    });

    test("should run all nested validations per model", async () => {
      return validate(binder.model).then(errors => {
        expect(errors.map(e => e.property)).to.eql([
          "customer.fullName",
          "customer.fullName",
          "notes"
        ]);
      });
    });

    test("should run all validations per array items", async () => {
      appendItem(binder.model.products);
      appendItem(binder.model.products);
      return validate(binder.model).then(errors => {
        expect(errors.map(e => e.property)).to.eql([
          "customer.fullName",
          "customer.fullName",
          "notes",
          "products.0.description",
          "products.0.price",
          "products.1.description",
          "products.1.price"
        ]);
      });
    });

    suite('submitTo', () => {
      test("should throw on validation failure", async () => {
        try {
          await binder.submitTo(async() => {});
          expect.fail();
        } catch (error) {
          expect(error.errors.length).to.gt(0);
        }
      });

      test("should re-throw on server failure", async () => {
        setValue(binder.model.customer.fullName, 'foobar');
        setValue(binder.model.notes, 'whatever');
        try {
          await binder.submitTo(async() => {throw new Error('whatever')});
          expect.fail();
        } catch (error) {
          expect(error.message).to.be.equal('whatever');
        }
      });

      test("should wrap server validation error", async () => {
        setValue(binder.model.customer.fullName, 'foobar');
        setValue(binder.model.notes, 'whatever');
        try {
          await binder.submitTo(async() => {throw {
            message: "Validation error in endpoint 'MyEndpoint' method 'saveMyBean'",
            validationErrorData: [{
              message: "Object of type 'com.example.MyBean' has invalid property 'foo' with value 'baz', validation error: 'custom message'",
              parameterName: "foo",
            }]
          }});
          expect.fail();
        } catch (error) {
          expect(error.errors[0].validator.message).to.be.equal('custom message');
          expect(error.errors[0].value).to.be.equal('baz');
          expect(error.errors[0].property).to.be.equal('foo');
        }
      });

      test("should wrap server validation error with any message", async () => {
        setValue(binder.model.customer.fullName, 'foobar');
        setValue(binder.model.notes, 'whatever');
        try {
          await binder.submitTo(async() => {throw {
            message: "Validation error in endpoint 'MyEndpoint' method 'saveMyBean'",
            validationErrorData: [{
              message: "Custom server message",
              parameterName: "bar",
            }]
          }});
          expect.fail();
        } catch (error) {
          expect(error.errors[0].validator.message).to.be.equal('Custom server message');
          expect(error.errors[0].value).to.be.undefined;
          expect(error.errors[0].property).to.be.equal('bar');
        }
      });
    });

    suite('model add validator', () => {
      let binder: Binder<IdEntity, IdEntityModel<IdEntity>>;

      beforeEach(async () => {
        binder = new Binder(
          {
            requestUpdate: ()=>{}
          },
          IdEntityModel
        );
      });

      test("should not have validation errors for a model without validators", async () => {
        assert.isEmpty(await validate(binder.model));
      });

      test("should fail validation after adding a synchronous validator to the model", async () => {
        getModelValidators(binder.model).add({message: 'foo', validate: () => false});
        return validate(binder.model).then(errors => {
          expect(errors[0].validator.message).to.equal("foo");
          expect(errors[0].property).to.equal('');
          expect(errors[0].value).to.eql({idString: ''});
        });
      });

      test("should fail validation after adding an asynchronous validator to the model", async () => {
        class AsyncValidator implements Validator<Order>{
          message = "bar";
          validate = async () => {
            await sleep(10);
            return false;
          };
        }
        getModelValidators(binder.model).add(new AsyncValidator());
        return validate(binder.model).then(errors => {
          expect(errors[0].validator.message).to.equal("bar");
        });
      });

      test("should not have validations errors after adding validators to properties if property is not required", async () => {
        getModelValidators(binder.model.idString).add({message: 'foo', validate: () => false});
        const errors = await validate(binder.model);
        assert.isEmpty(errors);
      });

      test("should fail after adding validators to properties if property is not required but it has a value", async () => {
        setValue(binder.model.idString, 'bar');
        getModelValidators(binder.model.idString).add({message: 'foo', validate: () => false});
        const errors = await validate(binder.model);
        expect(errors[0].validator.message).to.equal("foo");
        expect(errors[0].property).to.equal('idString');
        expect(errors[0].value).to.eql('bar');
      });

      test("should fail after adding validators to properties if required and not value", async () => {
        getModelValidators(binder.model.idString).add({message: 'foo', validate: () => false});
        getModelValidators(binder.model.idString).add(new Required());
        const errors = await validate(binder.model);
        expect(errors.length).to.equal(2);
      });
    });

    suite('field element', () => {
      let orderView: OrderView;

      beforeEach(async () => {
        orderView = document.createElement('order-view') as OrderView;
        binder = new Binder(orderView, OrderModel);
        document.body.appendChild(orderView);
        return sleep(10);
      });

      afterEach(async () => {
        document.body.removeChild(orderView)
      });

      ['change', 'blur'].forEach(event => {
        test(`should validate field on ${event}`, async () => {
          expect(orderView.notes.hasAttribute('invalid')).to.be.false;
          await fireEvent(orderView.notes, event);
          expect(orderView.notes.hasAttribute('invalid')).to.be.true;
        });

        test(`should validate field of nested model on  ${event}`, async () => {
          await fireEvent(orderView.add, 'click');
          expect(orderView.description.hasAttribute('invalid')).to.be.false;
          await fireEvent(orderView.description, event);
          expect(orderView.description.hasAttribute('invalid')).to.be.true;
        });
      });

      test(`should not validate field on input when first visit`, async () => {
        expect(orderView.notes.hasAttribute('invalid')).to.be.false;
        await fireEvent(orderView.notes, 'input');
        expect(orderView.notes.hasAttribute('invalid')).to.be.false;
      });

      test(`should validate field on input after first visit`, async () => {
        orderView.notes.value = 'foo';
        await fireEvent(orderView.notes, 'blur');
        expect(orderView.notes.hasAttribute('invalid')).to.be.false;

        orderView.notes.value = '';
        await fireEvent(orderView.notes, 'input');
        expect(orderView.notes.hasAttribute('invalid')).to.be.true;
      });

      test(`should validate fields on submit`, async () => {
        expect(orderView.notes.hasAttribute('invalid')).to.be.false;
        expect(orderView.fullName.hasAttribute('invalid')).to.be.false;
        expect(orderView.nickName.hasAttribute('invalid')).to.be.false;

        try {
          await orderView.binder.submitTo(async (item) => item);
          expect.fail();
        } catch (error) {
        }

        expect(orderView.notes.hasAttribute('invalid')).to.be.true;
        expect(orderView.fullName.hasAttribute('invalid')).to.be.true;
        expect(orderView.nickName.hasAttribute('invalid')).to.be.false;
      });

      test(`should validate fields of nested model on submit`, async () => {
        expect(orderView.description).to.be.null;
        await fireEvent(orderView.add, 'click');
        await fireEvent(orderView.add, 'click');

        expect(orderView.description.hasAttribute('invalid')).to.be.false;
        expect(orderView.price.hasAttribute('invalid')).to.be.false;

        try {
          await orderView.binder.submitTo(async (item) => item);
          expect.fail();
        } catch (error) {
          expect((error as ValidationError).errors.map(e => e.property)).to.be.eql([
            'customer.fullName',
            'customer.fullName',
            'notes',
            'products.0.description',
            'products.0.price',
            'products.1.description',
            'products.1.price'
          ]);
        }

        expect(orderView.description.hasAttribute('invalid')).to.be.true;
        expect(orderView.price.hasAttribute('invalid')).to.be.true;
      });

      test(`should validate fields of arrays on submit`, async () => {
        expect(orderView.description).to.be.null;
        await fireEvent(orderView.add, 'click');
        await fireEvent(orderView.add, 'click');

        expect(orderView.description.hasAttribute('invalid')).to.be.false;
        expect(orderView.price.hasAttribute('invalid')).to.be.false;

        try {
          await orderView.binder.submitTo(async (item) => item);
          expect.fail();
        } catch (error) {
        }

        expect(orderView.description.hasAttribute('invalid')).to.be.true;
        expect(orderView.price.hasAttribute('invalid')).to.be.true;
      });

      test(`should not submit when just validation fails`, async () => {
        expect(orderView.description).to.be.null;
        await fireEvent(orderView.add, 'click');

        orderView.notes.value = 'foo';
        await fireEvent(orderView.notes, 'change');
        orderView.fullName.value = 'manuel';
        await fireEvent(orderView.fullName, 'change');
        orderView.description.value = 'bread';
        await fireEvent(orderView.description, 'change');

        try {
          await orderView.binder.submitTo(async (item) => item);
          expect.fail();
        } catch (error) {
        }
      });

      test(`should submit when no validation errors`, async () => {
        expect(orderView.description).to.be.null;
        await fireEvent(orderView.add, 'click');

        orderView.notes.value = 'foo';
        await fireEvent(orderView.notes, 'change');
        orderView.fullName.value = 'manuel';
        await fireEvent(orderView.fullName, 'change');
        orderView.description.value = 'bread';
        await fireEvent(orderView.description, 'change');
        orderView.price.value = '10';
        await fireEvent(orderView.price, 'change');

        const item = await orderView.binder.submitTo(async (item) => item) as Order;
        expect(item).not.to.be.undefined;
        expect(item.products[0].description).to.be.equal('bread');
        expect(item.products[0].price).to.be.equal(10);
        expect(item.notes).to.be.equal('foo');
        expect(item.customer.fullName).to.be.equal('manuel');
      });
    });

  });

  suite('field with text-field', () => {
    @customElement('mock-text-field')
    class MockTextFieldElement extends HTMLElement {
      // pretend it’s a Vaadin component to use VaadinFieldStrategy
      static get version() {
        return '0.0.0';
      }

      __value = '';
      get value() {
        return this.__value;
      }
      set value(value) {
        this.__value = value;
      }
      valueSpy = sinon.spy(this, 'value', ['get', 'set']);

      __required = false;
      get required() {
        return this.__required;
      }
      set required(value) {
        this.__required = value;
      }
      requiredSpy = sinon.spy(this, 'required', ['get', 'set']);

      setAttributeSpy = sinon.spy(this, 'setAttribute');
    }

    let orderViewWithTextField: OrderViewWithTextField;

    @customElement('order-view-with-text-field')
    class OrderViewWithTextField extends LitElement {
      requestUpdateSpy = sinon.spy(this, 'requestUpdate');

      binder = new Binder(this, OrderModel);

      @query('#notesField')
      notesField?: MockTextFieldElement;

      @query('#customerFullNameField')
      customerFullNameField?: MockTextFieldElement;

      @query('#customerNickNameField')
      customerNickNameField?: MockTextFieldElement;

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

          <mock-text-field
           id="customerNickNameField"
           ...="${field(this.binder.model.customer.nickName)}"
           ></mock-text-field>
        `;
      }
    }

    beforeEach(async () => {
      orderViewWithTextField = document.createElement('order-view-with-text-field') as OrderViewWithTextField;
      document.body.appendChild(orderViewWithTextField);
      await orderViewWithTextField.updateComplete;
    });

    afterEach(async () => {
      document.body.removeChild(orderViewWithTextField);
    });

    test('should set name attribute', () => {
      sinon.assert.calledOnceWithExactly(orderViewWithTextField.notesField!.setAttributeSpy, 'name', 'notes');
      sinon.assert.calledOnceWithExactly(orderViewWithTextField.customerFullNameField!.setAttributeSpy, 'name', 'customer.fullName');
    });

    test('should only set name attribute once', async () => {
      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;
      sinon.assert.calledOnceWithExactly(orderViewWithTextField.notesField!.setAttributeSpy, 'name', 'notes');
    });

    test('should set required property when required', async () => {
      sinon.assert.calledOnceWithExactly(orderViewWithTextField.customerFullNameField!.requiredSpy.set, true);
      sinon.assert.notCalled(orderViewWithTextField.customerNickNameField!.requiredSpy.set);

      setValue(orderViewWithTextField.binder.model.customer.fullName, 'foo');
      await orderViewWithTextField.updateComplete;
      setValue(orderViewWithTextField.binder.model.customer.nickName, 'bar');
      await orderViewWithTextField.updateComplete;

      sinon.assert.calledOnceWithExactly(orderViewWithTextField.customerFullNameField!.requiredSpy.set, true);
      sinon.assert.notCalled(orderViewWithTextField.customerNickNameField!.requiredSpy.set);
    });

    test('should set value property on setValue', async () => {
      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;
      sinon.assert.calledOnceWithExactly(orderViewWithTextField.notesField!.valueSpy.set, 'foo');
    });

    test('should set given non-empty value on reset with argument', async () => {
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

    test('should set given empty value on reset with argument', async () => {
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

    test('should set default value on reset without argument', async () => {
      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;
      orderViewWithTextField.notesField!.valueSpy.set.resetHistory();

      orderViewWithTextField.binder.reset();
      await orderViewWithTextField.updateComplete;

      sinon.assert.calledWith(orderViewWithTextField.notesField!.valueSpy.set, '');
    });

    test('should update binder value on setValue', async () => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();

      setValue(orderViewWithTextField.binder.model.notes, 'foo');
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      sinon.assert.calledOnce(orderViewWithTextField.requestUpdateSpy);
    });

    test('should update binder value on input event', async () => {
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

    test('should update binder value on change event', async () => {
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

    test('should update binder value on blur event', async () => {
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

  suite('field with input', () => {
    @customElement('mock-input')
    class MockInputElement extends HTMLElement {
      __value = '';
      get value() {
        return this.__value;
      }
      set value(value) {
        this.__value = value;
      }
      valueSpy = sinon.spy(this, 'value', ['get', 'set']);

      setAttributeSpy = sinon.spy(this, 'setAttribute');
    }

    @customElement('order-view-with-input')
    class OrderViewWithInput extends LitElement {
      requestUpdateSpy = sinon.spy(this, 'requestUpdate');
      binder = new Binder(this, OrderModel);

      @query('#notesField')
      notesField?: MockInputElement;

      @query('#customerFullNameField')
      customerFullNameField?: MockInputElement;

      @query('#customerNickNameField')
      customerNickNameField?: MockInputElement;

      render() {
        return html`
          <mock-input id="notesField" ...="${field(this.binder.model.notes)}"></mock-input>
          <mock-input id="customerFullNameField" ...="${field(this.binder.model.customer.fullName)}"></mock-input>
          <mock-input id="customerNickNameField" ...="${field(this.binder.model.customer.nickName)}"></mock-input>
        `;
      }
    }

    let orderViewWithInput: OrderViewWithInput;

    beforeEach(async () => {
      orderViewWithInput = document.createElement('order-view-with-input') as OrderViewWithInput;
      document.body.appendChild(orderViewWithInput);
      await orderViewWithInput.updateComplete;
    });

    afterEach(async () => {
      document.body.removeChild(orderViewWithInput);
    });

    test('should set name and required attributes once', async () => {
      sinon.assert.calledTwice(orderViewWithInput.customerFullNameField!.setAttributeSpy);
      assert.deepEqual(orderViewWithInput.customerFullNameField!.setAttributeSpy.getCall(0).args, ['name', 'customer.fullName']);
      assert.deepEqual(orderViewWithInput.customerFullNameField!.setAttributeSpy.getCall(1).args, ['required', '']);
      sinon.assert.calledOnce(orderViewWithInput.customerNickNameField!.setAttributeSpy);
      assert.deepEqual(orderViewWithInput.customerNickNameField!.setAttributeSpy.getCall(0).args, ['name', 'customer.nickName']);

      setValue(orderViewWithInput.binder.model.customer.fullName, 'foo');
      await orderViewWithInput.updateComplete;
      setValue(orderViewWithInput.binder.model.customer.nickName, 'bar');
      await orderViewWithInput.updateComplete;

      sinon.assert.calledTwice(orderViewWithInput.customerFullNameField!.setAttributeSpy);
      sinon.assert.calledOnce(orderViewWithInput.customerNickNameField!.setAttributeSpy);
    });
  });
});
