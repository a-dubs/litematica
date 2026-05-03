package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;

import fi.dy.masa.litematica.gui.BlockReplacementPickEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetMaterialBlockReplacementEntry extends WidgetListEntryBase<BlockReplacementPickEntry>
{
    private final BlockReplacementPickEntry pick;
    private final boolean isOdd;

    public WidgetMaterialBlockReplacementEntry(int x, int y, int width, int height, boolean isOdd,
            BlockReplacementPickEntry pick, int listIndex)
    {
        super(x, y, width, height, pick, listIndex);

        this.pick = pick;
        this.isOdd = isOdd;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0x20FFFFFF);
        }
        else
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0x50FFFFFF);
        }

        int iconX = this.x + 3;
        int iconY = this.y + 5;

        if (this.pick.displayStack().isEmpty() == false)
        {
            RenderUtils.drawRect(ctx, iconX, iconY, 16, 16, 0x20FFFFFF);
            ctx.renderItem(this.pick.displayStack(), iconX, iconY);
        }

        int textX = this.x + 22;

        this.drawString(ctx, textX, this.y + 6, 0xFFFFFFFF, this.pick.primaryLabel());
        String sub = shortenSecondary(this.pick.secondaryLabel(), this.width - 28);

        if (sub.isEmpty() == false)
        {
            this.drawString(ctx, textX, this.y + 17, 0xFFBBBBBB, sub);
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, this.height))
        {
            ArrayList<String> lines = new ArrayList<>(2);

            lines.add(this.pick.secondaryLabel());
            lines.add(this.pick.blockState().toString());
            RenderUtils.drawHoverText(ctx, mouseX, mouseY, lines);
        }

        super.postRenderHovered(ctx, mouseX, mouseY, selected);
    }

    private static String shortenSecondary(String raw, int maxWidth)
    {
        if (raw == null || raw.isEmpty())
        {
            return "";
        }

        if (StringUtils.getStringWidth(raw) <= maxWidth)
        {
            return raw;
        }

        final String ellipsis = "...";
        int budget = maxWidth - StringUtils.getStringWidth(ellipsis);

        if (budget <= 0)
        {
            return ellipsis;
        }

        StringBuilder shortened = new StringBuilder();

        for (int i = 0; i < raw.length(); ++i)
        {
            char ch = raw.charAt(i);
            String next = shortened.toString() + ch;

            if (StringUtils.getStringWidth(next) > budget)
            {
                break;
            }

            shortened.append(ch);
        }

        return shortened.toString() + ellipsis;
    }
}
