/* tslint:disable: no-unused-expression */

const {beforeEach, suite, test} = intern.getInterface("tdd");
const {expect} = intern.getPlugin("chai");

// API to test
import {
  _key,
  ArrayModel,
  Binder,
  NotBlank,
  NotEmpty,
  NotNull,
  NumberModel,
  Positive,
  Size
} from "../../../main/resources/META-INF/resources/frontend/form";

import {IdEntity, IdEntityModel, TestEntity, TestModel} from "./TestModels";

suite("form/Model", () => {
  let binder: Binder<TestEntity, TestModel>;

  beforeEach(() => {
    binder = new Binder(document.createElement('div'), TestModel);
  });

  suite('model/requiredFlag', () => {
    test('should not be initially required', async () => {
      expect(binder.for(binder.model.fieldString).required).to.be.false;
    });

    test(`NotEmpty validator should mark a model as required`, async () => {
      binder.for(binder.model.fieldString).addValidator(new NotEmpty());
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`NotNull validator should mark a model as required`, async () => {
      binder.for(binder.model.fieldString).addValidator(new NotNull());
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`NotBlank validator should mark a model as required`, async () => {
      binder.for(binder.model.fieldString).addValidator(new NotBlank());
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`Size validator with min bigger than 0 should mark a model as required`, async () => {
      binder.for(binder.model.fieldString).addValidator(new Size({min:1}));
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`Size validator with min 0 should not be mark a model as required`, async () => {
      binder.for(binder.model.fieldString).addValidator(new Size({min:0}));
      expect(binder.for(binder.model.fieldString).required).to.be.false;
    });
  });

  suite('array model', () => {
    const strings = ['foo', 'bar'];

    const idEntities: ReadonlyArray<IdEntity> = [
      {...IdEntityModel.createEmptyValue(), idString: 'id0'},
      {...IdEntityModel.createEmptyValue(), idString: 'id1'}
    ];

    beforeEach(() => {
      binder.value = {
        ...binder.value,
        fieldArrayString: strings.slice(),
        fieldArrayModel: idEntities.slice()
      };
    });

    test('should be iterable', async () => {
      [
        binder.model.fieldArrayString,
        binder.model.fieldArrayModel
      ].forEach(arrayModel => {
        const values = binder.for(arrayModel).value;
        const iterator = arrayModel[Symbol.iterator]();
        for (let i = 0; i < values.length; i++) {
          const iteratorResult = iterator.next();
          expect(iteratorResult.done).to.be.false;
          const binderNode = iteratorResult.value;
          expect(binderNode.model[_key]).to.equal(i);
          expect(binderNode.value).to.equal(values[i]);
        }

        expect(iterator.next().done).to.be.true;
      });
    });

    test('should support prependItem on binder node', async () => {
      binder.for(binder.model.fieldArrayString).prependItem();
      binder.for(binder.model.fieldArrayString).prependItem('new');

      expect(binder.for(binder.model.fieldArrayString).value)
        .to.deep.equal(['new', '', 'foo', 'bar']);

      binder.for(binder.model.fieldArrayModel).prependItem();
      binder.for(binder.model.fieldArrayModel).prependItem({idString: 'new'});

      expect(binder.for(binder.model.fieldArrayModel).value).to.deep.equal([
        {idString: 'new'},
        {idString: ''},
        {idString: 'id0'},
        {idString: 'id1'}
      ]);
    });

    test('should support appendItem on binder node', async () => {
      binder.for(binder.model.fieldArrayString).appendItem();
      binder.for(binder.model.fieldArrayString).appendItem('new');

      expect(binder.for(binder.model.fieldArrayString).value)
        .to.deep.equal(['foo', 'bar', '', 'new']);

      binder.for(binder.model.fieldArrayModel).appendItem();
      binder.for(binder.model.fieldArrayModel).appendItem({idString: 'new'});

      expect(binder.for(binder.model.fieldArrayModel).value).to.deep.equal([
        {idString: 'id0'},
        {idString: 'id1'},
        {idString: ''},
        {idString: 'new'}
      ]);
    });

    /**
     * default value is used for checking dirty state
     * use case: adding a new order line, which contains a product 
     * and a quantity field. The product model's dirty state is
     * comparing the default value, which is getting from the parent
     * order line model, which is an array item.
    */
    test('array item defaultValue should not be undefined', async () => {
      binder.for(binder.model.fieldArrayModel).appendItem();
      const entityModels = [...binder.model.fieldArrayModel];
      expect(binder.for(entityModels[0].model).defaultValue).to.be.not.undefined;
    });

    test('should support removeSelf on binder node', async () => {
      binder.for(binder.model.fieldArrayString).appendItem();
      const stringModels = [...binder.model.fieldArrayString];
      binder.for(stringModels[1].model).removeSelf();

      expect(binder.for(binder.model.fieldArrayString).value)
        .to.deep.equal(['foo', '']);

      binder.for(binder.model.fieldArrayModel).appendItem();
      const entityModels = [...binder.model.fieldArrayModel];
      binder.for(entityModels[1].model).removeSelf();

      expect(binder.for(binder.model.fieldArrayModel).value).to.deep.equal([
        {idString: 'id0'},
        {idString: ''}
      ]);
    });

    test('should throw for prependItem on non-array binder node', async () => {
      [
        binder,
        binder.for(binder.model.fieldString),
        binder.for(binder.model.fieldBoolean),
        binder.for(binder.model.fieldNumber),
        binder.for(binder.model.fieldObject)
      ].forEach(binderNode => {
        expect(() => {
          binderNode.prependItem();
        }).to.throw('array');
      });
    });

    test('should throw for appendItem on non-array binder node', async () => {
      [
        binder,
        binder.for(binder.model.fieldString),
        binder.for(binder.model.fieldBoolean),
        binder.for(binder.model.fieldNumber),
        binder.for(binder.model.fieldObject)
      ].forEach(binderNode => {
        expect(() => {
          binderNode.appendItem();
        }).to.throw('array');
      });
    });

    test('should throw for removeSelf on non-array item binder node', async () => {
      expect(() => {
        binder.removeSelf();
      }).to.throw('array');

      Object.values(binder.model).forEach(model => {
        const binderNode = binder.for(model);
        expect(() => {
          binderNode.removeSelf();
        }).to.throw('array');
      });
    });

    test("should reuse model instance for the same array item", async () => {
      const nodes_1 = [...binder.model.fieldArrayModel].slice();
      [0, 1].forEach(i => expect(nodes_1[i].value).to.be.equal(idEntities[i]));

      binder.for(binder.model.fieldArrayModel).value = idEntities;
      const nodes_2 = [...binder.model.fieldArrayModel].slice();
      [0, 1].forEach(i => {
        expect(nodes_1[i]).to.be.equal(nodes_2[i]);
        expect(nodes_1[i].model).to.be.equal(nodes_2[i].model);
        expect(nodes_2[i].value).to.be.equal(idEntities[i]);
      });
    });

    test("should reuse model instance for the same array item after it is modified", async () => {
      const nodes_1 = [...binder.model.fieldArrayModel].slice();
      [0, 1].forEach(i => expect(nodes_1[i].value).to.be.equal(idEntities[i]));

      binder.for(nodes_1[0].model.idString).value = 'foo';
      binder.for(nodes_1[1].model.idString).value = 'bar';

      binder.for(binder.model.fieldArrayModel).value = idEntities.slice();
      binder.for(binder.model.fieldArrayModel).prependItem();
      binder.for(binder.model.fieldArrayModel).appendItem();

      const nodes_2 = [...binder.model.fieldArrayModel].slice();

      [0, 1].forEach(i => {
        expect(nodes_1[i]).to.be.equal(nodes_2[i]);
        expect(nodes_1[i].model).to.be.equal(nodes_2[i].model);
        expect(nodes_2[i + 1].value).to.be.equal(idEntities[i]);
      });
    });

    test("should update model keySymbol when inserting items", async () => {
      const nodes_1 = [...binder.model.fieldArrayModel].slice();
      [0, 1].forEach(i => expect(nodes_1[i].value).to.be.equal(idEntities[i]));

      for (let i = 0; i < nodes_1.length; i++) {
        expect(nodes_1[i].model[_key]).to.be.equal(i)
      }

      binder.for(nodes_1[0].model.idString).value = 'foo';
      expect(binder.model.fieldArrayModel.valueOf()[0].idString).to.be.equal('foo');

      binder.for(binder.model.fieldArrayModel).prependItem();
      expect(binder.model.fieldArrayModel.valueOf()[1].idString).to.be.equal('foo');

      const nodes_2 = [...binder.model.fieldArrayModel].slice();
      expect(nodes_2.length).to.be.equal(3);
      for (let i = 0; i < nodes_2.length; i++) {
        expect(nodes_2[i].model[_key]).to.be.equal(i)
      }
    });

    test("should pass variable arguments down", async () => {
      const matrix = [[0, 1], [2, 3]];
      binder.for(binder.model.fieldMatrixNumber).value = matrix;
      let walkedCells = 0;
      Array.from(binder.model.fieldMatrixNumber).forEach((rowBinder, i) => {
        expect(rowBinder.model).to.be.instanceOf(ArrayModel);
        Array.from(rowBinder.model).forEach((cellBinder, j) => {
          expect(cellBinder.model).to.be.instanceOf(NumberModel);
          expect(cellBinder.value).to.be.equal(matrix[i][j]);
          expect(cellBinder.validators[0]).to.be.instanceOf(Positive);
          walkedCells++;
        });
      });
      expect(walkedCells).to.equal(4);
    });
  });
});
