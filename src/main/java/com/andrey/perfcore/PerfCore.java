package com.andrey.perfcore;

import com.andrey.perfcore.client.PerfCoreClient;
import com.andrey.perfcore.client.PerfCoreClientConfig;
import com.andrey.perfcore.server.PerfCoreServer;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(PerfCore.MOD_ID)
public final class PerfCore {
    public static final String MOD_ID = "perfcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PerfCore(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();

        context.registerConfig(ModConfig.Type.COMMON, PerfCoreConfig.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.registerConfig(ModConfig.Type.CLIENT, PerfCoreClientConfig.SPEC);
        }

        ModConfigEvent.Loading.getBus(modBusGroup).addListener(PerfCore::onConfigLoading);
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(PerfCore::onConfigReloading);

        TickEvent.ServerTickEvent.Pre.BUS.addListener(event -> PerfCoreServer.onServerTickPre());
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> PerfCoreServer.onServerTick());
        ServerStoppedEvent.BUS.addListener(event -> PerfCoreServer.onServerStopped());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            PerfCoreClient.init();
        }

        LOGGER.info("PerfCore 1.3.0 загружен (Minecraft 26.3, Forge 66)");
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        bake(event);
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        bake(event);
    }

    private static void bake(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (!MOD_ID.equals(config.getModId())) {
            return;
        }

        switch (config.getType()) {
            case COMMON, SERVER -> PerfCoreConfig.bake();
            case CLIENT -> {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    PerfCoreClientConfig.bake();
                }
            }
        }
    }
}
