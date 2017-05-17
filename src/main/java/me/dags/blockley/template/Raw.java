package me.dags.blockley.template;

import java.io.IOException;

/**
 * @author dags <dags@dags.me>
 */
public class Raw implements Element {

    private final String raw;

    public Raw(String raw) {
        this.raw = raw;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String render(Object context) {
        return raw;
    }

    @Override
    public void render(Object context, Appendable appendable) throws IOException {
        appendable.append(raw);
    }

    @Override
    public String toString() {
        return "html";
    }
}
