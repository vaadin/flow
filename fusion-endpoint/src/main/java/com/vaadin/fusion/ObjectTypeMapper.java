package com.vaadin.fusion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectTypeMapper {

    public interface Mapper<ENDPOINT_TYPE, TRANSFER_TYPE> {
        public Class<? extends ENDPOINT_TYPE> getEndpointType();

        public Class<? extends TRANSFER_TYPE> getTransferType();

        public TRANSFER_TYPE toTransferType(ENDPOINT_TYPE endpointType);

        public ENDPOINT_TYPE toEndpointType(TRANSFER_TYPE transferType);

    }

    private Map<Class<?>, Class<?>> endpointToTransfer = new HashMap<>();

    private Map<Class<?>, Mapper<?, ?>> mappers = new HashMap<>();
    {
        registerMapper(new PageableMapper());
        registerMapper(new UUIDMapper());
        registerMapper(new PageMapper());
    }

    public String getTransferType(String cls) {
        for (Class<?> key : endpointToTransfer.keySet()) {
            if (key.getName().equals(cls)) {
                return endpointToTransfer.get(key).getName();
            }
        }
        return cls;
    }

    private void registerMapper(Mapper<?, ?> mapper) {
        Class<?> endpointType = mapper.getEndpointType();
        endpointToTransfer.put(endpointType, mapper.getTransferType());
        mappers.put(endpointType, mapper);
    }

    public Class<?> getTransferType(Class<?> cls) {
        return endpointToTransfer.getOrDefault(cls, cls);
    }

    public Class<?> getEndpointType(Class<?> cls) {
        for (Entry<Class<?>, Class<?>> entry : endpointToTransfer.entrySet()) {
            if (entry.getValue() == cls) {
                return entry.getKey();
            }
        }
        return cls;
    }

    public Object toTransferType(Object value) {
        if (value == null) {
            return value;
        }

        Class<? extends Object> valueType = value.getClass();

        Class<?> transferType = getTransferType(valueType);
        if (transferType == valueType) {
            return value;
        }

        getLogger().info("Mapping from endpoint type ("
                + value.getClass().getSimpleName() + ") to transfer type ("
                + transferType.getSimpleName() + ")");

        Mapper mapper = (Mapper) mappers.get(valueType);
        if (mapper == null) {
            throw new IllegalStateException("The type " + valueType.getName()
                    + " should be converted but is not");
        }

        return mapper.toTransferType(value);
    }

    public Object toEndpointType(Object value, Class endpointType) {
        if (value == null) {
            return value;
        }

        Class<? extends Object> valueType = value.getClass();
        if (endpointType == valueType) {
            return value;
        }

        getLogger().info("Mapping from transfer type ("
                + value.getClass().getSimpleName() + ") to endpoint type ("
                + endpointType.getSimpleName() + ")");
        Mapper mapper = (Mapper) mappers.get(endpointType);
        if (mapper == null) {
            throw new IllegalStateException("The type " + valueType.getName()
                    + " should be converted but is not");
        }

        return mapper.toEndpointType(value);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}
