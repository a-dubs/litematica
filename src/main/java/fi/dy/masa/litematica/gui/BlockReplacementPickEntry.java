package fi.dy.masa.litematica.gui;

import java.util.List;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.litematica.materials.MaterialCache;

/**
 * Single selectable schematic block-state row for wholesale replacement dialogs.
 */

public record BlockReplacementPickEntry(BlockState blockState,
                                        ItemStack displayStack,
                                        String primaryLabel,
                                        String secondaryLabel,
                                        List<String> searchKeys)
{
    public static BlockReplacementPickEntry create(BlockState state, MaterialCache cache)
    {
        ItemStack stack = cache.getRequiredBuildItemForState(state).copy();

        if (stack.isEmpty())
        {
            stack = cache.getItemForDisplayNameForState(state).copy();

            if (stack.isEmpty())
            {
                stack = new ItemStack(state.getBlock().asItem());
            }
        }

        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        ImmutableList.Builder<String> keys = ImmutableList.builder();

        keys.add(id.toLowerCase());
        keys.add(bracketsOnly(trimStateForSearch(state)).toLowerCase());

        if (stack.isEmpty() == false)
        {
            keys.add(stack.getHoverName().getString().toLowerCase());
        }

        String detail = bracketsOnly(state.toString());

        return new BlockReplacementPickEntry(state, stack.copy(), labelPrimary(stack, id), detail, keys.build());
    }

    private static String labelPrimary(ItemStack stack, String idFallback)
    {
        if (!stack.isEmpty())
        {
            return stack.getHoverName().getString();
        }

        return idFallback;
    }

    private static String trimStateForSearch(BlockState state)
    {
        String s = state.toString();
        int bracket = s.indexOf('[');

        return bracket >= 0 ? s.substring(bracket) : s;
    }

    /**
     * Strips Mojang/debug block wrappers so search keys stay concise.
     */
    private static String bracketsOnly(String raw)
    {
        int bracket = raw.indexOf('[');

        if (bracket > 0)
        {
            return raw.substring(bracket);
        }

        return raw;
    }

    /** Default factory guards for catalog construction. */
    public static boolean isSkippableBlock(net.minecraft.world.level.block.Block block)
    {
        return block == Blocks.AIR ||
               block == Blocks.CAVE_AIR ||
               block == Blocks.VOID_AIR;
    }
}
