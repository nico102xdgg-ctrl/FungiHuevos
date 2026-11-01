package com.huevos;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(Huevos.ID)
public class Huevos {
    public static final String ID = "huevos";
    public static final String[] COLORS = {"rojo", "azul", "verde", "amarillo"};
    public static final String[] EGG_COLORS = {"blanco", "rojo", "azul", "verde", "amarillo"};

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);

    public static final Map<String, RegistryObject<Item>> VESTS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> EGG_ITEMS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<EntityType<DummyEntity>>> DUMMIES = new LinkedHashMap<>();
    public static final RegistryObject<EntityType<EggEntity>> EGG;

    static {
        for (String c : COLORS) {
            VESTS.put(c, ITEMS.register("chaleco_" + c, () -> new VestItem(c)));
            DUMMIES.put(c, ENTITIES.register("dummy_" + c, () ->
                    EntityType.Builder.<DummyEntity>of((t, l) -> new DummyEntity(t, l, c), MobCategory.MISC)
                            .sized(0.7f, 1.9f).clientTrackingRange(10).build("dummy_" + c)));
        }
        for (String c : EGG_COLORS) EGG_ITEMS.put(c, ITEMS.register("huevo_" + c, () -> new EggItem(c)));
        EGG = ENTITIES.register("huevo", () ->
                EntityType.Builder.<EggEntity>of(EggEntity::new, MobCategory.MISC)
                        .sized(0.6f, 0.8f).clientTrackingRange(10).updateInterval(1).build("huevo"));
    }

    public Huevos() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Net::register));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                VESTS.values().forEach(r -> e.accept(r.get()));
                EGG_ITEMS.values().forEach(r -> e.accept(r.get()));
            }
        });
    }
}
