package com.hify.common.http;

public interface StreamCallback {

    void onLine(String line);

    default void onComplete() {}

    default void onError(Throwable t) {}
}
