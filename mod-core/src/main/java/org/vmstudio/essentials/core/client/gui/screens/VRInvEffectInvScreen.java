package org.vmstudio.essentials.core.client.gui.screens;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.essentials.core.client.config.WristUiConfig;
import org.vmstudio.essentials.core.platform.ClientEffectHooks;

import java.util.List;
import java.util.Optional;

public abstract class VRInvEffectInvScreen extends AbstractContainerScreen<AbstractContainerMenu> {

    private static final ResourceLocation EFFECT_BACKGROUND_LARGE_SPRITE =
            ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation EFFECT_BACKGROUND_SMALL_SPRITE =
            ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");

    @Getter
    protected boolean fullInventory;

    public VRInvEffectInvScreen(AbstractContainerMenu menu,
                                Inventory inventory,
                                Component component) {
        super(menu, inventory, component);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderEffects(guiGraphics, mouseX, mouseY);
    }

    private void renderEffects(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<MobEffectInstance> effects = visorEssentials$getVisibleEffects();
        EffectBounds effectBounds = visorEssentials$getEffectBounds(effects);
        if (effectBounds == null) {
            return;
        }

        int startPos = effectBounds.x();
        boolean large = effectBounds.width() == 120;
        int rowSpacing = effects.size() > 5
                ? 132 / (effects.size() - 1)
                : 33;

        this.renderBackgrounds(guiGraphics, startPos, rowSpacing, effects, large);
        this.renderIcons(guiGraphics, startPos, rowSpacing, effects, large);
        if (large) {
            this.renderLabels(guiGraphics, startPos, rowSpacing, effects);
        } else if (mouseX >= startPos && mouseX <= startPos + 32) {
            int effectY = this.topPos;
            MobEffectInstance hoveredEffect = null;

            for (MobEffectInstance effect : effects) {
                if (mouseY >= effectY && mouseY <= effectY + rowSpacing) {
                    hoveredEffect = effect;
                }

                effectY += rowSpacing;
            }

            if (hoveredEffect != null) {
                List<Component> tooltip = List.of(
                        this.getEffectName(hoveredEffect),
                        MobEffectUtil.formatDuration(
                                hoveredEffect,
                                1.0F,
                                visorEssentials$getTickRate()
                        )
                );
                guiGraphics.renderTooltip(
                        this.font,
                        tooltip,
                        Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    /**
     * Returns the exact status-effect rectangle rendered beside the wrist
     * inventory. Computing this from current state keeps cursor focus bounds
     * in sync even before the next render pass.
     */
    protected @Nullable EffectBounds visorEssentials$getEffectBounds() {
        return visorEssentials$getEffectBounds(
                visorEssentials$getVisibleEffects()
        );
    }

    private @Nullable EffectBounds visorEssentials$getEffectBounds(
            List<MobEffectInstance> effects
    ) {
        if (effects.isEmpty()) {
            return null;
        }

        // The simplified inventory is 80 px narrower, so bring the effects
        // 36 px left to sit just beyond its visible right edge.
        int startPos = this.leftPos + this.imageWidth + 2
                - (fullInventory ? 0 : 36);
        int availableWidth = this.width - startPos;
        if (availableWidth < 32) {
            return null;
        }

        int effectWidth = availableWidth >= 120 ? 120 : 32;
        int rowSpacing = effects.size() > 5
                ? 132 / (effects.size() - 1)
                : 33;
        int effectHeight = 32 + (effects.size() - 1) * rowSpacing;
        return new EffectBounds(startPos, this.topPos, effectWidth, effectHeight);
    }

    private List<MobEffectInstance> visorEssentials$getVisibleEffects() {
        if (!WristUiConfig.getInstance().showPotionEffects()
                || this.minecraft == null
                || this.minecraft.player == null) {
            return List.of();
        }

        return this.minecraft.player.getActiveEffects().stream()
                .filter(ClientEffectHooks::shouldRenderEffect)
                .sorted()
                .toList();
    }

    //---Nothing modified below
    private void renderBackgrounds(GuiGraphics guiGraphics, int renderX, int yOffset, Iterable<MobEffectInstance> effects, boolean large) {
        int i = this.topPos;

        for(MobEffectInstance mobEffectInstance : effects) {
            if (large) {
                guiGraphics.blitSprite(EFFECT_BACKGROUND_LARGE_SPRITE, renderX, i, 120, 32);
            } else {
                guiGraphics.blitSprite(EFFECT_BACKGROUND_SMALL_SPRITE, renderX, i, 32, 32);
            }

            i += yOffset;
        }

    }

    private void renderIcons(GuiGraphics guiGraphics, int renderX, int yOffset, Iterable<MobEffectInstance> effects, boolean isSmall) {
        MobEffectTextureManager mobEffectTextureManager = this.minecraft.getMobEffectTextures();
        int i = this.topPos;

        for(MobEffectInstance mobEffectInstance : effects) {
            TextureAtlasSprite textureAtlasSprite = mobEffectTextureManager.get(mobEffectInstance.getEffect());
            guiGraphics.blit(renderX + (isSmall ? 6 : 7), i + 7, 0, 18, 18, textureAtlasSprite);
            i += yOffset;
        }

    }

    private void renderLabels(GuiGraphics guiGraphics, int renderX, int yOffset, Iterable<MobEffectInstance> effects) {
        int i = this.topPos;

        for(MobEffectInstance mobEffectInstance : effects) {
            Component component = this.getEffectName(mobEffectInstance);
            guiGraphics.drawString(this.font, component, renderX + 10 + 18, i + 6, 16777215);
            Component component2 = MobEffectUtil.formatDuration(
                    mobEffectInstance,
                    1.0F,
                    visorEssentials$getTickRate()
            );
            guiGraphics.drawString(this.font, component2, renderX + 10 + 18, i + 6 + 10, 8355711);
            i += yOffset;
        }

    }

    private Component getEffectName(MobEffectInstance effect) {
        MutableComponent mutableComponent = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
            MutableComponent var10000 = mutableComponent.append(CommonComponents.SPACE);
            int var10001 = effect.getAmplifier();
            var10000.append(Component.translatable("enchantment.level." + (var10001 + 1)));
        }

        return mutableComponent;
    }

    private float visorEssentials$getTickRate() {
        return this.minecraft != null && this.minecraft.level != null
                ? this.minecraft.level.tickRateManager().tickrate()
                : 20.0F;
    }

    protected record EffectBounds(int x, int y, int width, int height) {}
}
