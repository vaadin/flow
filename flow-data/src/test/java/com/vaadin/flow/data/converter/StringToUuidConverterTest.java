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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * Unit test for {@link StringToUuidConverter}.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class StringToUuidConverterTest {
	
	
	@Test
	public void should_BeSuccessfull_When_InputValueIsEmpty() {
		
		final StringToUuidConverter converter = new StringToUuidConverter("exception when converting from value to uuid");
		final Result<UUID> result = converter.convertToModel("", new ValueContext());
		assertFalse(result.isError());
	}
	
	@Test
	public void should_BeSuccessfull_When_InputValueIsNull() {
		
		final StringToUuidConverter converter = new StringToUuidConverter("exception when converting from value to uuid");
		final Result<UUID> result = converter.convertToModel("", new ValueContext());
		assertFalse(result.isError());
	}
	
	@Test
	public void should_ReturnError_When_InputValueIsNotAValidUUID() {
		
		final StringToUuidConverter converter = new StringToUuidConverter("exception when converting from value to uuid");
		final Result<UUID> result = converter.convertToModel("123e4567-e89b", new ValueContext());
		assertTrue(result.isError());
	}
	
	@Test
	public void should_BeSuccessfull_When_InputValueIsAValidUUID() {
		
		final StringToUuidConverter converter = new StringToUuidConverter("exception when converting from value to uuid");
		final Result<UUID> result = converter.convertToModel("123e4567-e89b-12d3-a456-556642440000", new ValueContext());
		assertFalse(result.isError());
	}

}
