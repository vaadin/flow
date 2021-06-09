/* eslint-disable no-unused-expressions, no-shadow */
import { assert, expect } from '@open-wc/testing';
import sinon from 'sinon';
import { LitElement, nothing, render } from 'lit';
import { html, unsafeStatic } from 'lit/static-html.js';
import { customElement, query } from 'lit/decorators.js';
import type { BinderNode } from '../../src/form/BinderNode';
// API to test
import {
  Binder,
  field,
  GenericFieldStrategy,
  CheckedFieldStrategy,
  SelectedFieldStrategy,
  VaadinFieldStrategy,
  Required,
  AbstractModel,
  FieldStrategy,
  AbstractFieldStrategy,
} from '../../src/form';
import { OrderModel, TestModel, TestEntity, Order } from './TestModels';

describe('form/Field', () => {
  describe('field with text-field', () => {
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
        // Native inputs stringify incoming values
        this.__value = String(value);
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

      @query('#priorityField')
      priorityField?: MockTextFieldElement;

      render() {
        return html`
          <mock-text-field id="notesField" ...="${field(this.binder.model.notes)}"></mock-text-field>

          <mock-text-field
            id="customerFullNameField"
            ...="${field(this.binder.model.customer.fullName)}"
          ></mock-text-field>

          <mock-text-field
            id="customerNickNameField"
            ...="${field(this.binder.model.customer.nickName)}"
          ></mock-text-field>

          <mock-text-field id="priorityField" ...="${field(this.binder.model.priority)}"></mock-text-field>
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

    it('should set name attribute', () => {
      expect(orderViewWithTextField.notesField!.setAttributeSpy).to.be.calledOnceWithExactly('name', 'notes');
      expect(orderViewWithTextField.customerFullNameField!.setAttributeSpy).to.be.calledOnceWithExactly(
        'name',
        'customer.fullName'
      );
    });

    it('should only set name attribute once', async () => {
      orderViewWithTextField.binder.for(orderViewWithTextField.binder.model.notes).value = 'foo';
      await orderViewWithTextField.updateComplete;
      expect(orderViewWithTextField.notesField!.setAttributeSpy).to.be.calledOnceWithExactly('name', 'notes');
    });

    it('should set required property when required', async () => {
      expect(orderViewWithTextField.customerFullNameField!.requiredSpy.set).to.be.calledOnceWithExactly(true);
      expect(orderViewWithTextField.customerNickNameField!.requiredSpy.set).to.not.be.called;

      orderViewWithTextField.binder.for(orderViewWithTextField.binder.model.customer.fullName).value = 'foo';
      await orderViewWithTextField.updateComplete;
      orderViewWithTextField.binder.for(orderViewWithTextField.binder.model.customer.nickName).value = 'bar';
      await orderViewWithTextField.updateComplete;

      expect(orderViewWithTextField.customerFullNameField!.requiredSpy.set).to.be.calledOnceWithExactly(true);
      expect(orderViewWithTextField.customerNickNameField!.requiredSpy.set).to.not.be.called;
    });

    it('should set value property on setValue', async () => {
      orderViewWithTextField.binder.for(orderViewWithTextField.binder.model.notes).value = 'foo';
      await orderViewWithTextField.updateComplete;
      expect(orderViewWithTextField.notesField!.valueSpy.set, 'foo').to.be.calledOnceWithExactly;
    });

    it('should set given non-empty value on reset with argument', async () => {
      const emptyOrder = OrderModel.createEmptyValue();
      orderViewWithTextField.binder.read({
        ...emptyOrder,
        notes: 'foo',
        customer: {
          ...emptyOrder.customer,
          fullName: 'bar',
        },
      });
      await orderViewWithTextField.updateComplete;

      expect(orderViewWithTextField.notesField!.valueSpy.set, 'foo').to.be.calledWith;
      expect(orderViewWithTextField.customerFullNameField!.valueSpy.set, 'bar').to.be.calledWith;
    });

    it('should set given empty value on reset with argument', async () => {
      const emptyOrder = OrderModel.createEmptyValue();
      orderViewWithTextField.binder.read({
        ...emptyOrder,
        notes: 'foo',
        customer: {
          ...emptyOrder.customer,
          fullName: 'bar',
        },
      });
      await orderViewWithTextField.updateComplete;
      orderViewWithTextField.notesField!.valueSpy.set.resetHistory();

      orderViewWithTextField.binder.read(OrderModel.createEmptyValue());
      await orderViewWithTextField.updateComplete;

      expect(orderViewWithTextField.notesField!.valueSpy.set, '').to.be.calledWith;
      expect(orderViewWithTextField.customerFullNameField!.valueSpy.set, '').to.be.calledWith;
    });

    it('should set default value on reset without argument', async () => {
      orderViewWithTextField.binder.for(orderViewWithTextField.binder.model.notes).value = 'foo';
      await orderViewWithTextField.updateComplete;
      orderViewWithTextField.notesField!.valueSpy.set.resetHistory();

      orderViewWithTextField.binder.reset();
      await orderViewWithTextField.updateComplete;

      expect(orderViewWithTextField.notesField!.valueSpy.set, '').to.be.calledWith;
    });

    it('should update binder value on setValue', async () => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();

      orderViewWithTextField.binder.for(orderViewWithTextField.binder.model.notes).value = 'foo';
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      expect(orderViewWithTextField.requestUpdateSpy).to.be.calledOnce;
    });

    it('should update binder value on input event', async () => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();
      orderViewWithTextField.notesField!.value = 'foo';
      orderViewWithTextField.notesField!.dispatchEvent(
        new CustomEvent('input', { bubbles: true, composed: true, cancelable: false })
      );
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      expect(orderViewWithTextField.requestUpdateSpy).to.be.calledOnce;
    });

    it('should update binder value on change event', async () => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();
      orderViewWithTextField.notesField!.value = 'foo';
      orderViewWithTextField.notesField!.dispatchEvent(
        new CustomEvent('change', { bubbles: true, composed: true, cancelable: false })
      );
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      expect(orderViewWithTextField.requestUpdateSpy).to.be.calledOnce;
    });

    it('should update binder value on blur event', async () => {
      orderViewWithTextField.requestUpdateSpy.resetHistory();
      orderViewWithTextField.notesField!.value = 'foo';
      orderViewWithTextField.notesField!.dispatchEvent(
        new CustomEvent('blur', { bubbles: true, composed: true, cancelable: false })
      );
      await orderViewWithTextField.updateComplete;

      assert.equal(orderViewWithTextField.binder.value.notes, 'foo');
      expect(orderViewWithTextField.requestUpdateSpy).to.be.calledOnce;
    });

    it('should set visited on blur event', async () => {
      const { binder } = orderViewWithTextField;
      const binderNode = binder.for(binder.model.notes);
      expect(binderNode.visited).to.be.false;

      orderViewWithTextField.notesField!.dispatchEvent(
        new CustomEvent('blur', { bubbles: true, composed: true, cancelable: false })
      );
      await orderViewWithTextField.updateComplete;

      expect(binderNode.visited).to.be.true;
    });

    describe('number model', () => {
      let view: OrderViewWithTextField;
      let priorityField: MockTextFieldElement;
      let binder: Binder<Order, OrderModel>;

      beforeEach(async () => {
        view = orderViewWithTextField;
        binder = view.binder;
        priorityField = view.priorityField!;
      });

      it('should set initial zero', async () => {
        expect(priorityField.value).to.equal('0');
      });

      it('should set number value from binder', async () => {
        priorityField.valueSpy.get.resetHistory();
        priorityField.valueSpy.set.resetHistory();

        binder.for(binder.model.priority).value = 1.2;
        await view.updateComplete;
        expect(priorityField.valueSpy.set).to.be.calledOnceWithExactly(1.2);
        expect(priorityField.valueSpy.set).to.be.calledOnceWithExactly(1.2);
      });

      it('should update binder value on typing', async () => {
        const cases: Array<[string, number]> = [
          ['1', 1],
          ['1.', NaN], // not allowed format
          ['1.2', 1.2],
          ['', NaN],
          ['not a number', NaN],
          ['.', NaN],
          ['.1', 0.1],
          // Invalid separator
          [',', NaN],
          [',2', NaN],
          ['1,', NaN],
          ['1,2', NaN],
        ];

        for (const [inputValue, expectedNumber] of cases) {
          for (const eventName of ['input', 'change']) {
            priorityField.value = inputValue;
            priorityField.valueSpy.get.resetHistory();
            priorityField.valueSpy.set.resetHistory();
            priorityField.dispatchEvent(
              new CustomEvent(eventName, { bubbles: true, composed: true, cancelable: false })
            );
            await view.updateComplete; // eslint-disable-line no-await-in-loop

            if (Number.isNaN(expectedNumber)) {
              // NaN never equals
              expect(binder.value.priority).to.satisfy(Number.isNaN);
            } else {
              expect(binder.value.priority).to.eq(expectedNumber);
            }
            expect(priorityField.valueSpy.get).to.be.calledOnce;
            // Should not change typed value
            expect(priorityField.valueSpy.set).to.not.be.called;
            expect(priorityField.value).to.equal(inputValue);

            priorityField.valueSpy.get.resetHistory();
            priorityField.valueSpy.set.resetHistory();
          }
        }
      });
    });
  });

  describe('field with input', () => {
    @customElement('mock-input')
    class MockInputElement extends HTMLElement {
      __value = '';

      get value() {
        return this.__value;
      }

      set value(value) {
        // Native inputs stringify incoming values
        this.__value = String(value);
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

      @query('#priorityField')
      priorityField?: MockInputElement;

      render() {
        return html`
          <mock-input id="notesField" ...="${field(this.binder.model.notes)}"></mock-input>
          <mock-input id="customerFullNameField" ...="${field(this.binder.model.customer.fullName)}"></mock-input>
          <mock-input id="customerNickNameField" ...="${field(this.binder.model.customer.nickName)}"></mock-input>
          <mock-input id="priorityField" ...="${field(this.binder.model.priority)}"></mock-input>
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

    it('should set name and required attributes once', async () => {
      expect(orderViewWithInput.customerFullNameField!.setAttributeSpy).to.be.calledTwice;
      assert.deepEqual(orderViewWithInput.customerFullNameField!.setAttributeSpy.getCall(0).args, [
        'name',
        'customer.fullName',
      ]);
      assert.deepEqual(orderViewWithInput.customerFullNameField!.setAttributeSpy.getCall(1).args, ['required', '']);
      expect(orderViewWithInput.customerNickNameField!.setAttributeSpy).to.be.calledOnce;
      assert.deepEqual(orderViewWithInput.customerNickNameField!.setAttributeSpy.getCall(0).args, [
        'name',
        'customer.nickName',
      ]);

      orderViewWithInput.binder.for(orderViewWithInput.binder.model.customer.fullName).value = 'foo';
      await orderViewWithInput.updateComplete;
      orderViewWithInput.binder.for(orderViewWithInput.binder.model.customer.nickName).value = 'bar';
      await orderViewWithInput.updateComplete;

      expect(orderViewWithInput.customerFullNameField!.setAttributeSpy).to.be.calledTwice;
      expect(orderViewWithInput.customerNickNameField!.setAttributeSpy).to.be.calledOnce;
    });

    describe('number model', () => {
      let view: OrderViewWithInput;
      let priorityField: MockInputElement;
      let binder: Binder<Order, OrderModel>;

      beforeEach(async () => {
        view = orderViewWithInput;
        binder = view.binder;
        priorityField = view.priorityField!;
      });

      it('should set initial zero', async () => {
        expect(priorityField.value).to.equal('0');
      });

      it('should set number value from binder', async () => {
        priorityField.valueSpy.get.resetHistory();
        priorityField.valueSpy.set.resetHistory();

        binder.for(binder.model.priority).value = 1.2;
        await view.updateComplete;
        expect(priorityField.valueSpy.set).to.be.calledOnceWithExactly(1.2);
      });

      it('should update binder value on typing', async () => {
        const cases: Array<[string, number]> = [
          ['1', 1],
          ['1.', NaN], // not allowed format
          ['1.2', 1.2],
          ['', NaN],
          ['not a number', NaN],
          ['.', NaN],
          ['.1', 0.1],
          // Invalid separator
          [',', NaN],
          [',2', NaN],
          ['1,', NaN],
          ['1,2', NaN],
        ];

        for (const [inputValue, expectedNumber] of cases) {
          for (const eventName of ['input', 'change']) {
            priorityField.value = inputValue;
            priorityField.valueSpy.get.resetHistory();
            priorityField.valueSpy.set.resetHistory();
            priorityField.dispatchEvent(
              new CustomEvent(eventName, { bubbles: true, composed: true, cancelable: false })
            );
            await view.updateComplete; // eslint-disable-line no-await-in-loop

            if (Number.isNaN(expectedNumber)) {
              // NaN never equals
              expect(binder.value.priority).to.satisfy(Number.isNaN);
            } else {
              expect(binder.value.priority).to.eq(expectedNumber);
            }
            expect(priorityField.valueSpy.get).to.be.calledOnce;
            // Should not change typed value
            expect(priorityField.valueSpy.set).to.not.be.called;
            expect(priorityField.value).to.equal(inputValue);

            priorityField.valueSpy.get.resetHistory();
            priorityField.valueSpy.set.resetHistory();
          }
        }
      });
    });
  });

  describe('field/Strategy', () => {
    const div = document.createElement('div');
    let currentStrategy: FieldStrategy;
    const binder = new (class StrategySpyBinder<T, M extends AbstractModel<T>> extends Binder<T, M> {
      getFieldStrategy(elm: any): FieldStrategy {
        currentStrategy = super.getFieldStrategy(elm);
        return currentStrategy;
      }
    })(div, TestModel);

    async function resetBinderNodeValidation(binderNode: BinderNode<any, AbstractModel<any>>) {
      binderNode.validators = [];
      await binderNode.validate();
    }

    @customElement('any-vaadin-element-tag')
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    class AnyVaadinElement extends LitElement {
      static get version() {
        return '1.0';
      }

      render() {
        return html``;
      }
    }

    beforeEach(() => {
      render(nothing, div);
    });

    ['div', 'input', 'vaadin-rich-text-editor'].forEach((tag) => {
      it(`GenericFieldStrategy ${tag}`, async () => {
        /* eslint-disable lit/binding-positions, lit/no-invalid-html */
        const tagName = unsafeStatic(tag);

        const model = binder.model.fieldString;
        const binderNode = binder.for(model);
        binderNode.value = 'foo';
        await resetBinderNodeValidation(binderNode);

        const renderElement = () => {
          render(html`<${tagName} ${field(model)}></${tagName}>`, div);
          return div.firstElementChild as Element & { value?: any };
        };

        binderNode.validators = [{ message: 'any-err-msg', validate: () => false }];

        renderElement();

        expect(currentStrategy instanceof GenericFieldStrategy).to.be.true;
        expect(currentStrategy.value).to.be.equal('foo');

        await binderNode.validate();
        const element = renderElement();

        expect(element.hasAttribute('invalid')).to.be.true;
        expect(element.hasAttribute('errorMessage')).to.be.false;
      });
    });

    [
      { tag: 'input', type: 'checkbox' },
      { tag: 'input', type: 'radio' },
      { tag: 'vaadin-checkbox', type: '' },
      { tag: 'vaadin-radio-button', type: '' },
    ].forEach(({ tag, type }) => {
      it(`CheckedFieldStrategy ${tag} ${type}`, async () => {
        const tagName = unsafeStatic(tag);

        const model = binder.model.fieldBoolean;
        const binderNode = binder.for(model);

        let element;
        const renderElement = () => {
          if (type) {
            render(html`<${tagName} type="${type}" ${field(model)}></${tagName}>`, div);
          } else {
            render(html`<${tagName} ${field(model)}></${tagName}>`, div);
          }
          return div.firstElementChild as Element & { checked?: boolean };
        };

        binderNode.value = true;
        await resetBinderNodeValidation(binderNode);

        binderNode.validators = [{ message: 'any-err-msg', validate: () => false }];

        element = renderElement();

        expect(currentStrategy instanceof CheckedFieldStrategy).to.be.true;
        expect(currentStrategy.value).to.be.true;

        expect(element.checked).to.be.true;

        await binderNode.validate();
        element = renderElement();
        expect(element.hasAttribute('invalid')).to.be.true;
        expect(element.hasAttribute('errorMessage')).to.be.false;
      });
    });

    it(`SelectedFieldStrategy`, async () => {
      const model = binder.model.fieldBoolean;
      const binderNode = binder.for(model);

      let element;
      const renderElement = () => {
        render(html`<vaadin-list-box ${field(model)}></vaadin-list-box>`, div);
        return div.firstElementChild as Element & { selected?: boolean };
      };

      binderNode.value = true;
      binderNode.validators = [{ message: 'any-err-msg', validate: () => false }];

      element = renderElement();

      expect(currentStrategy instanceof SelectedFieldStrategy).to.be.true;
      expect(currentStrategy.value).to.be.true;
      expect(element.selected).to.be.true;

      await binderNode.validate();
      element = renderElement();
      expect(element.hasAttribute('invalid')).to.be.true;
      expect(element.hasAttribute('errorMessage')).to.be.false;
    });

    [
      { model: binder.model.fieldString as AbstractModel<any>, value: 'a-string-value' },
      { model: binder.model.fieldBoolean as AbstractModel<any>, value: true },
      { model: binder.model.fieldNumber as AbstractModel<any>, value: 10 },
      { model: binder.model.fieldObject as AbstractModel<any>, value: { foo: true } },
      { model: binder.model.fieldArrayString as AbstractModel<any>, value: ['a', 'b'] },
      { model: binder.model.fieldArrayModel as AbstractModel<any>, value: [{ idString: 'id' }] },
    ].forEach(async ({ model, value }, idx) => {
      it(`VaadinFieldStrategy ${model.constructor.name} ${idx}`, async () => {
        let element;
        const renderElement = () => {
          render(html`<any-vaadin-element-tag ${field(model)}></any-vaadin-element-tag>`, div);
          const result = div.firstElementChild as Element & {
            value?: any;
            invalid?: boolean;
            required?: boolean;
            errorMessage?: string;
          };
          return result;
        };

        const binderNode = binder.for(model);

        binderNode.value = value;
        await resetBinderNodeValidation(binderNode);

        binderNode.validators = [];
        await binderNode.validate();

        binderNode.validators = [{ message: 'any-err-msg', validate: () => false }, new Required()];

        element = renderElement();

        expect(currentStrategy instanceof VaadinFieldStrategy).to.be.true;
        expect(currentStrategy.value).to.be.equal(value);
        expect(element.value).to.be.equal(value);
        expect(element.required).to.be.true;
        expect(element.invalid).to.be.undefined;
        expect(element.errorMessage).to.be.undefined;

        await binderNode.validate();
        element = renderElement();
        expect(element.invalid).to.be.true;
        expect(element.errorMessage).to.be.equal('any-err-msg');
      });
    });

    it(`Strategy can be overridden in binder`, async () => {
      const element = document.createElement('div');
      class MyStrategy extends AbstractFieldStrategy {
        invalid = true;

        required = true;
      }

      class MyBinder extends Binder<TestEntity, TestModel> {
        getFieldStrategy(elm: any): FieldStrategy {
          currentStrategy = new MyStrategy(elm);
          return currentStrategy;
        }

        constructor(elm: Element) {
          super(elm, TestModel);
        }
      }

      const binder = new MyBinder(element);
      const { model } = binder;

      render(html`<div ${field(model)}></div>`, element);
      expect(currentStrategy instanceof MyStrategy).to.be.true;
    });
  });
});
