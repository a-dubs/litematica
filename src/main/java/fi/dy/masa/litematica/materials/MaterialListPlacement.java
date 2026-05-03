package fi.dy.masa.litematica.materials;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public class MaterialListPlacement extends MaterialListBase
{
    private final SchematicPlacement placement;

    public MaterialListPlacement(SchematicPlacement placement)
    {
        this(placement, false);
    }

    public MaterialListPlacement(SchematicPlacement placement, boolean reCreate)
    {
        super();

        this.placement = placement;

        if (reCreate)
        {
            this.reCreateMaterialList();
        }
    }

    @Override
    public boolean supportsRenderLayers()
    {
        return true;
    }

    @Override
    public boolean supportsMaterialListBlockReplacement()
    {
        return true;
    }

    public SchematicPlacement getSchematicPlacement()
    {
        return this.placement;
    }

    /**
     * Layer slicing used together with schematic iteration for counting and for schematic-storage edits —
     * {@link fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksBase}.
     */
    public LayerRange getMaterialListIterationLayerRange()
    {
        if (this.getMaterialListType() == BlockInfoListType.ALL)
        {
            return new LayerRange(SchematicWorldRefresher.INSTANCE);
        }

        return DataManager.getRenderLayerRange();
    }

    @Override
    public String getName()
    {
        return this.placement.getName();
    }

    @Override
    public String getTitle()
    {
        return StringUtils.translate("litematica.gui.title.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();
        TaskCountBlocksPlacement task = new TaskCountBlocksPlacement(this.placement, this, ignoreState);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
    }
}
