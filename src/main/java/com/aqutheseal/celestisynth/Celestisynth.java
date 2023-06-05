package com.aqutheseal.celestisynth;

import com.aqutheseal.celestisynth.animation.CSAnimator;
import com.aqutheseal.celestisynth.network.CSNetwork;
import com.aqutheseal.celestisynth.registry.CSEntityRegistry;
import com.aqutheseal.celestisynth.registry.CSItemRegistry;
import com.aqutheseal.celestisynth.registry.CSSoundRegistry;
import com.aqutheseal.celestisynth.registry.datagen.CSItemModelProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(Celestisynth.MODID)
public class Celestisynth {
    public static final String MODID = "celestisynth";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Celestisynth() {
        GeckoLib.initialize();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        CSEntityRegistry.ENTITY_TYPES.register(modEventBus);
        CSItemRegistry.ITEMS.register(modEventBus);
        CSSoundRegistry.SOUND_EVENTS.register(modEventBus);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                modEventBus.addListener(CSAnimator::registerAnimationLayer)
        );
        modEventBus.addListener(this::registerPackets);
        modEventBus.addListener(this::gatherData);
        modEventBus.register(this);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(this);
    }

    private void registerPackets(FMLCommonSetupEvent event) {
        CSNetwork.register();
    }

    private void gatherData(final GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        final ExistingFileHelper efh = event.getExistingFileHelper();
        if (event.includeServer()) {
            dataGenerator.addProvider(true, new CSItemModelProvider(dataGenerator.getPackOutput(), MODID, efh));
        }
    }
}