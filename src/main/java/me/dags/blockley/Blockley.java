package me.dags.blockley;

import com.google.common.collect.ImmutableMap;
import me.dags.blockley.template.Context;
import me.dags.blockley.template.Template;
import net.minecraft.block.Block;
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

/**
 * @author dags <dags@dags.me>
 */
@Mod(modid = "blockley", name = "Blockley", version = "1.0.1", clientSideOnly = true)
public class Blockley {

    private static final KeyBinding show = new KeyBinding("blockley.show", Keyboard.KEY_B, "Blockley");

    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;

    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    private File index = new File("");
    private File images = new File("");

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
        if (Minecraft.getMinecraft().inGameHasFocus && show.isPressed()) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || !index.exists()) {
                createIndex(this::show);
            } else {
                show();
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

    private void createIndex(Runnable callback) {
        List<ItemStack> items = getAllItems();

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
            callback.run();
        }).start();
    }

    private static List<ItemStack> getAllItems() {
        List<ItemStack> list = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        for (Block block : Block.REGISTRY) {
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }

            List<ItemStack> items = new LinkedList<>();
            block.getSubBlocks(item, CreativeTabs.SEARCH, items);
            for (ItemStack stack : items) {
                if (visited.add(stack.getUnlocalizedName())) {
                    list.add(stack);
                }
            }
        }

        return list;
    }

    private static ImmutableMap<String, BufferedImage> getIcons(List<ItemStack> list) {
        ImmutableMap.Builder<String, BufferedImage> builder = ImmutableMap.builder();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        for (ItemStack stack : list) {
            BufferedImage image = createImage(stack, resolution);
            builder.put(stack.getUnlocalizedName(), image);
        }
        return builder.build();
    }

    private static Context getContext(List<ItemStack> list) {
        Context context = Context.root().list("content");
        for (ItemStack stack : list) {
            Block block = Block.getBlockFromItem(stack.getItem());

            String name = stack.getDisplayName();
            String identifier = stack.getUnlocalizedName();
            int id = Block.getIdFromBlock(block);
            int meta = stack.getMetadata();

            context.map(identifier)
                    .put("name", name)
                    .put("identifier", identifier)
                    .put("id", id)
                    .put("meta", meta);
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
        GlStateManager.translate(0, 0, 100);
        GlStateManager.scale(scaleW, scaleH, 1F);
        GlStateManager.alphaFunc(GL11.GL_NOTEQUAL, 0);

        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, 0,0);

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
}
