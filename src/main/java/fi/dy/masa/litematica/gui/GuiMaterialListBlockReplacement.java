package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialBlockReplacement;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialBlockReplacementEntry;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiMaterialListBlockReplacement extends GuiListBase<BlockReplacementPickEntry,
        WidgetMaterialBlockReplacementEntry, WidgetListMaterialBlockReplacement>
{
    private final GuiMaterialList parentGui;
    private final MaterialListPlacement placementBacking;
    private final MaterialListEntry rowEntry;

    public GuiMaterialListBlockReplacement(GuiMaterialList parentGui, MaterialListPlacement placementBacking,
            MaterialListEntry rowEntry)
    {
        super(12, 74);

        this.parentGui = parentGui;
        this.placementBacking = placementBacking;
        this.rowEntry = rowEntry;
        this.title = StringUtils.translate("litematica.gui.title.material_list.replace_block");
        this.useTitleHierarchy = false;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.getScreenWidth() - 22;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.getScreenHeight() - 126;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        String itemName = this.rowEntry.getStack().getHoverName().getString();
        String subtitle = StringUtils.translate("litematica.gui.label.material_list.replace_intro", itemName);

        int w = this.getStringWidth(subtitle);
        this.addLabel(12, 34, Math.min(w + 4, this.getScreenWidth() - 24), 12, 0xFFFFFFFF, subtitle);

        int yBtn = this.getScreenHeight() - 26;

        ButtonGeneric btnApply = new ButtonGeneric(12, yBtn, -1, 20,
                StringUtils.translate("litematica.gui.button.material_list.replace_apply"));

        ButtonGeneric btnCancel = new ButtonGeneric(12 + btnApply.getWidth() + 8, yBtn, -1, 20,
                StringUtils.translate("litematica.gui.button.material_list.replace_cancel"));

        btnApply.setHoverStrings("litematica.gui.button.hover.material_list.replace_apply");
        this.addButton(btnApply, new ButtonListener(ButtonListener.Type.APPLY, this));
        this.addButton(btnCancel, new ButtonListener(ButtonListener.Type.CANCEL, this));
    }

    @Override
    protected WidgetListMaterialBlockReplacement createListWidget(int listX, int listY)
    {
        return new WidgetListMaterialBlockReplacement(listX, listY, this.getBrowserWidth(),
                this.getBrowserHeight());
    }

    void applyChosenReplacement(BlockReplacementPickEntry pick)
    {
        if (pick == null)
        {
            this.addMessage(MessageType.ERROR, "litematica.message.error.material_list.replace_no_pick");
            return;
        }

        boolean ok = SchematicUtils.replaceMaterialRowBlocksAcrossPlacement(this.placementBacking.getSchematicPlacement(),
                this.rowEntry.getSchematicReplaceSourceStates(), pick.blockState(), this.mc.level,
                this.placementBacking.getMaterialListIterationLayerRange());

        if (ok)
        {
            this.parentGui.addMessage(MessageType.SUCCESS, "litematica.message.material_list.replace_success");
            this.placementBacking.reCreateMaterialList();
            GuiBase.openGui(this.parentGui);
        }
        else
        {
            this.addMessage(MessageType.ERROR, "litematica.message.error.material_list.replace_failed");
        }
    }

    private record ButtonListener(Type type,
                                  GuiMaterialListBlockReplacement gui) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.APPLY)
            {
                BlockReplacementPickEntry picked = this.gui.getListWidget().getLastSelectedEntry();

                this.gui.applyChosenReplacement(picked);
            }
            else if (this.type == Type.CANCEL)
            {
                GuiBase.openGui(this.gui.parentGui);
            }
        }

        private enum Type
        {
            APPLY,
            CANCEL
        }
    }
}
