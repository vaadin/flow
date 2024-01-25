function MyComp() {
    return <div data-expected="1_19">A component defined in a function</div>;
  }
  export function MyCompExport() {
    return (
      <>
        <div data-expected="4_34">A component defined in an exported function</div>
      </>
    );
  }
  const MyCompConst = () => <div data-expected="11_29">A component defined in a const</div>;
  export const MyCompConstExport = () => <div data-expected="12_42">A component defined in an exported const</div>;
  
  const NotAComp = undefined;
  
  export default function InProjectComponentView() {
    function Inner() {
      return <div data-expected="17_22">A component defined using a function inside another component</div>;
    }
    const InnerConst = () => <div data-expected="20_30">A component defined using a const inside another component</div>;
  
    return (
      <div data-expected="16_52">
        default
        <Inner></Inner>
        <InnerConst />
        <MyComp />
        <MyCompExport></MyCompExport>
        <MyCompConstExport></MyCompConstExport>
        <MyCompConst />
      </div>
    );
  }
  