package net.tslat.effectslib.networking.packet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.api.particle.ParticleBuilder;
import net.tslat.effectslib.networking.TELNetworking;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public record TELParticlePacket(Collection<ParticleBuilder> particles) implements MultiloaderPacket {
    public static final CustomPacketPayload.Type<TELParticlePacket> TYPE = new Type<>(new ResourceLocation(TELConstants.MOD_ID, "tel_particle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TELParticlePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ObjectArrayList::new, ParticleBuilder.CODEC),
            TELParticlePacket::particles,
            TELParticlePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public TELParticlePacket() {
        this(1);
    }

    public TELParticlePacket(int amount) {
        this(new ObjectArrayList<>(amount));
    }

    public TELParticlePacket(ParticleBuilder... particles) {
        this(ObjectArrayList.of(particles));
    }

    public TELParticlePacket particle(final ParticleBuilder particle) {
        this.particles.add(particle);

        return this;
    }

    public boolean isEmpty() {
        return this.particles.isEmpty();
    }

    public void send(ServerLevel level) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllPlayersInWorld(this, level);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersTrackingEntity(ServerLevel level, Entity entity) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllPlayersTrackingEntity(this, entity);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersTrackingBlock(ServerLevel level, BlockPos pos) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllPlayersTrackingBlock(this, level, pos);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllNearbyPlayers(ServerLevel level, Vec3 origin, double radius) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllNearbyPlayers(this, level, origin, radius);
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            for (ParticleBuilder builder : this.particles) {
                builder.spawnParticles(TELClient.getClientPlayer().level());
            }
        });
    }
}