/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.data.converter;

import java.util.UUID;

import com.vaadin.flow.data.binder.ErrorMessageProvider;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link String} to {@link UUID} and back. <br/>
 * Empty strings are converted to <code>null</code>.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class StringToUuidConverter implements Converter<String, UUID> {

	private ErrorMessageProvider errorMessageProvider;

	/**
	 * Creates converter to convert from String to UUID and back
	 *
	 * @param errorMessage 
	 *            the error message to use if conversion fails
	 */
	public StringToUuidConverter(String errorMessage) {
		this(context -> errorMessage);
	}

	/**
	 * Creates a new converter instance with the given error message provider. 
	 *
	 * @param errorMessageProvider 
	 *            the error message provider to use if conversion
	 *                             fails
	 */
	public StringToUuidConverter(ErrorMessageProvider errorMessageProvider) {
		this.errorMessageProvider = errorMessageProvider;
	}

	@Override
	public Result<UUID> convertToModel(String value, ValueContext context) {

		if (isNullOrEmptyString(value)) {
			return Result.ok(null);
		}
		
		try {
			value = value.trim();
			final UUID uuid = UUID.fromString(value);
			return Result.ok(uuid);
		} catch (IllegalArgumentException e) {
			return Result.error(this.errorMessageProvider.apply(context));
		}
	}
	
	private boolean isNullOrEmptyString(String value) {
		return value == null || value.isEmpty();
	}

	@Override
	public String convertToPresentation(UUID uuid, ValueContext context) {

		if (uuid == null) {
			return null;
		}

		return uuid.toString();
	}

}
