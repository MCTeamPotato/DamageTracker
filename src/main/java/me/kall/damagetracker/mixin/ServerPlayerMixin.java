package me.kall.damagetracker.mixin;

import me.kall.damagetracker.DamageTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "hurt", at = @At("RETURN"))
    private void onHurt(DamageSource source, float amount, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            ServerPlayer player = (ServerPlayer)(Object) this;
            player.server.execute(() -> DamageTracker.update(source, amount, player.getUUID()));
        }
    }
}
