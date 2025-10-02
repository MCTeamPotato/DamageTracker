package me.kall.damagetracker.mixin;

import com.google.common.collect.Lists;
import me.kall.damagetracker.DamageTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    @Shadow @Final private List<Button> exitButtons;

    @Unique private Button death$reasonButton;
    @Unique private List<DamageTracker.TimestampedDamage> death$reasons;

    protected DeathScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.death$reasons = Lists.newArrayList(DamageTracker.getDamages(this.minecraft.player.getUUID()));
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addButton(CallbackInfo ci) {
        if (!this.exitButtons.isEmpty()) {
            Button respawnButton = this.exitButtons.get(0);

            this.death$reasonButton = Button.builder(Component.translatable("button.damage_tracker"), button -> {}).bounds(respawnButton.getX(), respawnButton.getY() - 24, respawnButton.getWidth(), 20).build();
            this.death$reasonButton.active = false;
            this.addRenderableWidget(this.death$reasonButton);
        }
    }

    @Inject(method = "setButtonsActive", at = @At("HEAD"))
    private void onActivate(boolean active, CallbackInfo ci) {
        if (this.death$reasonButton == null) return;
        this.death$reasonButton.active = active;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderDeathReasons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.death$reasonButton != null && this.death$reasonButton.isHovered()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                if (this.death$reasons == null) this.death$reasons = Lists.newArrayList(DamageTracker.getDamages(this.minecraft.player.getUUID()));
                int startX = this.width - 150;
                int startY = 60;
                int lineHeight = 10;

                int i = 0;
                for (DamageTracker.TimestampedDamage dmg : this.death$reasons) {
                    String text = String.format("%s: %.1f", dmg.getDisplayText().getString(), dmg.amount());
                    guiGraphics.drawString(this.font, text, startX, startY + i * lineHeight, 0xFFFFFF, false);
                    i++;
                }

                if (i == 0) {
                    guiGraphics.drawString(this.font, Component.translatable("info.damage_tracker"), startX, startY, 0XAAAAAA, false);
                }
            }
        }
    }
}
