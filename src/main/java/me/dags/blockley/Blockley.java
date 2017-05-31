package me.dags.blockley;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import me.dags.blockley.template.Context;
import me.dags.blockley.template.Template;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author dags <dags@dags.me>
 */
@Mod(modid = "blockley", name = "Blockley", version = "1.0.3", clientSideOnly = true)
public class Blockley {

    private static final KeyBinding show = new KeyBinding("blockley.show", Keyboard.KEY_B, "Blockley");

    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static final Runnable EMPTY = () -> {};
    private static final Set<String> propertyFilter = Sets.newHashSet(
            "age", "color", "eye", "half", "layers", "mode", "snowy", "type", "variant", "wet"
    );

    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    private File index = new File("");
    private File images = new File("");
    private final AtomicReference<Runnable> task = new AtomicReference<>(EMPTY);

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        File root = event.getModConfigurationDirectory();
        File data = new File(root, "blockley");
        index = new File(data, "index.html");
        images = new File(data, "images");
        MinecraftForge.EVENT_BUS.register(this);
        ClientRegistry.registerKeyBinding(show);
    }

    @SubscribeEvent
    public void tick(TickEvent.RenderTickEvent event) {
        Runnable runnable = task.get();

        if (runnable != EMPTY) {
            task.set(EMPTY);
            runnable.run();
            return;
        } 

        if (Minecraft.getMinecraft().inGameHasFocus && show.isPressed()) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || !index.exists()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString("Generating block list..."));
                task.set(this::createIndex);
            } else {
                task.set(this::show);
            }
        }
    }

    private void show() {
        if (index.exists()) {
            try {
                Desktop.getDesktop().open(index);
            } catch (IOException ignored) {

            }
        }
    }

    private void createIndex() {
        List<BlockInfo> items = getAllItems();

        try (FileWriter writer = new FileWriter(ensure(index))) {
            try (InputStream inputStream = Blockley.class.getResourceAsStream("/template.html")) {
                Context context = getContext(items);
                Template template = Template.compile("template", inputStream);
                template.render(context, writer);
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, BufferedImage> icons = getIcons(items);

        new Thread(() -> {
            for (Map.Entry<String, BufferedImage> entry : icons.entrySet()) {
                try {
                    File file = ensure(images, entry.getKey() + ".png");
                    ImageIO.write(entry.getValue(), "png", file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            task.set(this::show);
        }).start();
    }

    private static List<BlockInfo> getAllItems() {
        List<BlockInfo> list = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        for (Block block : Block.REGISTRY) {
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }

            List<ItemStack> items = new LinkedList<>();
            block.getSubBlocks(item, CreativeTabs.SEARCH, items);
            for (ItemStack stack : items) {
                IBlockState state = block.getStateFromMeta(stack.getMetadata());
                BlockInfo info = new BlockInfo(stack, state);
                if (visited.add(info.identifier)) {
                    list.add(info);
                }
            }
        }

        return list;
    }

    private static ImmutableMap<String, BufferedImage> getIcons(List<BlockInfo> list) {
        ImmutableMap.Builder<String, BufferedImage> builder = ImmutableMap.builder();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        for (BlockInfo info : list) {
            BufferedImage image = createImage(info.stack, resolution);
            builder.put(info.identifier, image);
        }
        return builder.build();
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

    private static BufferedImage createImage(ItemStack stack, ScaledResolution resolution) {
        int width = WIDTH;
        int height = HEIGHT;

        float scaleW = (width / 16F) * (resolution.getScaledWidth() / (float) width);
        float scaleH = (height / 16F) * (resolution.getScaledHeight() / (float) height);

        Framebuffer framebuffer = new Framebuffer(width, height, true);
        framebuffer.bindFramebuffer(true);

        RenderHelper.enableStandardItemLighting();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 250);
        GlStateManager.scale(scaleW, scaleH, 1F);
        GlStateManager.alphaFunc(GL11.GL_NOTEQUAL, 0);

        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, 0, 0);

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();

        BufferedImage image = createImage(width, height);
        framebuffer.unbindFramebuffer();
        framebuffer.deleteFramebuffer();

        return image;
    }

    // from render helper
    private static BufferedImage createImage(int width, int height) {
        int i = width * height;

        if (pixelBuffer == null || pixelBuffer.capacity() < i) {
            pixelBuffer = BufferUtils.createIntBuffer(i);
            pixelValues = new int[i];
        }

        GlStateManager.glPixelStorei(3333, 1);
        GlStateManager.glPixelStorei(3317, 1);
        pixelBuffer.clear();

        GlStateManager.glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);

        pixelBuffer.get(pixelValues);
        TextureUtil.processPixelValues(pixelValues, width, height);

        BufferedImage bufferedimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferedimage.setRGB(0, 0, width, height, pixelValues, 0, width);

        return bufferedimage;
    }

    private static File ensure(File parent, String name) {
        return ensure(new File(parent, name));
    }

    private static File ensure(File file) {
        file.getParentFile().mkdirs();
        return file;
    }

    private static class BlockInfo {

        private final ItemStack stack;
        private final String name;
        private final String state;
        private final String identifier;
        private final int id;
        private final int meta;

        private BlockInfo(ItemStack stack, IBlockState state) {
            this.stack = stack;
            this.name = stack.getDisplayName();
            this.state = simpleState(state);
            this.identifier = safeId(this.state);
            this.id = Block.getIdFromBlock(state.getBlock());
            this.meta = state.getBlock().getMetaFromState(state);
        }

        private static String simpleState(IBlockState state) {
            StringBuilder sb = new StringBuilder(state.toString().length());
            sb.append(state.getBlock().getRegistryName());

            boolean extended = false;

            for (Map.Entry<IProperty<?>, ?> property : state.getProperties().entrySet()) {
                String name = property.getKey().getName();
                if (propertyFilter.contains(name)) {
                    sb.append(extended ? ',' : '[');
                    sb.append(name).append('=').append(property.getValue());
                    extended = true;
                }
            }

            if (extended) {
                sb.append(']');
            }

            return sb.toString();
        }

        private static String safeId(Object in) {
            String id = in.toString().replaceAll("[^A-Za-z0-9=]", "_");
            return id.endsWith("_") ? id.substring(0, id.length() - 1) : id;
        }
    }
}
