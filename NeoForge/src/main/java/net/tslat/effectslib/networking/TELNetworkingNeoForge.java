package net.tslat.effectslib.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tslat.effectslib.TslatEffectsLib;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

public class TELNetworkingNeoForge implements TELNetworking {
	@Override
	public <B extends FriendlyByteBuf, P extends MultiloaderPacket> void registerPacketInternal(CustomPacketPayload.Type<P> packetType, StreamCodec<B, P> codec, boolean isClientBound) {
		if (isClientBound) {
			TslatEffectsLib.packetRegistrar.playToClient(packetType, (StreamCodec<FriendlyByteBuf, P>)codec, (packet, context) -> packet.receiveMessage(context.player(), context::enqueueWork));
		}
		else {
			TslatEffectsLib.packetRegistrar.playToServer(packetType, (StreamCodec<FriendlyByteBuf, P>)codec, (packet, context) -> packet.receiveMessage(context.player(), context::enqueueWork));
		}
	}

	/**
	 * Send a packet to the server from the client
	 */
	@Override
	public void sendToServerInternal(MultiloaderPacket packet) {
		PacketDistributor.sendToServer(packet);
	}

	/**
	 * Send a packet to all players on the server
	 */
	@Override
	public void sendToAllPlayersInternal(MultiloaderPacket packet) {
		PacketDistributor.sendToAllPlayers(packet);
	}

	/**
	 * Send a packet to all players in a given world
	 */
	@Override
	public void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level) {
		PacketDistributor.sendToPlayersInDimension(level, packet);
	}

	/**
	 * Send a packet to all players within a given radius of a position
	 */
	@Override
	public void sendToAllNearbyPlayersInternal(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(origin) <= radius * radius)
				sendToPlayerInternal(packet, player);
		}
	}

	/**
	 * Send a packet to a given player
	 */
	@Override
	public void sendToPlayerInternal(MultiloaderPacket packet, ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, packet);
	}

	/**
	 * Send a packet to all players currently tracking a given entity.<br>
	 * Good as a shortcut for sending a packet to all players that may have an interest in a given entity or its dealings.<br>
	 * <br>
	 * Will also send the packet to the entity itself if the entity is also a player
	 */
	@Override
	public void sendToAllPlayersTrackingEntityInternal(MultiloaderPacket packet, Entity trackingEntity) {
		if (trackingEntity instanceof Player) {
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(trackingEntity, packet);
		}
		else {
			PacketDistributor.sendToPlayersTrackingEntity(trackingEntity, packet);
		}
	}

	/**
	 * Send a packet to all players tracking a given block position
	 */
	@Override
	public void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
		PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), packet);
	}
}