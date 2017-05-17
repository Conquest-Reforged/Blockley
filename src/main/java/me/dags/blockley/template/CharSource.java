package me.dags.blockley.template;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author dags <dags@dags.me>
 */
public interface CharSource extends Closeable {

    boolean hasNext() throws IOException;

    char next() throws IOException;
}
