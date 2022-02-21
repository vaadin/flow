package dev.hilla.push.messages.toclient;

public class ClientMessageComplete extends AbstractClientMessage {
    public ClientMessageComplete() {
    }

    public ClientMessageComplete(String id) {
        super(id);
    }

    public boolean isDone() {
        return true;
    }

    @Override
    public String toString() {
        return "ClientMessageComplete  [id=" + getId() + "]";
    }

}
