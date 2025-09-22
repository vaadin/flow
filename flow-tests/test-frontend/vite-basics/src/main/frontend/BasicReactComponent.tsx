
export function BasicReactComponent(props: Readonly<{ name?: string }>){
    return <div data-test-id="basic-react-component">{props.name ?? 'Unknown'}</div>
}