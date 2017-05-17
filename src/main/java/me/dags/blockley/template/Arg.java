package me.dags.blockley.template;

import java.io.IOException;

/**
 * @author dags <dags@dags.me>
 */
public class Arg implements Element {

    private String name;

    public Arg(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String render(Object context) {
        if (context != null) {
            return context.toString();
        }
        return "";
    }

    @Override
    public void render(Object context, Appendable appendable) throws IOException {
        appendable.append(render(context));
    }

    @Override
    public String toString() {
        return getName();
    }
}
