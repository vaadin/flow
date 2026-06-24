/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
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
