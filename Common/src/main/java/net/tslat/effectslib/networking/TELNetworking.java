package net.tslat.effectslib.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;
import net.tslat.effectslib.networking.packet.TELClearParticlesPacket;
import net.tslat.effectslib.networking.packet.TELParticlePacket;

public interface TELNetworking {
    /**
     * Register a custom packet for networking
     * <p>Packet must extend {@link MultiloaderPacket} for ease-of-use</p>
     */
    static <B extends FriendlyByteBuf, P extends MultiloaderPacket> void registerPacket(CustomPacketPayload.Type<P> payloadType, StreamCodec<B, P> codec, boolean isClientBound) {
        TELConstants.NETWORKING.registerPacketInternal(payloadType, codec, isClientBound);
    }

    /**
     * Send a packet to the server from the client
     */
    static void sendToServer(MultiloaderPacket packet) {
        TELConstants.NETWORKING.sendToServerInternal(packet);
    }

    /**
     * Send a packet to all players on the server
     */
    static void sendToAllPlayers(MultiloaderPacket packet) {
        TELConstants.NETWORKING.sendToAllPlayersInternal(packet);
    }

    /**
     * Send a packet to all players in a given world
     */
    static void sendToAllPlayersInWorld(MultiloaderPacket packet, ServerLevel level) {
        TELConstants.NETWORKING.sendToAllPlayersInWorldInternal(packet, level);
    }

    /**
     * Send a packet to all players within a given radius of a position
     */
    static void sendToAllNearbyPlayers(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius) {
        TELConstants.NETWORKING.sendToAllNearbyPlayersInternal(packet, level, origin, radius);
    }

    /**
     * Send a packet to a given player
     */
    static void sendToPlayer(MultiloaderPacket packet, ServerPlayer player) {
        TELConstants.NETWORKING.sendToPlayerInternal(packet, player);
    }

    /**
     * Send a packet to all players currently tracking a given entity.<br>
     * Good as a shortcut for sending a packet to all players that may have an interest in a given entity or its dealings.<br>
     * <br>
     * Will also send the packet to the entity itself if the entity is also a player
     */
    static void sendToAllPlayersTrackingEntity(MultiloaderPacket packet, Entity trackingEntity) {
        TELConstants.NETWORKING.sendToAllPlayersTrackingEntityInternal(packet, trackingEntity);
    }

    /**
     * Send a packet to all players tracking a given block position
     */
    static void sendToAllPlayersTrackingBlock(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
        TELConstants.NETWORKING.sendToAllPlayersTrackingBlockInternal(packet, level, pos);
    }

    // <-- Internal instanced methods --> //


    static void init() {
        registerPacket(TELParticlePacket.TYPE, TELParticlePacket.CODEC, true);
        registerPacket(TELClearParticlesPacket.TYPE, TELClearParticlesPacket.CODEC, true);
    }

    <B extends FriendlyByteBuf, P extends MultiloaderPacket> void registerPacketInternal(CustomPacketPayload.Type<P> packetType, StreamCodec<B, P> codec, boolean isClientBound);
    void sendToServerInternal(MultiloaderPacket packet);
    void sendToAllPlayersInternal(MultiloaderPacket packet);
    void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level);
    void sendToAllNearbyPlayersInternal(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius);
    void sendToPlayerInternal(MultiloaderPacket packet, ServerPlayer player);
    void sendToAllPlayersTrackingEntityInternal(MultiloaderPacket packet, Entity trackingEntity);
    void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos);
}