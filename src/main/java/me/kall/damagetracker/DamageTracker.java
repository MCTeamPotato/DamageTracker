package me.kall.damagetracker;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Mod(DamageTracker.MOD_ID)
public final class DamageTracker {
    public static final String MOD_ID = "damagetracker";

    public static final Map<UUID, List<TimestampedDamage>> DAMAGES = new Object2ObjectOpenHashMap<>();

    private static final long OUTDATED = 10000L;
    private static long lastClean = 0L;

    private static final Runnable CLEAN = () -> {
        if (now() - lastClean >= OUTDATED) {
            Iterator<Map.Entry<UUID, List<TimestampedDamage>>> iterator = DAMAGES.entrySet().iterator();
            while (iterator.hasNext()) {
                List<TimestampedDamage> damages = iterator.next().getValue();
                damages.removeIf(TimestampedDamage::isOutdated);
                if (damages.isEmpty()) iterator.remove();
            }
            lastClean = now();
        }
    };

    public DamageTracker(IEventBus modBus, Dist dist, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(DamageTracker::onTick);
    }

    public static void update(@NotNull DamageSource damageSource, float amount, UUID victimUuid) {
        UUID attackerUuid = null;
        UUID directUuid = null;

        if (damageSource.getEntity() != null) {
            attackerUuid = damageSource.getEntity().getUUID();
        }
        if (damageSource.getDirectEntity() != null) {
            directUuid = damageSource.getDirectEntity().getUUID();
        }

        DAMAGES.computeIfAbsent(victimUuid, key -> new ObjectArrayList<>()).add(new TimestampedDamage(damageSource, amount, now(), attackerUuid, directUuid));
    }

    public static @NotNull Iterator<TimestampedDamage> getDamages(UUID uuid) {
        List<TimestampedDamage> damages = DAMAGES.get(uuid);
        if (damages == null) return Collections.emptyIterator();

        Set<UUID> seen = new ObjectOpenHashSet<>();

        return Iterators.filter(damages.iterator(), dmg -> {
            if (dmg.isOutdated()) return false;

            UUID attacker = dmg.attackerUuid() != null ? dmg.attackerUuid() : dmg.directUuid();
            if (attacker == null) return true;

            return seen.add(attacker);
        });
    }

    public static void onTick(ServerTickEvent.@NotNull Pre event) {
        event.getServer().execute(() -> {
            if (!DAMAGES.isEmpty()) CLEAN.run();
        });
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public record TimestampedDamage(DamageSource damageSource, float amount, long timestamp, @Nullable UUID attackerUuid, @Nullable UUID directUuid) {
        public boolean isOutdated() {
            return now() - this.timestamp >= OUTDATED;
        }

        public Component getDisplayText() {
            if (damageSource.getEntity() != null) {
                Component attacker = damageSource.getEntity().getDisplayName();
                if (damageSource.getDirectEntity() != null && !Objects.equals(attackerUuid, directUuid)) {
                    Component direct = damageSource.getDirectEntity().getDisplayName();
                    return Component.literal(attacker.getString() + " (" + direct.getString() + ")");
                }
                return attacker;
            }
            return Component.literal(damageSource.getMsgId());
        }
    }
}