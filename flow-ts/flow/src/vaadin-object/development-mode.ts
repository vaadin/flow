declare global {
  interface Vaadin {
    developmentMode: boolean;
  }
}

const part: Pick<Vaadin, 'developmentMode'> = {
  developmentMode: false
};

export default part;
