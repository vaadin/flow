

export default function ReactComponentProperties(){
    return (
        <div data-test-id="react-component-properties">
            <ComponentWithInlineProps  name={'fooName'} surname={'fooSurname'} />
            <ComponentWithoutProps></ComponentWithoutProps>
            <h1 data-test-id="simple-header">Header 1</h1>
            <ComponentWithInterfaceProps label={"fooLabel"}></ComponentWithInterfaceProps>
            <ComponentWithAnyProps />
        </div>
    )
}

const ComponentWithoutProps = () => {
    return <span data-test-id="component-without-props"></span>
}
const ComponentWithInlineProps = (props: { name: string, surname: string, sex?: 'M' | 'F'}) => {
    const { name, surname, sex } = props;
    return (
        <div data-test-id="component-with-inline-props">
            <h4>{name} - ${surname}</h4>
            <h5>{sex ?? 'Unknown'}</h5>
        </div>
    )
}
interface ComponentWithInterfaceProperties {
    label: string;
}
const ComponentWithInterfaceProps = (props: ComponentWithInterfaceProperties) => {
    return (
        <div data-test-id="component-with-interface-props">
            {props.label}
        </div>
    )
}

const ComponentWithAnyProps = (props: any) => {
    return (
        <h5 data-test-id="component-with-any-props">This component has any typed props</h5>
    )
}