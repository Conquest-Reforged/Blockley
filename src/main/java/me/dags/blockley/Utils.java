package me.dags.blockley;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * @author dags <dags@dags.me>
 */
public class Utils {

    private static int renderTextureSize = 128;
    private static int framebufferID = -1;
    private static int depthbufferID = -1;
    private static int textureID = -1;

    private static IntBuffer lastViewport;
    private static int lastTexture;
    private static int lastFramebuffer;

    public static BufferedImage createImage(ItemStack stack, ScaledResolution resolution) {
        setup(64);
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        cleanUp();
        return getImage();
    }

    /*
    Credit p455w0rds:
    https://github.com/p455w0rd/p455w0rds-Library/blob/master/src/main/java/p455w0rdslib/util/ImageUtils.java
     */
    private static void setup(int size) {
        pushFBO(size);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, 16, 16, 0, -100000.0, 100000.0);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        FloatBuffer matrix = GLAllocation.createDirectFloatBuffer(16);
        matrix.clear();
        matrix.put(new float[] {
                1f,0f,0f,0f,
                0f,1f,0f,0f,
                0f,0f,-1f,0f,
                0f,0f,0f,1f
        });
        matrix.rewind();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
    }

    /*
    Credit p455w0rds:
    https://github.com/p455w0rd/p455w0rds-Library/blob/master/src/main/java/p455w0rdslib/util/ImageUtils.java
     */
    private static void cleanUp() {
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        popFBO();
        lastTexture = -1;
        lastFramebuffer = -1;
        lastViewport = null;
    }

    /*
    Credit p455w0rds:
    https://github.com/p455w0rd/p455w0rds-Library/blob/master/src/main/java/p455w0rdslib/util/RenderUtils.java
     */
    private static void pushFBO(int size) {
        renderTextureSize = size;
        GL30.glDeleteFramebuffers(framebufferID);
        GL11.glDeleteTextures(textureID);
        GL30.glDeleteRenderbuffers(depthbufferID);

        framebufferID = GL30.glGenFramebuffers();
        textureID = GL11.glGenTextures();
        int currentFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferID);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, renderTextureSize, renderTextureSize, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
        depthbufferID = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthbufferID);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, renderTextureSize, renderTextureSize);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthbufferID);
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, textureID, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFramebuffer);

        lastFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferID);
        lastViewport = GLAllocation.createDirectIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, lastViewport);
        GL11.glViewport(0, 0, renderTextureSize, renderTextureSize);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        lastTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.clearColor(0, 0, 0, 0);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableRescaleNormal();
    }

    /*
    Credit p455w0rds:
    https://github.com/p455w0rd/p455w0rds-Library/blob/master/src/main/java/p455w0rdslib/util/RenderUtils.java
     */
    private static void popFBO() {
        GlStateManager.disableDepth();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GL11.glViewport(lastViewport.get(0), lastViewport.get(1), lastViewport.get(2), lastViewport.get(3));
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lastFramebuffer);
        GlStateManager.bindTexture(lastTexture);
    }

    /*
    Credit p455w0rds:
    https://github.com/p455w0rd/p455w0rds-Library/blob/master/src/main/java/p455w0rdslib/util/RenderUtils.java
     */
    private static BufferedImage getImage() {
        GlStateManager.bindTexture(textureID);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        IntBuffer texture = BufferUtils.createIntBuffer(width * height);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, texture);
        int[] texture_array = new int[width * height];
        texture.get(texture_array);
        BufferedImage image = new BufferedImage(renderTextureSize, renderTextureSize, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, renderTextureSize, renderTextureSize, texture_array, 0, width);
        AffineTransform flip = AffineTransform.getScaleInstance(1, -1);
        flip.translate(0, -renderTextureSize);
        return new AffineTransformOp(flip, null).filter(image, null);
    }

    public static File ensure(File parent, String name) {
        return ensure(new File(parent, name));
    }

    public static File ensure(File file) {
        file.getParentFile().mkdirs();
        return file;
    }
}
