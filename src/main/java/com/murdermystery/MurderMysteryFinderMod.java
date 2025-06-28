package com.murdermystery;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = MurderMysteryFinderMod.MODID, name = MurderMysteryFinderMod.NAME, version = MurderMysteryFinderMod.VERSION)
public class MurderMysteryFinderMod {
    
    public static final String MODID = "murdermysteryfinder";
    public static final String NAME = "Murder Mystery Helper";
    public static final String VERSION = "1.0.0";
    
    @Mod.Instance(MODID)
    public static MurderMysteryFinderMod instance;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Pre-initialization
        System.out.println("Murder Mystery Helper Mod is loading...");
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new GameStateHandler());
        MinecraftForge.EVENT_BUS.register(new MurderDetectionHandler());
        MinecraftForge.EVENT_BUS.register(new MurderListRenderer());
        MinecraftForge.EVENT_BUS.register(new MurderESPRenderer());
        MinecraftForge.EVENT_BUS.register(new ChatMessageHandler());
        MinecraftForge.EVENT_BUS.register(new TitleHandler());
        MinecraftForge.EVENT_BUS.register(new ScoreboardHandler());
        MinecraftForge.EVENT_BUS.register(new GoldItemGlowRenderer());
        MinecraftForge.EVENT_BUS.register(new TracerRenderer());
        MinecraftForge.EVENT_BUS.register(new NametagRenderer());
        MinecraftForge.EVENT_BUS.register(new TabListRenderer());
        System.out.println("Murder Mystery Helper Mod loaded successfully with ESP, tracers, gold glow, nametags, tab list, and detection features!");
    }
} 