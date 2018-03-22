package me.dags.blockley;

import com.google.gson.stream.JsonWriter;
import net.minecraft.client.gui.ScaledResolution;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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

    private static final int PER_TICK = 3;

    private final List<BlockInfo> blocks;
    private final LinkedList<BlockInfo> queue;
    private final ScaledResolution resolution;
    private final File images;
    private final File baseDir;
    private final ExecutorService executor;
    private final List<Future> tasks = new LinkedList<>();

    private boolean indexExported = false;

    private ExportTask() {
        baseDir = null;
        blocks = null;
        queue = null;
        resolution = null;
        images = null;
        executor = null;
    }

    public ExportTask(List<BlockInfo> blocks, ScaledResolution resolution, File baseDir) {
        this.blocks = blocks;
        this.baseDir = baseDir;
        this.resolution = resolution;
        this.queue = new LinkedList<>(blocks);
        this.images = new File(baseDir, "images");
        this.executor = Executors.newCachedThreadPool();
    }

    public boolean isDone() {
        return queue.isEmpty() && tasks.isEmpty() && indexExported;
    }

    public void tick() {
        if (!queue.isEmpty()) {
            drainQueue();
        } else if (!indexExported) {
            exportData();
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

    private void exportData() {
        copyResource(new File(baseDir, "index.html"));
        copyResource(new File(baseDir, "style/style.css"));
        copyResource(new File(baseDir, "script/page.js"));

        File data = new File(baseDir, "script/data.js");
        try (FileWriter writer = new FileWriter(Utils.ensure(data))) {
            StringWriter stringWriter = new StringWriter();
            try (JsonWriter jsonWriter = new JsonWriter(stringWriter)) {
                jsonWriter.beginArray();
                for (BlockInfo info : blocks) {
                    jsonWriter.beginObject();
                    jsonWriter.name("name").value(info.name);
                    jsonWriter.name("id").value(info.id + ":" + info.meta);
                    jsonWriter.name("state").value(info.state);
                    jsonWriter.name("identifier").value(info.identifier);
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
            writer.write("const data = `");
            writer.write(stringWriter.toString());
            writer.write("`;");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyResource(File file) {
        try (InputStream inputStream = Blockley.class.getResourceAsStream("/" + file.getName())) {
            try (FileWriter writer = new FileWriter(Utils.ensure(file))) {
                IOUtils.copy(inputStream, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
