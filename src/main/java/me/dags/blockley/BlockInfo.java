package me.dags.blockley;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * @author dags <dags@dags.me>
 */
public class BlockInfo {

    private static final Set<String> propertyFilter = Sets.newHashSet(
            "age", "color", "eye", "half", "layers", "mode", "snowy", "type", "variant", "wet"
    );

    public final ItemStack stack;
    public final String name;
    public final String state;
    public final String identifier;
    public final int id;
    public final int meta;

    public BlockInfo(ItemStack stack, IBlockState state) {
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
