package com.here.account.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

public class CloseUtil {

    /**
     * A null-safe invocation of closeable.close(), such that if an IOException is
     * triggered, it is wrapped instead in an UncheckedIOException.
     *
     * @param closeable the closeable to be closed
     */
    public static void nullSafeCloseThrowingUnchecked(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

    }
}
