package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

public class MaterialListUtils
{
    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic)
    {
        return createMaterialListFor(schematic, schematic.getAreas().keySet());
    }

    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();

        for (String regionName : subRegions)
        {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

            if (container != null)
            {
                Vec3i size = container.getSize();
                final int sizeX = size.getX();
                final int sizeY = size.getY();
                final int sizeZ = size.getZ();

                for (int y = 0; y < sizeY; ++y)
                {
                    for (int z = 0; z < sizeZ; ++z)
                    {
                        for (int x = 0; x < sizeX; ++x)
                        {
                            BlockState state = container.get(x, y, z);
                            countsTotal.addTo(state, 1);
                        }
                    }
                }
            }
        }

        Minecraft mc = Minecraft.getInstance();

        return getMaterialList(countsTotal, countsTotal.clone(), new Object2IntOpenHashMap<>(), mc.player);
    }

    public static List<MaterialListEntry> getMaterialList(
            Object2IntOpenHashMap<BlockState> countsTotal,
            Object2IntOpenHashMap<BlockState> countsMissing,
            Object2IntOpenHashMap<BlockState> countsMismatch,
            Player player)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (!countsTotal.isEmpty())
        {
            MaterialCache cache = MaterialCache.getInstance();
            Object2IntOpenHashMap<ItemType> itemTypesTotal = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMissing = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMismatch = new Object2IntOpenHashMap<>();
            Object2ObjectOpenHashMap<ItemType, ObjectOpenHashSet<BlockState>> schematicSourcesByTypeTotal =
                    new Object2ObjectOpenHashMap<>();

            convertStatesToStacksWithReplacementSources(countsTotal, itemTypesTotal, schematicSourcesByTypeTotal, cache);
            convertStatesToStacks(countsMissing, itemTypesMissing, cache);
            convertStatesToStacks(countsMismatch, itemTypesMismatch, cache);

            if (player != null)
            {
                Object2IntOpenHashMap<ItemType> playerInvItems = getInventoryItemCounts(player.getInventory());

                for (ItemType type : itemTypesTotal.keySet())
                {
                    list.add(new MaterialListEntry(type.getStack().copy(),
                                                   itemTypesTotal.getInt(type),
                                                   itemTypesMissing.getInt(type),
                                                   itemTypesMismatch.getInt(type),
                                                   playerInvItems.getInt(type),
                                                   toImmutableSourceSet(schematicSourcesByTypeTotal.get(type))));
                }
            }
            else
            {
                for (ItemType type : itemTypesTotal.keySet())
                {
                    list.add(new MaterialListEntry(type.getStack().copy(),
                                                   itemTypesTotal.getInt(type),
                                                   itemTypesMissing.getInt(type),
                                                   itemTypesMismatch.getInt(type),
                                                   0,
                                                   toImmutableSourceSet(schematicSourcesByTypeTotal.get(type))));
                }
            }
        }

        return list;
    }

    private static ImmutableSet<BlockState> toImmutableSourceSet(@Nullable ObjectOpenHashSet<BlockState> setIn)
    {
        if (setIn == null || setIn.isEmpty())
        {
            return ImmutableSet.of();
        }

        return ImmutableSet.copyOf(setIn);
    }

    private static void convertStatesToStacksWithReplacementSources(
            Object2IntOpenHashMap<BlockState> blockStatesIn,
            Object2IntOpenHashMap<ItemType> itemTypesOut,
            Object2ObjectOpenHashMap<ItemType, ObjectOpenHashSet<BlockState>> schematicSourcesOut,
            MaterialCache cache)
    {
        for (BlockState state : blockStatesIn.keySet())
        {
            accumulateOneSchematicBlockStateIntoItemTypes(
                    state,
                    blockStatesIn.getInt(state),
                    itemTypesOut,
                    schematicSourcesOut,
                    cache);
        }
    }

    private static void accumulateOneSchematicBlockStateIntoItemTypes(BlockState schematicStateStored,
                                                                      int count,
                                                                      Object2IntOpenHashMap<ItemType> itemTypesOut,
                                                                      Object2ObjectOpenHashMap<ItemType,
                                                                              ObjectOpenHashSet<BlockState>> schematicSourcesOut,
                                                                      MaterialCache cache)
    {
        BlockState primary = isWaterloggedBlock(schematicStateStored) ?
                             getBaseBlockState(schematicStateStored) :
                             schematicStateStored;

        if (isWaterloggedBlock(schematicStateStored))
        {
            itemTypesOut.addTo(new ItemType(new ItemStack(Items.WATER_BUCKET), false, false), count);
            // Water bucket stacks are synthesized; omit block pins on synthesized bucket rows only.
        }

        final BlockState schematicKeyForPins = schematicStateStored;

        if (cache.requiresMultipleItems(primary))
        {
            boolean sawNonEmptyProduct = false;

            for (ItemStack stack : cache.getItems(primary))
            {
                if (!stack.isEmpty())
                {
                    sawNonEmptyProduct = true;
                    ItemType typeKey = new ItemType(stack, true, false);

                    itemTypesOut.addTo(typeKey, count * stack.getCount());
                    addReplacementSourcesFor(schematicSourcesOut, typeKey, schematicKeyForPins);
                }
            }

            if (!sawNonEmptyProduct)
            {
                maybeAddBareBlock(primary, schematicKeyForPins, count, itemTypesOut, schematicSourcesOut, cache);
            }
        }
        else
        {
            ItemStack stack = cache.getRequiredBuildItemForState(primary);

            if (!stack.isEmpty())
            {
                ItemType typeKey = new ItemType(stack, true, false);

                itemTypesOut.addTo(typeKey, count * stack.getCount());
                addReplacementSourcesFor(schematicSourcesOut, typeKey, schematicKeyForPins);
            }
            else
            {
                maybeAddBareBlock(primary, schematicKeyForPins, count, itemTypesOut, schematicSourcesOut, cache);
            }
        }
    }

    /**
     * {@link MaterialCache} omits some halves; synthesize stacks from vanilla block-as-item mappings so
     * tall blocks still expose replacement pins keyed to schematic storage.
     */
    private static void maybeAddBareBlock(BlockState primary,
                                          BlockState schematicStateStoredRaw,
                                          int count,
                                          Object2IntOpenHashMap<ItemType> itemTypesOut,
                                          Object2ObjectOpenHashMap<ItemType, ObjectOpenHashSet<BlockState>> schematicSourcesOut,
                                          MaterialCache cache)
    {
        ItemStack stackBare = cache.getRequiredBuildItemForState(primary);

        if (stackBare.isEmpty() == false)
        {
            return;
        }

        ItemStack asItemFallback = primary.getBlock().asItem().getDefaultInstance();

        if (asItemFallback.isEmpty())
        {
            return;
        }

        ItemType typeKey = new ItemType(asItemFallback, true, false);

        itemTypesOut.addTo(typeKey, count);
        addReplacementSourcesFor(schematicSourcesOut, typeKey, schematicStateStoredRaw);
    }

    private static void addReplacementSourcesFor(Object2ObjectOpenHashMap<ItemType, ObjectOpenHashSet<BlockState>> out,
                                                 ItemType typeKey,
                                                 @Nullable BlockState schematicBlockStateKey)
    {
        if (schematicBlockStateKey == null)
        {
            return;
        }

        ObjectOpenHashSet<BlockState> set = out.get(typeKey);

        if (set == null)
        {
            set = new ObjectOpenHashSet<>();

            out.put(typeKey, set);
        }

        MaterialListSourceStates.addExpanded(set, schematicBlockStateKey);
    }

    private static void convertStatesToStacks(
            Object2IntOpenHashMap<BlockState> blockStatesIn,
            Object2IntOpenHashMap<ItemType> itemTypesOut,
            MaterialCache cache)
    {
        for (BlockState state : blockStatesIn.keySet())
        {
            int count = blockStatesIn.getInt(state);
            BlockState stateToConvert = isWaterloggedBlock(state) ? getBaseBlockState(state) : state;

            // Add water bucket for waterlogged blocks
            if (isWaterloggedBlock(state))
            {
                itemTypesOut.addTo(new ItemType(new ItemStack(Items.WATER_BUCKET), false, false), count);
            }

            // Convert block to items
            if (cache.requiresMultipleItems(stateToConvert))
            {
                for (ItemStack stack : cache.getItems(stateToConvert))
                {
                    if (!stack.isEmpty())
                    {
                        itemTypesOut.addTo(new ItemType(stack, true, false), count * stack.getCount());
                    }
                }
            }
            else
            {
                ItemStack stack = cache.getRequiredBuildItemForState(stateToConvert);
                if (!stack.isEmpty())
                {
                    itemTypesOut.addTo(new ItemType(stack, true, false), count * stack.getCount());
                }
            }
        }
    }

    public static void updateAvailableCounts(List<MaterialListEntry> list, Player player)
    {
        if (player == null) return;
        Object2IntOpenHashMap<ItemType> playerInvItems = getInventoryItemCounts(player.getInventory());

        for (MaterialListEntry entry : list)
        {
            ItemType type = new ItemType(entry.getStack(), true, false);
            int countAvailable = playerInvItems.getInt(type);
            entry.setCountAvailable(countAvailable);
        }
    }

    public static Object2IntOpenHashMap<ItemType> getInventoryItemCounts(Container inv)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        final int slots = inv.getContainerSize();

        for (int slot = 0; slot < slots; ++slot)
        {
            ItemStack stack = inv.getItem(slot);

            if (stack.isEmpty() == false)
            {
                Item item = stack.getItem();

                if (item instanceof BlockItem &&
                    ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock &&
                    InventoryUtils.shulkerBoxHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> boxCounts = getStoredItemCounts(stack);

                    for (ItemType boxType : boxCounts.keySet())
                    {
                        map.addTo(boxType, boxCounts.getInt(boxType));
                    }

                    boxCounts.clear();
                }
                else if (item instanceof BundleItem && InventoryUtils.bundleHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> bundleCounts = getBundleItemCounts(stack);

                    for (ItemType bundleType : bundleCounts.keySet())
                    {
                        map.addTo(bundleType, bundleCounts.getInt(bundleType));
                    }

                    bundleCounts.clear();
                }
                else
                {
                    map.addTo(new ItemType(stack, true, false), stack.getCount());
                }
            }
        }

        return map;
    }

    public static Object2IntOpenHashMap<ItemType> getStoredItemCounts(ItemStack stackShulkerBox)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        NonNullList<ItemStack> items = InventoryUtils.getStoredItems(stackShulkerBox);

        for (ItemStack boxStack : items)
        {
            if (boxStack.isEmpty() == false)
            {
                // Copy Nested Bundles
                if (boxStack.getItem() instanceof BundleItem && InventoryUtils.bundleHasItems(boxStack))
                {
                    Object2IntOpenHashMap<ItemType> bundleMap = getBundleItemCounts(boxStack);

                    if (!bundleMap.isEmpty())
                    {
                        bundleMap.forEach(map::addTo);
                    }
                }

                map.addTo(new ItemType(boxStack, false, false), boxStack.getCount());
            }
        }

        return map;
    }

    public static Object2IntOpenHashMap<ItemType> getBundleItemCounts(ItemStack stackBundle)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        NonNullList<ItemStack> items = InventoryUtils.getBundleItems(stackBundle);

        for (ItemStack bundleStack : items)
        {
            if (bundleStack.isEmpty() == false)
            {
                // Copy Nested Bundles
                if (bundleStack.getItem() instanceof BundleItem && InventoryUtils.bundleHasItems(bundleStack))
                {
                    Object2IntOpenHashMap<ItemType> bundleMap = getBundleItemCounts(bundleStack);

                    if (!bundleMap.isEmpty())
                    {
                        bundleMap.forEach(map::addTo);
                    }
                }

                map.addTo(new ItemType(bundleStack, false, false), bundleStack.getCount());
            }
        }

        return map;
    }

    private static boolean isWaterloggedBlock(BlockState state)
    {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) &&
               state.getValue(BlockStateProperties.WATERLOGGED);
    }

    private static BlockState getBaseBlockState(BlockState state)
    {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED))
        {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return state;
    }
}
