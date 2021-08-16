package com.vaadin.tests.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import junit.framework.TestCase;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class SerializationTest extends TestCase {

    public void testSerializeVaadinSession_accessQueueIsRecreated()
            throws Exception {
        VaadinSession session = mockSession(true);

        session = serializeAndDeserialize(session);

        assertNotNull(
                "Pending access queue was not recreated after deserialization",
                session.getPendingAccessQueue());
    }

    public void testSerializeVaadinSession_notProductionMode_disableDevModeSerialization_deserializedSessionHasLockInstance()
            throws Exception {
        VaadinSession session = mockSession(false);

        session = serializeAndDeserialize(session);

        assertNotNull("Lock should be available after empty deserialization",
                session.getLockInstance());
        // self check : session is not really deserialized so the transient
        // queue should be null
        assertNull(session.getPendingAccessQueue());
    }

    private VaadinSession mockSession(boolean productionMode) {
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinContext context = Mockito.mock(VaadinContext.class);
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Mockito.any(), Mockito.any()))
                .thenReturn(applicationConfiguration);
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(productionMode);
        Mockito.when(
                applicationConfiguration.isDevModeSessionSerializationEnabled())
                .thenReturn(false);
        VaadinSession session = new VaadinSession(service);
        return session;
    }

    private static <S extends Serializable> S serializeAndDeserialize(S s)
            throws IOException, ClassNotFoundException {
        // Serialize and deserialize

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(s);
        byte[] data = bs.toByteArray();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));
        @SuppressWarnings("unchecked")
        S s2 = (S) in.readObject();

        // using special toString(Object) method to avoid calling
        // Property.toString(), which will be temporarily disabled
        // TODO This is hilariously broken (#12723)
        if (s.equals(s2)) {
            System.out.println(toString(s) + " equals " + toString(s2));
        } else {
            System.out.println(toString(s) + " does NOT equal " + toString(s2));
        }

        return s2;
    }

    private static String toString(Object o) {
        return String.valueOf(o);
    }

    public static class Data implements Serializable {
        private String dummyGetter;
        private String dummyGetterAndSetter;
        private int dummyInt;

        public String getDummyGetterAndSetter() {
            return dummyGetterAndSetter;
        }

        public void setDummyGetterAndSetter(String dummyGetterAndSetter) {
            this.dummyGetterAndSetter = dummyGetterAndSetter;
        }

        public int getDummyInt() {
            return dummyInt;
        }

        public void setDummyInt(int dummyInt) {
            this.dummyInt = dummyInt;
        }

        public String getDummyGetter() {
            return dummyGetter;
        }
    }
}
