package com.mini.lorerecord;
import com.mini.lorerecord.config.Config;
import com.mini.lorerecord.config.ServerConfig;
import com.mini.lorerecord.network.LoreNetwork;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LoreRecord.MODID)
public class LoreRecord {

    /** 模组ID*/
    public static final String MODID = "lorerecord";

    public static final Logger LOGGER = LogUtils.getLogger();

    public LoreRecord(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        LoreNetwork.register();
    }
}
