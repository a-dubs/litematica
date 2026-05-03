package fi.dy.masa.litematica.materials;

import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Collects schematic / schematic-world {@link BlockState} keys backing a material-list row so
 * replacements can touch every matching cell (including paired double blocks such as beds and doors).
 */
public final class MaterialListSourceStates
{
    private MaterialListSourceStates() {}

    /**
     * Adds {@code state} plus any mirrored half for two-block tall placements so wholesale replace
     * clears both halves.
     */
    public static void addExpanded(ObjectOpenHashSet<BlockState> out, BlockState state)
    {
        out.add(state);
        BlockState partner = pairingState(state);

        if (partner != null)
        {
            out.add(partner);
        }
    }

    private static BlockState pairingState(BlockState state)
    {
        if (state.hasProperty(BedBlock.PART))
        {
            BedPart part = state.getValue(BedBlock.PART);
            BedPart opposite = part == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT;

            try
            {
                return state.setValue(BedBlock.PART, opposite);
            }
            catch (IllegalArgumentException ignored)
            {
                return null;
            }
        }

        if (state.hasProperty(DoorBlock.HALF))
        {
            DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
            DoubleBlockHalf opposite = half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;

            try
            {
                return state.setValue(DoorBlock.HALF, opposite);
            }
            catch (IllegalArgumentException ignored)
            {
                return null;
            }
        }

        if (state.hasProperty(DoublePlantBlock.HALF))
        {
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            DoubleBlockHalf opposite = half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;

            try
            {
                return state.setValue(DoublePlantBlock.HALF, opposite);
            }
            catch (IllegalArgumentException ignored)
            {
                return null;
            }
        }

        return null;
    }
}
