package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.Comparator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.litematica.materials.MaterialCache;

/**
 * Builds a searchable catalog with one canonical {@link BlockState} per registered block (typically
 * {@link net.minecraft.world.level.block.Block#defaultBlockState()}). Orientation and other
 * properties overlapping the source schematic state are reapplied during replacement.
 */
public final class BlockReplacementPickCatalog
{
    private static volatile ImmutableList<BlockReplacementPickEntry> cached;

    private BlockReplacementPickCatalog() {}

    public static ImmutableList<BlockReplacementPickEntry> getAllVariants()
    {
        ImmutableList<BlockReplacementPickEntry> list = cached;

        if (list != null)
        {
            return list;
        }

        synchronized (BlockReplacementPickCatalog.class)
        {
            list = cached;

            if (list != null)
            {
                return list;
            }

            cached = rebuild();
            return cached;
        }
    }

    private static ImmutableList<BlockReplacementPickEntry> rebuild()
    {
        MaterialCache cache = MaterialCache.getInstance();
        ArrayList<BlockReplacementPickEntry> raw = new ArrayList<>();

        BuiltInRegistries.BLOCK.forEach(block ->
        {
            if (BlockReplacementPickEntry.isSkippableBlock(block))
            {
                return;
            }

            BlockState state = block.defaultBlockState();

            raw.add(BlockReplacementPickEntry.create(state, cache));
        });

        raw.sort(
                Comparator.comparing(BlockReplacementPickEntry::primaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BlockReplacementPickEntry::secondaryLabel, String.CASE_INSENSITIVE_ORDER));

        return ImmutableList.copyOf(raw);
    }
}
