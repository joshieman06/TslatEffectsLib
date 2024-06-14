package net.tslat.effectslib.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.TELConstants;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record TELClearParticlesPacket() implements MultiloaderPacket {
    public static final CustomPacketPayload.Type<TELClearParticlesPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TELConstants.MOD_ID, "tel_clear_particles"));
    public static final StreamCodec<FriendlyByteBuf, TELClearParticlesPacket> CODEC = StreamCodec.unit(new TELClearParticlesPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(TELClient::clearParticles);
    }
}