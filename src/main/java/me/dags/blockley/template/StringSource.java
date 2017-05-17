package me.dags.blockley.template;

import java.io.IOException;

/**
 * @author dags <dags@dags.me>
 */
public class StringSource implements CharSource {

    private final String input;
    private int pos = -1;

    public StringSource(String in) {
        this.input = in;
    }

    @Override
    public boolean hasNext() {
        return pos + 1 < input.length();
    }

    @Override
    public char next() {
        return input.charAt(++pos);
    }

    @Override
    public void close() throws IOException {

    }
}
