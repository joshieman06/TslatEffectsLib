package net.tslat.effectslib.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadProtocol;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

public class TELNetworkingForge implements TELNetworking {
	public static PayloadProtocol<RegistryFriendlyByteBuf, CustomPacketPayload> NETWORK_CHANNEL_BUILDER = ChannelBuilder.named(new ResourceLocation(TELConstants.MOD_ID, "tel_packets")).networkProtocolVersion(1).payloadChannel().play();
	public static Channel<CustomPacketPayload> CHANNEL;

	public TELNetworkingForge() {}

	public static void init() {
		TELNetworking.init();

		CHANNEL = NETWORK_CHANNEL_BUILDER.bidirectional().build();
	}

	/**
	 * Register a custom packet for networking
	 * Packet must extend {@link MultiloaderPacket} for ease-of-use
	 */
	@Override
	public <B extends FriendlyByteBuf, P extends MultiloaderPacket> void registerPacketInternal(CustomPacketPayload.Type<P> packetType, StreamCodec<B, P> codec, boolean isClientBound) {
		if (isClientBound) {
			NETWORK_CHANNEL_BUILDER.clientbound().add(packetType, (StreamCodec<RegistryFriendlyByteBuf, P>)codec, (packet, context) -> {
				packet.receiveMessage(context.getSender() != null ? context.getSender() : TELClient.getClientPlayer(), context::enqueueWork);
				context.setPacketHandled(true);
			});
		}
		else {
			NETWORK_CHANNEL_BUILDER.serverbound().add(packetType, (StreamCodec<RegistryFriendlyByteBuf, P>)codec, (packet, context) -> {
				packet.receiveMessage(context.getSender() != null ? context.getSender() : TELClient.getClientPlayer(), context::enqueueWork);
				context.setPacketHandled(true);
			});
		}
	}

	/**
	 * Send a packet to the server from the client
	 */
	@Override
	public void sendToServerInternal(MultiloaderPacket packet) {
		CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
	}

	/**
	 * Send a packet to all players on the server
	 */
	@Override
	public void sendToAllPlayersInternal(MultiloaderPacket packet) {
		CHANNEL.send(packet, PacketDistributor.ALL.noArg());
	}

	/**
	 * Send a packet to all players in a given world
	 */
	@Override
	public void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level) {
		CHANNEL.send(packet, PacketDistributor.DIMENSION.with(level.dimension()));
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
		CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
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
			CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(trackingEntity));
		}
		else {
			CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY.with(trackingEntity));
		}
	}

	/**
	 * Send a packet to all players tracking a given block position
	 */
	@Override
	public void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
		CHANNEL.send(packet, PacketDistributor.TRACKING_CHUNK.with(level.getChunkAt(pos)));
	}
}