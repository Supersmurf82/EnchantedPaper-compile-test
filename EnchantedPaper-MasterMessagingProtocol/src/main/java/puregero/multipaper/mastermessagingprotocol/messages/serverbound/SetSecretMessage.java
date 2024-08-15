package puregero.multipaper.mastermessagingprotocol.messages.serverbound;

import puregero.multipaper.mastermessagingprotocol.ExtendedByteBuf;

public class SetSecretMessage extends ServerBoundMessage {

    public final String secret;

    public SetSecretMessage(String secret) {
        this.secret = secret;
    }

    public SetSecretMessage(ExtendedByteBuf byteBuf) {
        secret = byteBuf.readString();
    }

    @Override
    public void write(ExtendedByteBuf byteBuf) {
        byteBuf.writeString(secret);
    }

    @Override
    public void handle(ServerBoundMessageHandler handler) {
        handler.handle(this);
    }
}
