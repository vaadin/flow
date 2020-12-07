const {describe, it} = intern.getPlugin('interface.bdd');
const {assert} = intern.getPlugin("chai");

describe('DeferrableEndpoint', () => {
  it('import should add user statistics', async () => {
    await import('../../main/resources/META-INF/resources/frontend/DeferrableEndpoint');
    const registrations = ((window as any).Vaadin.registrations as Array<any>);
    assert.exists(registrations);
    assert.isAbove(registrations.filter(obj =>
      obj['is'] === '@vaadin/@Deferrable'
    ).length, 0);
  });
});