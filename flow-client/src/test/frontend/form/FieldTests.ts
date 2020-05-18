/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');
import { expect } from "chai";

// API to test
import {
  Binder,
  field,
  setValue,
  // GenericFieldStrategy,
  // CheckedFieldStrategy,
  // SelectedFieldStrategy,
  // VaadinFieldStrategy,
  Required,
  AbstractModel
} from "../../../main/resources/META-INF/resources/frontend/form";

import { OrderModel, TestModel } from "./TestModels";

import { customElement, html, LitElement, query} from 'lit-element';
import { PropertyPart, AttributeCommitter } from "lit-html";

suite("form/Field", () => {

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


  suite('field/Strategy', () => {
    const binder = new Binder(document.createElement('div'), TestModel);

    ['div',
     'input',
     'vaadin-rich-text-editor'
    ].forEach(tag => {
      test(`GenericFieldStrategy ${tag}`, async() => {
        const element: Element & {value?: any} = document.createElement(tag);
        const model = binder.model.fieldString;
        const binderNode = binder.for(model);
        binderNode.value = 'foo';
        binderNode.addValidator({message: 'any-err-msg', validate: () => false});

        const part = new PropertyPart(new AttributeCommitter(element, '..', []));
        field(model)(part);
        // const strategy = (model as any)[fieldSymbol];
        //
        // expect(strategy instanceof GenericFieldStrategy).to.be.true;
        // expect(strategy.value).to.be.equal('foo');

        await binderNode.validate();
        field(model)(part);
        expect(element.hasAttribute('invalid')).to.be.true;
        expect(element.hasAttribute('errorMessage')).to.be.false;
      });
    });

    [{tag: 'input', type: 'checkbox'},
     {tag: 'input', type: 'radio'},
     {tag: 'vaadin-checkbox', type: ''},
     {tag: 'vaadin-radio-button', type: ''}
    ].forEach(({tag, type}) => {
      test(`CheckedFieldStrategy ${tag} ${type}`, async() => {
        const element: Element & {checked?: boolean} = document.createElement(tag);
        type && element.setAttribute('type', type);
        const model = binder.model.fieldBoolean;
        const binderNode = binder.for(model);

        binderNode.value = true;
        binderNode.addValidator({message: 'any-err-msg', validate: () => false});

        const part = new PropertyPart(new AttributeCommitter(element, '..', []));
        field(model)(part);
        // const strategy = (model as any)[fieldSymbol];
        //
        // expect(strategy instanceof CheckedFieldStrategy).to.be.true;
        // expect(strategy.value).to.be.true;
        expect(element.checked).to.be.true;

        await binderNode.validate();
        field(model)(part);
        expect(element.hasAttribute('invalid')).to.be.true;
        expect(element.hasAttribute('errorMessage')).to.be.false;
      });
    });

    test(`SelectedFieldStrategy`, async () => {
      const element: Element & {selected?: boolean} = document.createElement('vaadin-list-box');
      const model = binder.model.fieldBoolean;
      const binderNode = binder.for(model);

      binderNode.value = true;
      binderNode.addValidator({message: 'any-err-msg', validate: () => false});

      const part = new PropertyPart(new AttributeCommitter(element, '..', []));
      field(model)(part);
      // const strategy = (model as any)[fieldSymbol];

      // expect(strategy instanceof SelectedFieldStrategy).to.be.true;
      // expect(strategy.value).to.be.true;
      expect(element.selected).to.be.true;

      await binderNode.validate();
      field(model)(part);
      expect(element.hasAttribute('invalid')).to.be.true;
      expect(element.hasAttribute('errorMessage')).to.be.false;
    });


    [{model: binder.model.fieldString as AbstractModel<any>, value: 'a-string-value'},
     {model: binder.model.fieldBoolean as AbstractModel<any>, value: true},
     {model: binder.model.fieldNumber as AbstractModel<any>, value: 10},
     {model: binder.model.fieldObject as AbstractModel<any>, value: {foo: true}},
     {model: binder.model.fieldArrayString as AbstractModel<any>, value: ['a', 'b']},
     {model: binder.model.fieldArrayModel as AbstractModel<any>, value: [{idString: 'id'}]}
    ].forEach(async ({model, value}, idx) => {
      test(`VaadinFieldStrategy ${model.constructor.name} ${idx}`, async () => {
        const element: Element & {
          value?: any,
          invalid?: boolean,
          required?: boolean,
          errorMessage?: string} = document.createElement('any-vaadin-element-tag');
        (element.constructor as any).version = '1.0';
        const binderNode = binder.for(model);

        binderNode.value = value;
        binderNode.addValidator({message: 'any-err-msg', validate: () => false});
        binderNode.addValidator(new Required());

        const part = new PropertyPart(new AttributeCommitter(element, '..', []));
        field(model)(part);
        // const strategy = (model as any)[fieldSymbol];
        delete (element.constructor as any).version;

        // expect(strategy instanceof VaadinFieldStrategy).to.be.true;
        // expect(strategy.value).to.be.equal(value);
        expect(element.value).to.be.equal(value);
        expect(element.required).to.be.true;
        expect(element.invalid).to.be.undefined;
        expect(element.errorMessage).to.be.undefined;

        await binderNode.validate();
        field(model)(part);
        expect(element.invalid).to.be.true;
        expect(element.errorMessage).to.be.equal('any-err-msg');
      });
    });
  })
});
