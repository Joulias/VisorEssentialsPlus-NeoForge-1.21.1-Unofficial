package org.vmstudio.essentials.core.client.gui.screens;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import org.vmstudio.essentials.core.client.compat.ColorfulHeartsCompat;
import org.vmstudio.essentials.core.client.compat.OverloadedArmorBarCompat;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.essentials.core.client.config.QuickActionDisplayMode;
import org.vmstudio.essentials.core.client.config.WristHudMetric;
import org.vmstudio.essentials.core.client.config.WristHudPosition;
import org.vmstudio.essentials.core.client.config.WristUiConfig;
import org.vmstudio.essentials.core.client.gui.ContainerSlot;
import org.vmstudio.essentials.core.client.gui.overlays.VROverlayContainer;
import org.vmstudio.essentials.core.client.extensions.AbstractContainerScreenExtension;
import org.vmstudio.essentials.core.client.quickactions.QuickActionManager;
import org.vmstudio.essentials.core.common.VisorEssentials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;


public class VRInvScreen extends VRInvEffectInvScreen implements AbstractContainerScreenExtension {
    private static final int CRAFT_RESULT_X = 237;
    private static final int CRAFT_RESULT_Y = 36;
    private static final int CONTEXT_ACTION_WIDTH = 128;
    private static final int CONTEXT_ACTION_HEIGHT = 20;
    private static final int CONTEXT_ACTION_GAP = 6;
    private static final int UTILITY_ACTION_SIZE = 20;
    private static final int UTILITY_ACTION_GAP = 2;
    private static final int QUICK_ACTION_SIZE = 20;
    private static final int QUICK_ACTION_GAP = 2;
    private static final int QUICK_ACTION_FIRST_Y = 90;
    private static final int QUICK_ACTION_LEFT_X = 20;
    private static final int QUICK_ACTION_RIGHT_X = 218;
    private static final int QUICK_ACTION_OUTER_LEFT_X = -2;
    private static final int QUICK_ACTION_OUTER_RIGHT_X = 240;
    private static final int CREATIVE_MODE_ACTION_WIDTH = 128;
    private static final int CREATIVE_MODE_ACTION_HEIGHT = 20;
    private static final int CREATIVE_MODE_ACTION_GAP = 6;
    private static final int STATUS_STRIP_X = 42;
    private static final int STATUS_STRIP_WIDTH = 174;
    private static final int STATUS_METRIC_WIDTH = STATUS_STRIP_WIDTH / 2;
    private static final int STATUS_METRIC_HEIGHT = 16;

    private static final ResourceLocation HEALTH_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/heart/full"
    );
    private static final ResourceLocation HEALTH_CONTAINER_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/heart/container"
    );
    private static final ResourceLocation HEALTH_HALF_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/heart/half"
    );
    private static final ResourceLocation ABSORPTION_FULL_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/heart/absorbing_full"
    );
    private static final ResourceLocation ABSORPTION_HALF_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/heart/absorbing_half"
    );
    private static final ResourceLocation HUNGER_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/food_full"
    );
    private static final ResourceLocation HUNGER_EMPTY_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/food_empty"
    );
    private static final ResourceLocation HUNGER_HALF_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/food_half"
    );
    private static final ResourceLocation ARMOR_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/armor_full"
    );
    private static final ResourceLocation ARMOR_EMPTY_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/armor_empty"
    );
    private static final ResourceLocation ARMOR_HALF_ICON = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/armor_half"
    );
    private static final ResourceLocation EXPERIENCE_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/experience_bar_background"
    );
    private static final ResourceLocation EXPERIENCE_PROGRESS = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "hud/experience_bar_progress"
    );

    private static final ResourceLocation IMAGE_FULL = ResourceLocation.fromNamespaceAndPath(
            VisorEssentials.MOD_ID,"textures/gui/inventory.png"
    );
    private static final ResourceLocation IMAGE_SIMPLIFIED = ResourceLocation.fromNamespaceAndPath(
            VisorEssentials.MOD_ID,"textures/gui/inventory_simplified.png"
    );
    private static final Component CREATIVE_MODE_ACTION_LABEL = Component.translatable(
            "visor_essentials.inventory.creative_mode"
    );
    private static final Component CREATIVE_MODE_ACTION_TOOLTIP = Component.translatable(
            "visor_essentials.inventory.creative_mode.tooltip"
    );


    private float xMouse;
    private float yMouse;

    private @Nullable Component contextActionLabel;
    private @Nullable Runnable contextAction;
    private @Nullable Runnable creativeModeAction;
    private final Map<String, UtilityAction> utilityActions = new LinkedHashMap<>();
    private CraftingInput cachedCraftingInput = CraftingInput.EMPTY;
    private ItemStack cachedCraftingResult = ItemStack.EMPTY;

    public VRInvScreen(AbstractContainerMenu menu,
                       Inventory inventory) {
        super(menu,  inventory, Component.literal(""));
        this.titleLabelX = 97;
        this.imageWidth = 258;
        this.imageHeight = 156;
        visorEssentials$setVRContainer(true);
    }
    @Override
    public void visorEssentials$fillVRSlots(
            @NotNull List<ContainerSlot> slots
    ) {
        fullInventory = !VisorAPI.client().getGuiManager()
                .getOverlayManager().getOverlay(VROverlayContainer.ID).isEnabled();

        slots.clear();
        for(Slot slot : menu.slots){
            int posX = 0;
            int posY = 0;
            if((slot.container instanceof CraftingContainer)
                    && fullInventory){
                // 2x2 grid layout
                int index = slot.getContainerSlot();
                int row = index / 2;
                int col = index % 2;
                posX = 181 + col * 18;
                posY = 26 + row * 18;
                slots.add(new ContainerSlot(slot, posX,posY));
            }else if(slot.container instanceof Inventory){
                if(slot.getContainerSlot()<=8){
                    //hotbar
                    switch (slot.getContainerSlot()){
                        case 0 -> {
                            posX = 121;
                            posY = 38;
                        }
                        case 1 -> {
                            posX = 121;
                            posY = 11;
                        }
                        case 2 -> {
                            posX = 148;
                            posY = 11;
                        }
                        case 3 -> {
                            posX = 148;
                            posY = 38;
                        }
                        case 4 -> {
                            posX = 148;
                            posY = 65;
                        }
                        case 5 -> {
                            posX = 121;
                            posY = 65;
                        }
                        case 6 -> {
                            posX = 94;
                            posY = 65;
                        }
                        case 7 -> {
                            posX = 94;
                            posY = 38;
                        }
                        case 8 -> {
                            posX = 94;
                            posY = 11;
                        }
                    }

                }else if(slot.getContainerSlot()<=35) {
                    // 9x3 grid layout
                    int index = slot.getContainerSlot() - 9;
                    int row = index / 9;
                    int col = index % 9;
                    posX = 49 + col * 18;
                    posY = 96 + row * 18;
                }else{
                    if(!fullInventory) continue;
                    // equipment slots
                    if(slot.getContainerSlot() == 40) continue; //ignore offhand
                    int index = slot.getContainerSlot() - 36;
                    posX = 8;
                    posY = 62 + index * -18;
                }
                slots.add(new ContainerSlot(slot,posX,posY));
            }
            else if(fullInventory && slot instanceof ResultSlot){
                posX = CRAFT_RESULT_X;
                posY = CRAFT_RESULT_Y;
                slots.add(new ContainerSlot(slot, posX,posY));
            }
        }
    }
    @Override
    public void containerTick() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderCraftingResult(guiGraphics);
        renderContextAction(guiGraphics, mouseX, mouseY);
        renderQuickActions(guiGraphics, mouseX, mouseY, false);
        renderUtilityActions(guiGraphics, mouseX, mouseY, false);
        renderCreativeModeAction(guiGraphics, mouseX, mouseY, false);
        renderStatusStrip(guiGraphics, mouseX, mouseY, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderStatusStrip(guiGraphics, mouseX, mouseY, true);
        renderQuickActions(guiGraphics, mouseX, mouseY, true);
        renderUtilityActions(guiGraphics, mouseX, mouseY, true);
        renderCreativeModeAction(guiGraphics, mouseX, mouseY, true);

        this.xMouse = (float)mouseX;
        this.yMouse = (float)mouseY;
    }

    public void setContextAction(@Nullable Component label,
                                 @Nullable Runnable action) {
        contextActionLabel = label;
        contextAction = action;
    }
    public void setCreativeModeAction(@Nullable Runnable action) {
        creativeModeAction = action;
    }

    private void renderCreativeModeAction(GuiGraphics graphics,
                                          int mouseX,
                                          int mouseY,
                                          boolean tooltipPass) {
        if (!isCreativeModeActionVisible()) {
            return;
        }
        boolean hovered = isOverCreativeModeAction(mouseX, mouseY);
        if (tooltipPass) {
            if (hovered) {
                graphics.renderTooltip(
                        font,
                        CREATIVE_MODE_ACTION_TOOLTIP,
                        mouseX,
                        mouseY
                );
            }
            return;
        }

        int x = getCreativeModeActionX();
        int y = getCreativeModeActionY();
        int background = hovered ? 0xEA454545 : 0xDA202020;
        int border = hovered ? 0xFFFFFFFF : 0xFF9A9A9A;
        graphics.fill(
                x,
                y,
                x + CREATIVE_MODE_ACTION_WIDTH,
                y + CREATIVE_MODE_ACTION_HEIGHT,
                background
        );
        graphics.fill(x, y, x + CREATIVE_MODE_ACTION_WIDTH, y + 1, border);
        graphics.fill(
                x,
                y + CREATIVE_MODE_ACTION_HEIGHT - 1,
                x + CREATIVE_MODE_ACTION_WIDTH,
                y + CREATIVE_MODE_ACTION_HEIGHT,
                border
        );
        graphics.fill(x, y, x + 1, y + CREATIVE_MODE_ACTION_HEIGHT, border);
        graphics.fill(
                x + CREATIVE_MODE_ACTION_WIDTH - 1,
                y,
                x + CREATIVE_MODE_ACTION_WIDTH,
                y + CREATIVE_MODE_ACTION_HEIGHT,
                border
        );
        graphics.drawCenteredString(
                font,
                CREATIVE_MODE_ACTION_LABEL,
                x + CREATIVE_MODE_ACTION_WIDTH / 2,
                y + (CREATIVE_MODE_ACTION_HEIGHT - 8) / 2,
                0xFFFFFFFF
        );
    }

    private boolean isCreativeModeActionVisible() {
        return fullInventory
                && creativeModeAction != null
                && minecraft != null
                && minecraft.player != null
                && minecraft.gameMode != null
                && minecraft.gameMode.hasInfiniteItems()
                && menu == minecraft.player.inventoryMenu
                && minecraft.player.containerMenu == minecraft.player.inventoryMenu;
    }

    private int getCreativeModeActionX() {
        return leftPos + (imageWidth - CREATIVE_MODE_ACTION_WIDTH) / 2;
    }

    private int getCreativeModeActionY() {
        int topEdge = topPos;
        for (StatusGroup group : buildStatusLayout().groups()) {
            if (group.position() == WristHudPosition.TOP) {
                topEdge = Math.min(topEdge, group.bounds().y());
            }
        }
        return topEdge - CREATIVE_MODE_ACTION_HEIGHT - CREATIVE_MODE_ACTION_GAP;
    }

    private boolean isOverCreativeModeAction(double mouseX, double mouseY) {
        int x = getCreativeModeActionX();
        int y = getCreativeModeActionY();
        return mouseX >= x && mouseX < x + CREATIVE_MODE_ACTION_WIDTH
                && mouseY >= y && mouseY < y + CREATIVE_MODE_ACTION_HEIGHT;
    }

    public void setUtilityAction(@NotNull String ownerId,
                                 @NotNull ItemStack icon,
                                 @NotNull Component tooltip,
                                 @NotNull BooleanSupplier visible,
                                 @NotNull Runnable action) {
        setUtilityAction(ownerId, icon, tooltip, visible, () -> false, action);
    }

    public void setUtilityAction(@NotNull String ownerId,
                                 @NotNull ItemStack icon,
                                 @NotNull Component tooltip,
                                 @NotNull BooleanSupplier visible,
                                 @NotNull BooleanSupplier toggled,
                                 @NotNull Runnable action) {
        utilityActions.put(ownerId, new UtilityAction(
                icon.copy(),
                tooltip,
                visible,
                toggled,
                action
        ));
    }

    public void clearUtilityAction(@NotNull String ownerId) {
        utilityActions.remove(ownerId);
    }

    /**
     * Renders addon shortcuts in the unused upper-right corner of the player
     * portrait. Keeping the actions inside the existing panel avoids claiming
     * more wrist space and leaves the opposite arm available for other UIs.
     */
    private void renderUtilityActions(GuiGraphics graphics,
                                      int mouseX,
                                      int mouseY,
                                      boolean tooltipPass) {
        List<UtilityAction> visibleActions = visibleUtilityActions();
        for (int index = 0; index < visibleActions.size(); index++) {
            UtilityAction action = visibleActions.get(index);
            int x = getUtilityActionX(index);
            int y = getUtilityActionY(index);
            boolean hovered = isOverUtilityAction(mouseX, mouseY, index);

            if (tooltipPass) {
                if (hovered) {
                    graphics.renderTooltip(
                            font,
                            List.of(action.tooltip()),
                            Optional.empty(),
                            mouseX,
                            mouseY
                    );
                }
                continue;
            }

            boolean toggled = action.toggled().getAsBoolean();
            int background = toggled
                    ? (hovered ? 0xE04B6251 : 0xD02E4735)
                    : (hovered ? 0xE0434343 : 0xD0181818);
            int border = toggled
                    ? (hovered ? 0xFFFFFFFF : 0xFF7FD67F)
                    : (hovered ? 0xFFFFFFFF : 0xFF9A9A9A);
            graphics.fill(
                    x,
                    y,
                    x + UTILITY_ACTION_SIZE,
                    y + UTILITY_ACTION_SIZE,
                    background
            );
            graphics.fill(x, y, x + UTILITY_ACTION_SIZE, y + 1, border);
            graphics.fill(
                    x,
                    y + UTILITY_ACTION_SIZE - 1,
                    x + UTILITY_ACTION_SIZE,
                    y + UTILITY_ACTION_SIZE,
                    border
            );
            graphics.fill(x, y, x + 1, y + UTILITY_ACTION_SIZE, border);
            graphics.fill(
                    x + UTILITY_ACTION_SIZE - 1,
                    y,
                    x + UTILITY_ACTION_SIZE,
                    y + UTILITY_ACTION_SIZE,
                    border
            );
            graphics.renderItem(action.icon(), x + 2, y + 2);
        }
    }

    private List<UtilityAction> visibleUtilityActions() {
        if (!fullInventory) {
            return List.of();
        }
        return utilityActions.values().stream()
                .filter(action -> action.visible().getAsBoolean())
                .toList();
    }

    private int getUtilityActionX(int index) {
        // The compact addon shortcut occupies the small bridge at the
        // portrait's upper-right, immediately before the circular hotbar. This
        // is the blue-marked position in the wrist-inventory layout and keeps
        // the six configurable actions around the lower inventory unobstructed.
        return leftPos + 58;
    }

    private int getUtilityActionY(int index) {
        return topPos + 4 + index * (UTILITY_ACTION_SIZE + UTILITY_ACTION_GAP);
    }

    private boolean isOverUtilityAction(double mouseX,
                                        double mouseY,
                                        int index) {
        int x = getUtilityActionX(index);
        int y = getUtilityActionY(index);
        return mouseX >= x && mouseX < x + UTILITY_ACTION_SIZE
                && mouseY >= y && mouseY < y + UTILITY_ACTION_SIZE;
    }

    /**
     * Draws up to twelve user-configurable shortcuts as connected tabs around
     * the lower inventory panel. Slots 0-2 and 3-5 retain the original inner
     * left/right columns; slots 6-8 and 9-11 add outer left/right columns.
     * Their bounds stop before the nearest item slot, so a laser click can
     * never be mistaken for an inventory interaction.
     */
    private void renderQuickActions(GuiGraphics graphics,
                                    int mouseX,
                                    int mouseY,
                                    boolean tooltipPass) {
        int visibleCount = getVisibleQuickActionCount();
        if (!fullInventory || visibleCount == 0) {
            return;
        }

        QuickActionManager manager = QuickActionManager.getInstance();
        for (int slot = 0; slot < visibleCount; slot++) {
            int x = getQuickActionX(slot);
            int y = getQuickActionY(slot);
            boolean hovered = isOverQuickAction(mouseX, mouseY, slot);

            if (tooltipPass) {
                if (hovered) {
                    Component editHint = manager.isConfigured(slot)
                            ? Component.literal("Right-click or Shift-click to edit")
                                    .withStyle(ChatFormatting.GRAY)
                            : Component.literal("Choose a keybind or command")
                                    .withStyle(ChatFormatting.GRAY);
                    graphics.renderTooltip(
                            font,
                            List.of(manager.getTooltip(slot), editHint),
                            Optional.empty(),
                            mouseX,
                            mouseY
                    );
                }
                continue;
            }

            int background = hovered ? 0xE04B4B4B : 0xE02B2B2B;
            int border = hovered ? 0xFFFFFFFF : 0xFF9A9A9A;
            graphics.fill(x, y, x + QUICK_ACTION_SIZE, y + QUICK_ACTION_SIZE, background);
            graphics.fill(x, y, x + QUICK_ACTION_SIZE, y + 1, border);
            graphics.fill(
                    x,
                    y + QUICK_ACTION_SIZE - 1,
                    x + QUICK_ACTION_SIZE,
                    y + QUICK_ACTION_SIZE,
                    border
            );
            graphics.fill(x, y, x + 1, y + QUICK_ACTION_SIZE, border);
            graphics.fill(
                    x + QUICK_ACTION_SIZE - 1,
                    y,
                    x + QUICK_ACTION_SIZE,
                    y + QUICK_ACTION_SIZE,
                    border
            );

            ItemStack icon = manager.getIconStack(slot);
            if (icon.isEmpty()) {
                graphics.drawCenteredString(
                        font,
                        "+",
                        x + QUICK_ACTION_SIZE / 2,
                        y + (QUICK_ACTION_SIZE - 8) / 2,
                        0xFFFFFFFF
                );
            } else {
                graphics.renderItem(icon, x + 2, y + 2);
            }
        }
    }

    private int getVisibleQuickActionCount() {
        QuickActionDisplayMode mode = WristUiConfig.getInstance().quickActionDisplayMode();
        return Math.min(mode.visibleSlots(), QuickActionManager.SLOT_COUNT);
    }

    private int getQuickActionX(int slot) {
        int offset = switch (slot / 3) {
            case 0 -> QUICK_ACTION_LEFT_X;
            case 1 -> QUICK_ACTION_RIGHT_X;
            case 2 -> QUICK_ACTION_OUTER_LEFT_X;
            case 3 -> QUICK_ACTION_OUTER_RIGHT_X;
            default -> throw new IndexOutOfBoundsException("Quick action slot: " + slot);
        };
        return leftPos + offset;
    }

    private int getQuickActionY(int slot) {
        int row = slot % 3;
        return topPos + QUICK_ACTION_FIRST_Y
                + row * (QUICK_ACTION_SIZE + QUICK_ACTION_GAP);
    }

    private boolean isOverQuickAction(double mouseX,
                                      double mouseY,
                                      int slot) {
        int x = getQuickActionX(slot);
        int y = getQuickActionY(slot);
        return mouseX >= x && mouseX < x + QUICK_ACTION_SIZE
                && mouseY >= y && mouseY < y + QUICK_ACTION_SIZE;
    }

    private void clickQuickAction(int slot, boolean edit) {
        QuickActionManager manager = QuickActionManager.getInstance();
        playQuickActionClick();
        if (edit || !manager.isConfigured(slot)) {
            QuickActionEditorScreen.open(slot);
        } else {
            manager.execute(slot);
        }
    }

    private void playQuickActionClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }

    /** Renders the configurable, always-live HUD around the wrist inventory. */
    private void renderStatusStrip(GuiGraphics graphics,
                                   int mouseX,
                                   int mouseY,
                                   boolean tooltipPass) {
        if (!fullInventory || minecraft.player == null) {
            return;
        }

        StatusLayout layout = buildStatusLayout();
        if (layout.groups().isEmpty()) {
            return;
        }
        StatusSnapshot snapshot = getStatusSnapshot();

        if (tooltipPass) {
            StatusCell hovered = getHoveredStatusCell(layout, mouseX, mouseY);
            if (hovered != null) {
                graphics.renderTooltip(
                        font,
                        getStatusTooltip(hovered.metric(), snapshot),
                        Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
            return;
        }

        for (StatusGroup group : layout.groups()) {
            drawStatusGroupBackground(graphics, group);
        }

        for (StatusCell cell : layout.cells()) {
            drawVanillaHorizontalMetric(graphics, cell, snapshot);
        }
    }

    private StatusSnapshot getStatusSnapshot() {
        var player = minecraft.player;
        float maximumHealth = finiteNonNegative(player.getMaxHealth());
        float health = Math.min(finiteNonNegative(player.getHealth()), maximumHealth);
        float absorption = finiteNonNegative(player.getAbsorptionAmount());
        int hunger = Math.max(0, Math.min(20, player.getFoodData().getFoodLevel()));
        float saturation = Math.max(
                0.0F,
                Math.min(20.0F, finiteNonNegative(player.getFoodData().getSaturationLevel()))
        );
        return new StatusSnapshot(
                maximumHealth,
                health,
                absorption,
                hunger,
                saturation,
                Math.max(0, player.getArmorValue()),
                Math.max(0, player.experienceLevel),
                clampRatio(player.experienceProgress)
        );
    }

    private List<Component> getStatusTooltip(WristHudMetric metric,
                                             StatusSnapshot snapshot) {
        return switch (metric) {
            case HEALTH -> snapshot.absorption() > 0.0F
                    ? List.of(
                            Component.literal("Health: " + formatAmount(snapshot.health())
                                    + " / " + formatAmount(snapshot.maximumHealth())),
                            Component.literal("Absorption: +"
                                            + formatAmount(snapshot.absorption()))
                                    .withStyle(ChatFormatting.GOLD)
                    )
                    : List.of(Component.literal(
                            "Health: " + formatAmount(snapshot.health())
                                    + " / " + formatAmount(snapshot.maximumHealth())
                    ));
            case HUNGER -> List.of(
                    Component.literal("Hunger: " + snapshot.hunger() + " / 20"),
                    Component.literal("Saturation: " + formatAmount(snapshot.saturation()))
                            .withStyle(ChatFormatting.GOLD)
            );
            case ARMOR -> List.of(Component.literal("Armor: " + snapshot.armor()));
            case EXPERIENCE -> List.of(Component.literal(
                    "Experience: Level " + snapshot.experienceLevel() + " ("
                            + Math.round(snapshot.experienceProgress() * 100.0F) + "%)"
            ));
        };
    }

    private StatusLayout buildStatusLayout() {
        WristUiConfig config = WristUiConfig.getInstance();
        List<StatusGroup> groups = new ArrayList<>();
        List<StatusCell> cells = new ArrayList<>();

        for (WristHudPosition position : WristHudPosition.values()) {
            List<WristHudMetric> metrics = new ArrayList<>();
            for (WristHudMetric metric : WristHudMetric.values()) {
                if (config.isVisible(metric) && config.position(metric) == position) {
                    metrics.add(metric);
                }
            }
            if (metrics.isEmpty()) {
                continue;
            }

            StatusGroup group = buildHorizontalStatusGroup(position, metrics);
            groups.add(group);
            cells.addAll(group.cells());
        }
        return new StatusLayout(List.copyOf(groups), List.copyOf(cells));
    }

    private StatusGroup buildHorizontalStatusGroup(WristHudPosition position,
                                                   List<WristHudMetric> metrics) {
        int columns = Math.min(2, metrics.size());
        int rows = (metrics.size() + 1) / 2;
        int width = columns * STATUS_METRIC_WIDTH;
        int height = rows * STATUS_METRIC_HEIGHT + 1;
        int x = leftPos + STATUS_STRIP_X + (STATUS_STRIP_WIDTH - width) / 2;
        int y = position == WristHudPosition.BOTTOM
                ? topPos + imageHeight - 1
                : topPos - height + 1;

        List<StatusCell> cells = new ArrayList<>();
        for (int index = 0; index < metrics.size(); index++) {
            int column = index % 2;
            int row = index / 2;
            boolean centeredLastCell = columns == 2
                    && index == metrics.size() - 1
                    && metrics.size() % 2 != 0;
            int cellX = centeredLastCell
                    ? x + (width - STATUS_METRIC_WIDTH) / 2
                    : x + column * STATUS_METRIC_WIDTH;
            cells.add(new StatusCell(
                    metrics.get(index),
                    new InventoryBounds(
                            cellX,
                            y + 1 + row * STATUS_METRIC_HEIGHT,
                            STATUS_METRIC_WIDTH,
                            STATUS_METRIC_HEIGHT
                    )
            ));
        }
        return new StatusGroup(
                position,
                new InventoryBounds(x, y, width, height),
                columns,
                rows,
                List.copyOf(cells)
        );
    }

    private void drawStatusGroupBackground(GuiGraphics graphics, StatusGroup group) {
        InventoryBounds bounds = group.bounds();
        int x = bounds.x();
        int y = bounds.y();
        int width = bounds.width();
        int height = bounds.height();
        int border = 0xFF9A9A9A;

        graphics.fill(x, y, x + width, y + height, 0xEC171717);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);

        for (int row = 0; row < group.rows(); row++) {
            int cellsInRow = Math.min(
                    group.columns(),
                    group.cells().size() - row * group.columns()
            );
            if (cellsInRow > 1) {
                int separatorX = x + STATUS_METRIC_WIDTH;
                int separatorTop = y + 1 + row * STATUS_METRIC_HEIGHT;
                int separatorBottom = y + (row + 1) * STATUS_METRIC_HEIGHT;
                graphics.fill(
                        separatorX,
                        separatorTop,
                        separatorX + 1,
                        separatorBottom,
                        0xFF505050
                );
            }
        }
        for (int row = 1; row < group.rows(); row++) {
            int separatorY = y + row * STATUS_METRIC_HEIGHT;
            graphics.fill(x + 1, separatorY, x + width - 1,
                    separatorY + 1, 0xFF505050);
        }
    }

    private void drawVanillaHorizontalMetric(GuiGraphics graphics,
                                             StatusCell cell,
                                             StatusSnapshot snapshot) {
        InventoryBounds bounds = cell.bounds();
        if (cell.metric() == WristHudMetric.EXPERIENCE) {
            String level = Integer.toString(snapshot.experienceLevel());
            graphics.drawCenteredString(font, level,
                    bounds.x() + bounds.width() / 2, bounds.y() + 1, 0xFF80FF20);
            drawVanillaExperienceBar(
                    graphics,
                    bounds.x() + 3,
                    bounds.y() + 10,
                    81,
                    snapshot.experienceProgress()
            );
            return;
        }
        drawVanillaMetricIcons(
                graphics,
                cell.metric(),
                snapshot,
                bounds.x() + 3,
                bounds.y() + 2
        );
    }

    private void drawVanillaMetricIcons(GuiGraphics graphics,
                                        WristHudMetric metric,
                                        StatusSnapshot snapshot,
                                        int x,
                                        int y) {
        ResourceLocation empty = switch (metric) {
            case HEALTH -> HEALTH_CONTAINER_ICON;
            case HUNGER -> HUNGER_EMPTY_ICON;
            case ARMOR -> ARMOR_EMPTY_ICON;
            case EXPERIENCE -> throw new IllegalArgumentException("Experience uses a bar");
        };
        ResourceLocation half = switch (metric) {
            case HEALTH -> HEALTH_HALF_ICON;
            case HUNGER -> HUNGER_HALF_ICON;
            case ARMOR -> ARMOR_HALF_ICON;
            case EXPERIENCE -> throw new IllegalArgumentException("Experience uses a bar");
        };
        ResourceLocation full = switch (metric) {
            case HEALTH -> HEALTH_ICON;
            case HUNGER -> HUNGER_ICON;
            case ARMOR -> ARMOR_ICON;
            case EXPERIENCE -> throw new IllegalArgumentException("Experience uses a bar");
        };
        float healthDisplayCapacity = Math.max(
                snapshot.maximumHealth(),
                snapshot.health() + snapshot.absorption()
        );
        int units = switch (metric) {
            case HEALTH -> Math.round(
                    safeRatio(snapshot.health(), healthDisplayCapacity) * 20.0F
            );
            case HUNGER -> snapshot.hunger();
            case ARMOR -> Math.min(snapshot.armor(), 20);
            case EXPERIENCE -> 0;
        };
        int absorptionUnits = metric == WristHudMetric.HEALTH
                && snapshot.absorption() > 0.0F
                ? Math.max(
                        1,
                        Math.round(
                                safeRatio(
                                        snapshot.absorption(),
                                        healthDisplayCapacity
                                ) * 20.0F
                        )
                )
                : 0;
        // A rounded 19- or 20-unit health display leaves no complete icon
        // slot for absorption. Reserve the last slot so even a small shield is
        // visible instead of silently disappearing at full health.
        if (absorptionUnits > 0 && units > 18) {
            units = 18;
        }
        absorptionUnits = Math.min(20 - units, absorptionUnits);

        boolean compatibilityRendererHandled = false;
        if (metric == WristHudMetric.HEALTH && minecraft.player != null) {
            compatibilityRendererHandled = ColorfulHeartsCompat.drawHorizontalHealth(
                    graphics,
                    minecraft.player,
                    x,
                    y
            );
        } else if (metric == WristHudMetric.ARMOR) {
            compatibilityRendererHandled = OverloadedArmorBarCompat.drawHorizontalArmor(
                    graphics,
                    snapshot.armor(),
                    x,
                    y
            );
        }

        if (!compatibilityRendererHandled) {
            for (int slot = 0; slot < 10; slot++) {
                drawVanillaHudSprite(graphics, empty, x, y, slot);
                int threshold = slot * 2;
                if (units >= threshold + 2) {
                    drawVanillaHudSprite(graphics, full, x, y, slot);
                } else if (units == threshold + 1) {
                    drawVanillaHudSprite(graphics, half, x, y, slot);
                }
            }
        }

        if (absorptionUnits > 0) {
            int slot = Math.min(10, (units + 1) / 2);
            while (absorptionUnits > 0 && slot < 10) {
                ResourceLocation sprite = absorptionUnits >= 2
                        ? ABSORPTION_FULL_ICON
                        : ABSORPTION_HALF_ICON;
                drawVanillaHudSprite(graphics, sprite, x, y, slot);
                absorptionUnits -= 2;
                slot++;
            }
        }
    }

    private void drawVanillaHudSprite(GuiGraphics graphics,
                                      ResourceLocation sprite,
                                      int x,
                                      int y,
                                      int slot) {
        graphics.blitSprite(sprite, x + slot * 8, y, 9, 9);
    }

    private void drawVanillaExperienceBar(GuiGraphics graphics,
                                          int x,
                                          int y,
                                          int width,
                                          float progress) {
        graphics.blitSprite(EXPERIENCE_BACKGROUND, x, y, width, 5);
        int progressWidth = Math.round(clampRatio(progress) * width);
        if (progressWidth > 0) {
            graphics.blitSprite(
                    EXPERIENCE_PROGRESS,
                    width,
                    5,
                    0,
                    0,
                    x,
                    y,
                    progressWidth,
                    5
            );
        }
    }

    private @Nullable StatusCell getHoveredStatusCell(StatusLayout layout,
                                                      double mouseX,
                                                      double mouseY) {
        for (StatusCell cell : layout.cells()) {
            if (contains(cell.bounds(), mouseX, mouseY)) {
                return cell;
            }
        }
        return null;
    }

    private boolean isOverStatusStrip(double mouseX, double mouseY) {
        if (!fullInventory) {
            return false;
        }
        for (StatusGroup group : buildStatusLayout().groups()) {
            if (contains(group.bounds(), mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(InventoryBounds bounds,
                                    double mouseX,
                                    double mouseY) {
        return mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width()
                && mouseY >= bounds.y() && mouseY < bounds.y() + bounds.height();
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    private static float safeRatio(float value, float maximum) {
        if (!Float.isFinite(maximum) || maximum <= 0.0F) {
            return 0.0F;
        }
        return clampRatio(value / maximum);
    }

    private static float clampRatio(float ratio) {
        if (!Float.isFinite(ratio)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, ratio));
    }

    private static String formatAmount(float value) {
        float safeValue = finiteNonNegative(value);
        int rounded = Math.round(safeValue);
        if (Math.abs(safeValue - rounded) < 0.05F) {
            return Integer.toString(rounded);
        }
        return Float.toString(Math.round(safeValue * 10.0F) / 10.0F);
    }

    private void renderContextAction(GuiGraphics graphics,
                                     int mouseX,
                                     int mouseY) {
        if (contextActionLabel == null || contextAction == null) {
            return;
        }

        int x = getContextActionX();
        int y = getContextActionY();
        boolean hovered = isOverContextAction(mouseX, mouseY);
        boolean enabled = menu.getCarried().isEmpty();
        int background = !enabled
                ? 0xB0181818
                : hovered ? 0xE0434343 : 0xD02B2B2B;
        int border = !enabled
                ? 0xFF555555
                : hovered ? 0xFFFFFFFF : 0xFFA9A9A9;
        Component label = enabled
                ? contextActionLabel
                : Component.literal("Place Held Item First");

        graphics.fill(x, y, x + CONTEXT_ACTION_WIDTH,
                y + CONTEXT_ACTION_HEIGHT, background);
        graphics.fill(x, y, x + 4, y + CONTEXT_ACTION_HEIGHT, 0xFFE84A52);
        graphics.fill(x, y, x + CONTEXT_ACTION_WIDTH, y + 1, border);
        graphics.fill(x, y + CONTEXT_ACTION_HEIGHT - 1,
                x + CONTEXT_ACTION_WIDTH, y + CONTEXT_ACTION_HEIGHT, border);
        graphics.fill(x, y, x + 1, y + CONTEXT_ACTION_HEIGHT, border);
        graphics.fill(x + CONTEXT_ACTION_WIDTH - 1, y,
                x + CONTEXT_ACTION_WIDTH, y + CONTEXT_ACTION_HEIGHT, border);
        graphics.drawCenteredString(
                font,
                label,
                x + CONTEXT_ACTION_WIDTH / 2,
                y + (CONTEXT_ACTION_HEIGHT - 8) / 2,
                enabled ? 0xFFFFFFFF : 0xFF888888
        );
    }

    private int getContextActionX() {
        return leftPos + (imageWidth - CONTEXT_ACTION_WIDTH) / 2;
    }

    private int getContextActionY() {
        if (fullInventory) {
            int lowerEdge = topPos + imageHeight;
            for (StatusGroup group : buildStatusLayout().groups()) {
                if (group.position() == WristHudPosition.BOTTOM) {
                    lowerEdge = Math.max(
                            lowerEdge,
                            group.bounds().y() + group.bounds().height()
                    );
                }
            }
            return lowerEdge + CONTEXT_ACTION_GAP;
        }
        return topPos + imageHeight + CONTEXT_ACTION_GAP;
    }

    private boolean isOverContextAction(double mouseX, double mouseY) {
        int x = getContextActionX();
        int y = getContextActionY();
        return mouseX >= x && mouseX < x + CONTEXT_ACTION_WIDTH
                && mouseY >= y && mouseY < y + CONTEXT_ACTION_HEIGHT;
    }

    /**
     * FastWorkbench only applies its client crafting-result payload when a
     * normal inventory screen is open. This wrist inventory deliberately runs
     * with Minecraft.screen == null, so its result slot can remain visually
     * empty even though the server accepts clicks and crafts the item. Prefer
     * the real result when available, otherwise render a cached client recipe
     * preview after the container pass at the final wrist-panel coordinates.
     */
    private void renderCraftingResult(GuiGraphics guiGraphics) {
        if (!fullInventory) {
            return;
        }

        ItemStack result = ItemStack.EMPTY;
        for (Slot slot : menu.slots) {
            if (slot instanceof ResultSlot) {
                result = slot.getItem();
                break;
            }
        }

        // Compute a visual-only fallback for the payload FastWorkbench drops
        // while this off-screen menu is active. The real click still goes
        // through the menu and remains server-authoritative.
        if (result.isEmpty()
                && menu instanceof InventoryMenu inventoryMenu
                && minecraft.level != null) {
            CraftingInput input = inventoryMenu.getCraftSlots().asCraftInput();
            if (!input.equals(cachedCraftingInput)) {
                cachedCraftingInput = input;
                var recipe = minecraft.level.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, minecraft.level);
                cachedCraftingResult = recipe
                        .map(holder -> holder.value().assemble(
                                input,
                                minecraft.level.registryAccess()
                        ))
                        .orElse(ItemStack.EMPTY);
            }
            result = cachedCraftingResult;
        }

        if (result.isEmpty()) {
            return;
        }

        int x = leftPos + CRAFT_RESULT_X;
        int y = topPos + CRAFT_RESULT_Y;

        Lighting.setupFor3DItems();
        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.renderItem(result, x, y);
            guiGraphics.renderItemDecorations(font, result, x, y, null);
            guiGraphics.flush();
        } finally {
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    /**
     * The result is deliberately drawn after the inherited container pass by
     * {@link #renderCraftingResult(GuiGraphics)}. Suppress the inherited slot
     * draw so the output has one deterministic rendering path.
     */
    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (fullInventory && slot instanceof ResultSlot) {
            return;
        }
        super.renderSlot(guiGraphics, slot);
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;

        guiGraphics.blit(fullInventory
                        ? IMAGE_FULL : IMAGE_SIMPLIFIED,
                width/2-258/2, height/2-156/2,
                0, 0.0F, 0.0F,
                258, 156,
                258,156
        );

        if(fullInventory) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    i + 26,
                    j + 8,
                    i + 75,
                    j + 78,
                    30,
                    0.0625F,
                    this.xMouse,
                    this.yMouse,
                    this.minecraft.player
            );
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && isCreativeModeActionVisible()
                && isOverCreativeModeAction(mouseX, mouseY)) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
            creativeModeAction.run();
            return true;
        }

        int visibleQuickActions = getVisibleQuickActionCount();
        if (fullInventory
                && visibleQuickActions > 0
                && (button == 0 || button == 1)) {
            for (int slot = 0; slot < visibleQuickActions; slot++) {
                if (isOverQuickAction(mouseX, mouseY, slot)) {
                    boolean edit = button == 1 || Screen.hasShiftDown();
                    clickQuickAction(slot, edit);
                    return true;
                }
            }
        }
        if (button == 0) {
            List<UtilityAction> visibleActions = visibleUtilityActions();
            for (int index = 0; index < visibleActions.size(); index++) {
                if (isOverUtilityAction(mouseX, mouseY, index)) {
                    visibleActions.get(index).action().run();
                    return true;
                }
            }
        }
        if (button == 0
                && contextAction != null
                && isOverContextAction(mouseX, mouseY)) {
            if (menu.getCarried().isEmpty()) {
                contextAction.run();
            }
            return true;
        }
        // The status footer extends beyond AbstractContainerScreen.imageHeight.
        // Consume clicks there so vanilla can never interpret a harmless HUD
        // press as an outside-inventory click and throw a carried stack.
        if (isOverStatusStrip(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * This nested inventory intentionally defines no desktop-style widgets.
     * NeoForge screen-init hooks still inject global modpack buttons such as
     * Dark Mode and the FTB sidebar, so remove those after public init() has
     * finished posting its pre/post events.
     */
    public void removeInjectedWidgets() {
        var injectedWidgets = children().stream()
                .filter(AbstractWidget.class::isInstance)
                .toList();
        injectedWidgets.forEach(this::removeWidget);
    }

    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        InventoryBounds bounds = visorEssentials$getCombinedBounds();
        return mouseX < bounds.x()
                || mouseY < bounds.y()
                || mouseX >= bounds.x() + bounds.width()
                || mouseY >= bounds.y() + bounds.height();
    }



    @Override
    public int visorEssentials$getEdgeX() {
        return visorEssentials$getCombinedBounds().x();
    }

    @Override
    public int visorEssentials$getEdgeY() {
        return visorEssentials$getCombinedBounds().y();
    }

    @Override
    public int visorEssentials$getEdgeWidth() {
        return visorEssentials$getCombinedBounds().width();
    }

    @Override
    public int visorEssentials$getEdgeHeight() {
        return visorEssentials$getCombinedBounds().height();
    }

    /**
     * Cursor focus must cover exactly the same rectangle as the visible wrist
     * inventory. Include the dynamically sized status-effect strip instead of
     * relying on render state from the previous frame.
     */
    private InventoryBounds visorEssentials$getCombinedBounds() {
        int inventoryX = fullInventory ? leftPos : leftPos + 40;
        int inventoryY = topPos;
        int inventoryWidth = fullInventory ? imageWidth : imageWidth - 80;
        int inventoryHeight = imageHeight;

        int minX = inventoryX;
        int minY = inventoryY;
        int maxX = inventoryX + inventoryWidth;
        int maxY = inventoryY + inventoryHeight;

        EffectBounds effects = visorEssentials$getEffectBounds();
        if (effects != null) {
            minX = Math.min(minX, effects.x());
            minY = Math.min(minY, effects.y());
            maxX = Math.max(maxX, effects.x() + effects.width());
            maxY = Math.max(maxY, effects.y() + effects.height());
        }

        if (contextActionLabel != null && contextAction != null) {
            int actionX = getContextActionX();
            int actionY = getContextActionY();
            minX = Math.min(minX, actionX);
            minY = Math.min(minY, actionY);
            maxX = Math.max(maxX, actionX + CONTEXT_ACTION_WIDTH);
            maxY = Math.max(maxY, actionY + CONTEXT_ACTION_HEIGHT);
        }
        if (isCreativeModeActionVisible()) {
            int actionX = getCreativeModeActionX();
            int actionY = getCreativeModeActionY();
            minX = Math.min(minX, actionX);
            minY = Math.min(minY, actionY);
            maxX = Math.max(maxX, actionX + CREATIVE_MODE_ACTION_WIDTH);
            maxY = Math.max(maxY, actionY + CREATIVE_MODE_ACTION_HEIGHT);
        }
        if (fullInventory) {
            int visibleQuickActions = getVisibleQuickActionCount();
            if (visibleQuickActions > 0) {
                for (int slot = 0; slot < visibleQuickActions; slot++) {
                    int actionX = getQuickActionX(slot);
                    int actionY = getQuickActionY(slot);
                    minX = Math.min(minX, actionX);
                    minY = Math.min(minY, actionY);
                    maxX = Math.max(maxX, actionX + QUICK_ACTION_SIZE);
                    maxY = Math.max(maxY, actionY + QUICK_ACTION_SIZE);
                }
            }

            for (StatusGroup group : buildStatusLayout().groups()) {
                InventoryBounds statusBounds = group.bounds();
                minX = Math.min(minX, statusBounds.x());
                minY = Math.min(minY, statusBounds.y());
                maxX = Math.max(maxX, statusBounds.x() + statusBounds.width());
                maxY = Math.max(maxY, statusBounds.y() + statusBounds.height());
            }
        }
        return new InventoryBounds(minX, minY, maxX - minX, maxY - minY);
    }

    private record InventoryBounds(int x, int y, int width, int height) {}

    private record StatusCell(WristHudMetric metric,
                              InventoryBounds bounds) {}

    private record StatusGroup(WristHudPosition position,
                               InventoryBounds bounds,
                               int columns,
                               int rows,
                               List<StatusCell> cells) {}

    private record StatusLayout(List<StatusGroup> groups,
                                List<StatusCell> cells) {}

    private record StatusSnapshot(float maximumHealth,
                                  float health,
                                  float absorption,
                                  int hunger,
                                  float saturation,
                                  int armor,
                                  int experienceLevel,
                                  float experienceProgress) {}

    private record UtilityAction(ItemStack icon,
                                 Component tooltip,
                                 BooleanSupplier visible,
                                 BooleanSupplier toggled,
                                 Runnable action) {
    }






}
