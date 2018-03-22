package me.dags.blockley;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
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
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author dags <dags@dags.me>
 */
@Mod(modid = "blockley", name = "Blockley", version = "2.0.0", clientSideOnly = true)
public class Blockley {

    private static final KeyBinding show = new KeyBinding("blockley.show", Keyboard.KEY_B, "Blockley");
    private static final AtomicReference<ExportTask> exportTask = new AtomicReference<>(ExportTask.EMPTY);

    private File index = new File("");
    private File baseDir = new File("");

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        File root = event.getModConfigurationDirectory();
        baseDir = new File(root, "blockley");
        index = new File(baseDir, "index.html");
        MinecraftForge.EVENT_BUS.register(this);
        ClientRegistry.registerKeyBinding(show);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        ExportTask task = exportTask.get();

        if (task != ExportTask.EMPTY) {
            if (task.isDone()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString("Finished generating block list!"));
                exportTask.set(ExportTask.EMPTY);
                return;
            }

            task.tick();
            return;
        }

        if (Minecraft.getMinecraft().inGameHasFocus && show.isPressed()) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || !index.exists()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString("Generating block list (this may take a while and lower your fps temporarily)..."));
                createTask();
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

    private void createTask() {
        List<BlockInfo> items = getAllItems();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        ExportTask task = new ExportTask(items, resolution, baseDir);
        exportTask.set(task);
    }

    private static List<BlockInfo> getAllItems() {
        LinkedList<BlockInfo> list = new LinkedList<>();
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
}
