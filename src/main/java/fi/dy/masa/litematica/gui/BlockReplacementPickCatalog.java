package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.Comparator;

import net.minecraft.core.registries.BuiltInRegistries;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.litematica.materials.MaterialCache;

/**
 * Builds a searchable catalog (block + every {@link BlockState} variant once) used by the schematic
 * material-list replacement dialog.
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

            for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates())
            {
                BlockReplacementPickEntry candidate = BlockReplacementPickEntry.create(state, cache);

                if (candidate.blockState().isAir())
                {
                    continue;
                }

                raw.add(candidate);
            }
        });

        raw.sort(
                Comparator.comparing(BlockReplacementPickEntry::primaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BlockReplacementPickEntry::secondaryLabel, String.CASE_INSENSITIVE_ORDER));

        return ImmutableList.copyOf(raw);
    }
}
