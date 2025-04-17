import { ReactAdapterElement, RenderHooks } from 'Frontend/generated/flow/ReactAdapter.js';
import {ChangeEvent, useCallback} from 'react';

class ReactInput extends ReactAdapterElement {
    protected override render(hooks: RenderHooks) {
        const [value, setValue] = hooks.useState<string>('value');
        const changeListener = useCallback((event: ChangeEvent<HTMLInputElement>) => {
            setValue(event.target.value)
        }, [setValue]);
        return <input value={value} onChange={changeListener} />;
    }
}

customElements.define('react-input', ReactInput);
