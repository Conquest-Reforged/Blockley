package me.dags.blockley.template;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author dags <dags@dags.me>
 */
public class Context {

    private final String name;
    private final Context parent;
    private final Object contents;
    private final List<Context> children = new LinkedList<>();
    private boolean added = false;

    private Context(Context parent, String name, Object contents) {
        this.name = name;
        this.parent = parent;
        this.contents = contents;
    }

    public Context list(String name) {
        Context context = new Context(this, name, new LinkedList<>());
        children.add(context);
        return context;
    }

    public Context map(String name) {
        Context context = new Context(this, name, new HashMap<>());
        children.add(context);
        return context;
    }

    public Context add(Object value) {
        if (value instanceof Context) {
            Context context = (Context) value;
            if (contents instanceof List) {
                ((List) contents).add(context.contents);
            }
            if (contents instanceof Map) {
                ((Map) contents).put(context.name, context.contents);
            }
        } else {
            if (contents instanceof List) {
                ((List) contents).add(value);
            }
        }
        return this;
    }

    public Context put(String key, Object value) {
        if (contents instanceof Map) {
            ((Map) contents).put(key, value);
        }
        return this;
    }

    public Context getParent() {
        if (parent == null) {
            return this;
        }
        return parent;
    }

    public Context getRoot() {
        Context root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }

    Object getContext() {
        Context root = getRoot();
        root.submit();
        return root.contents;
    }

    private void submit() {
        if (added) {
            return;
        }

        for (Context child : children) {
            child.submit();
        }

        if (parent != null) {
            parent.add(this);
        }

        added = true;
    }

    public static Context root() {
        return new Context(null, "root", new HashMap<>());
    }
}
