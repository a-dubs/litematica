package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import fi.dy.masa.litematica.gui.BlockReplacementPickEntry;
import fi.dy.masa.litematica.gui.BlockReplacementPickCatalog;
import fi.dy.masa.litematica.gui.Icons;

import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

import javax.annotation.Nullable;

public class WidgetListMaterialBlockReplacement extends WidgetListBase<BlockReplacementPickEntry, WidgetMaterialBlockReplacementEntry>
{
    public WidgetListMaterialBlockReplacement(int x, int y, int width, int height)
    {
        super(x, y, width, height, null);

        this.browserEntryHeight = 26;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
        this.shouldSortList = false;
    }

    @Override
    protected Collection<BlockReplacementPickEntry> getAllEntries()
    {
        return BlockReplacementPickCatalog.getAllVariants();
    }

    @Override
    protected List<String> getEntryStringsForFilter(BlockReplacementPickEntry entry)
    {
        return entry.searchKeys();
    }

    @Override
    protected Comparator<BlockReplacementPickEntry> getComparator()
    {
        return Comparator.comparing(BlockReplacementPickEntry::primaryLabel, String.CASE_INSENSITIVE_ORDER)
                         .thenComparing(BlockReplacementPickEntry::secondaryLabel, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected WidgetMaterialBlockReplacementEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable BlockReplacementPickEntry entry)
    {
        return new WidgetMaterialBlockReplacementEntry(x, y, this.browserEntryWidth, this.browserEntryHeight, isOdd, entry,
                                                       listIndex);
    }
}
