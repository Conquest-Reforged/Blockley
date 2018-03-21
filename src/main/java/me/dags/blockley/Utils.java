package me.dags.blockley;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;

/**
 * @author dags <dags@dags.me>
 */
public class Utils {

    private static final float LIGHT = 0.6F;
    private static final float ALPHA = 1F;
    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    public static BufferedImage createImage(ItemStack stack, ScaledResolution resolution) {
        int width = WIDTH;
        int height = HEIGHT;

        float scaleW = (width / 16F) * (resolution.getScaledWidth() / (float) width);
        float scaleH = (height / 16F) * (resolution.getScaledHeight() / (float) height);

        Framebuffer framebuffer = new Framebuffer(width, height, true);
        framebuffer.bindFramebuffer(true);

        RenderHelper.enableStandardItemLighting();
        GlStateManager.glLightModel(2899, RenderHelper.setColorBuffer(LIGHT, LIGHT, LIGHT, ALPHA));

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 0);
        GlStateManager.scale(scaleW, scaleH, 1F);
        GlStateManager.alphaFunc(GL11.GL_NOTEQUAL, 0);

        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
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

    public static File ensure(File parent, String name) {
        return ensure(new File(parent, name));
    }

    public static File ensure(File file) {
        file.getParentFile().mkdirs();
        return file;
    }
}
