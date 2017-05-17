package me.dags.blockley.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author dags <dags@dags.me>
 */
public class StreamSource implements CharSource {

    private static final Charset UTF8 = Charset.forName("utf8");

    private final InputStreamReader reader;
    private int next = -2;

    StreamSource(InputStream inputStream) {
        this.reader = new InputStreamReader(inputStream, UTF8);
    }

    @Override
    public boolean hasNext() throws IOException {
        if (next == -2) {
            next = reader.read();
        }

        return next != -1;
    }

    @Override
    public char next() throws IOException {
        if (next != -2) {
            char c = (char) next;
            next = -2;
            return c;
        }
        return (char) reader.read();
    }

    @Override
    public void close() throws IOException {
       // reader.close();
    }
}
