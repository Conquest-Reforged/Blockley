package me.dags.blockley.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author dags <dags@dags.me>
 */
public class Template implements Element {

    private final String name;
    private final List<Element> elements;

    Template(String name, List<Element> elements) {
        this.name = name;
        this.elements = Collections.unmodifiableList(elements);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String render(Object context) throws IOException {
        StringBuilder builder = new StringBuilder();
        render(context, builder);
        return builder.toString();
    }

    @Override
    public void render(Object context, Appendable appendable) throws IOException {
        if (context instanceof Map) {
            Map map = (Map) context;
            for (Element e : elements) {
                Object o = map.get(e.getName());
                e.render(o, appendable);
            }
        } else if (context instanceof List) {
            List list = (List) context;
            for (Object o : list) {
                render(o, appendable);
            }
        } else {
            for (Element e : elements) {
                e.render(context, appendable);
            }
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public static Template compile(String name, String raw) {
        return new Parser(new StringSource(raw)).parse(name);
    }

    public static Template compile(String name, InputStream inputStream) {
        return new Parser(new StreamSource(inputStream)).parse(name);
    }
}
