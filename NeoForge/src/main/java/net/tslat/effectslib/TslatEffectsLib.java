package net.tslat.effectslib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tslat.effectslib.command.TELCommand;
import net.tslat.effectslib.networking.TELNetworking;

@Mod(TELConstants.MOD_ID)
public class TslatEffectsLib {
    public static PayloadRegistrar packetRegistrar = null;

    public TslatEffectsLib(IEventBus modBus)  {
        NeoForge.EVENT_BUS.addListener(TslatEffectsLib::registerCommands);
        NeoForge.EVENT_BUS.addListener(TslatEffectsLib::serverStarted);
        modBus.addListener(TslatEffectsLib::networkingInit);
    }

    private static void registerCommands(final RegisterCommandsEvent ev) {
        TELCommand.registerSubcommands(ev.getDispatcher(), ev.getBuildContext());
    }

    private static void networkingInit(final RegisterPayloadHandlersEvent ev) {
        packetRegistrar = ev.registrar(TELConstants.MOD_ID);
        TELNetworking.init();
        packetRegistrar = null;
    }

    private static void serverStarted(final ServerStartedEvent ev) {
        TELConstants.SERVER = ev.getServer();
    }
}
