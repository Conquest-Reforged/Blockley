package me.dags.blockley;

import me.dags.blockley.template.Context;
import me.dags.blockley.template.Template;
import net.minecraft.client.gui.ScaledResolution;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author dags <dags@dags.me>
 */
public class ExportTask {

    static final ExportTask EMPTY = new ExportTask() {
        @Override
        public void tick() {}

        @Override
        public boolean isDone() {
            return true;
        }
    };

    private static final int PER_TICK = 5;

    private final List<BlockInfo> blocks;
    private final LinkedList<BlockInfo> queue;
    private final ScaledResolution resolution;
    private final File images;
    private final File index;
    private final ExecutorService executor;
    private final List<Future> tasks = new LinkedList<>();

    private boolean indexExported = false;

    private ExportTask() {
        this.index = null;
        blocks = null;
        queue = null;
        resolution = null;
        images = null;
        executor = null;
    }

    public ExportTask(List<BlockInfo> blocks, ScaledResolution resolution, File index, File images) {
        this.blocks = blocks;
        this.queue = new LinkedList<>(blocks);
        this.resolution = resolution;
        this.images = images;
        this.index = index;
        this.executor = Executors.newCachedThreadPool();
    }

    public boolean isDone() {
        return queue.isEmpty() && tasks.isEmpty() && indexExported;
    }

    public void tick() {
        if (!queue.isEmpty()) {
            drainQueue();
        } else if (!indexExported) {
            exportIndex();
            indexExported = true;
        } else {
            tasks.removeIf(future -> future.isDone() || future.isCancelled());
        }

        if (isDone() && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void drainQueue() {
        for (int i = 0; i < PER_TICK && !blocks.isEmpty(); i++) {
            BlockInfo info = queue.poll();
            if (info == null) {
                continue;
            }

            String name = info.identifier;
            BufferedImage img = Utils.createImage(info.stack, resolution);
            tasks.add(executor.submit(() -> {
                try {
                    File file = Utils.ensure(images, name + ".png");
                    ImageIO.write(img, "png", file);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }));
        }
    }

    private void exportIndex() {
        try (FileWriter writer = new FileWriter(Utils.ensure(index))) {
            try (InputStream inputStream = Blockley.class.getResourceAsStream("/template.html")) {
                Context context = getContext(blocks);
                Template template = Template.compile("template", inputStream);
                template.render(context, writer);
                writer.flush();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static Context getContext(List<BlockInfo> list) {
        Context context = Context.root().list("content");
        for (BlockInfo info : list) {
            context.map(info.identifier)
                    .put("name", info.name)
                    .put("identifier", info.identifier)
                    .put("state", info.state)
                    .put("id", info.id)
                    .put("meta", info.meta);
        }
        return context.getRoot();
    }
}
