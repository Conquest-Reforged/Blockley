package me.dags.blockley.template;

import java.io.IOException;

/**
 * @author dags <dags@dags.me>
 */
public interface Element {

    String getName();

    default String render(Context context) throws IOException {
        return render(context.getRoot().getContext());
    }

    default void render(Context context, Appendable appendable) throws IOException {
        render(context.getRoot().getContext(), appendable);
    }

    String render(Object context) throws IOException;

    void render(Object context, Appendable appendable) throws IOException;
}
