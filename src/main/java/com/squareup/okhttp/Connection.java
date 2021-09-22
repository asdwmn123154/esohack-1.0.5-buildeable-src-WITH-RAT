package com.squareup.okhttp;

import java.net.Socket;

public interface Connection {
   Route getRoute();

   Socket getSocket();

   Handshake getHandshake();

   Protocol getProtocol();
}
