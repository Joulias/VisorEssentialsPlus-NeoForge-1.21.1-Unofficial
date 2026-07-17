package org.vmstudio.essentials.core.platform.neoforge;

import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Respects NeoForge extensions which hide effects from inventory screens.
 */
public final class ClientEffectHooksImpl {
    private ClientEffectHooksImpl() {}

    public static boolean shouldRenderEffect(MobEffectInstance effect) {
        return ClientHooks.shouldRenderEffect(effect);
    }
}
