package me.dags.blockley.template;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author dags <dags@dags.me>
 */
public class Parser {

    private final CharSource charStream;
    private final List<Element> elements = new LinkedList<>();

    private StringBuilder builder = new StringBuilder();

    Parser(CharSource source) {
        this.charStream = source;
    }

    Template parse(String name) {
        try {
            while (charStream.hasNext()) {
                char c = charStream.next();

                if (c == '{' && charStream.hasNext()) {
                    c = charStream.next();
                    if (c == '{') {
                        flush();
                        parseElement();
                    } else {
                        builder.append('{').append(c);
                    }
                } else {
                    builder.append(c);
                }
            }

            flush();

            return new Template(name, elements);
        } catch (IOException e) {
            e.printStackTrace();
            return new Template("", Collections.emptyList());
        }
    }

    private void parseElement() throws IOException {
        char c = charStream.next();
        StringBuilder builder = new StringBuilder();

        if (c == '#') {
            String name = parseName(builder);
            if (name == null) {
                return;
            }
            parseTemplate(name);
        } else {
            String name = parseName(builder.append(c));
            if (name != null) {
                elements.add(new Arg(name));
            }
        }
    }

    private String parseName(StringBuilder name) throws IOException {
        while (charStream.hasNext()) {
            char c = charStream.next();

            if (c == '}') {
                if (charStream.hasNext()) {
                    c = charStream.next();

                    if (c == '}') {
                        return name.toString();
                    }

                    name.append('}');
                }
            }

            name.append(c);
        }

        builder.append(name);

        return null;
    }

    private void parseTemplate(String name) throws IOException {
        StringBuilder template = new StringBuilder();

        String match = "{{/" + name + "}}";
        int matchLength = match.length();
        int matchPos = 0;

        while (charStream.hasNext()) {
            if (matchPos == matchLength) {
                int start = template.length() - matchLength;
                int end = template.length();
                String trimmed = template.delete(start, end).toString();
                elements.add(Template.compile(name, trimmed));
                return;
            }

            char c = charStream.next();

            if (c == match.charAt(matchPos)) {
                matchPos++;
            } else {
                matchPos = 0;
            }

            template.append(c);
        }

        builder.append(template);
    }

    private void flush() {
        if (builder.length() > 0) {
            Raw raw = new Raw(builder.toString());
            elements.add(raw);
            builder = new StringBuilder();
        }
    }
}
