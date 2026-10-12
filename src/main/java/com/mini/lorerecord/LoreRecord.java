package com.mini.lorerecord;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 模组主类
 */
@Mod(LoreRecord.MODID)
public class LoreRecord {

    /** 模组唯一 ID*/
    public static final String MODID = "lorerecord";

    /** 全模组共用的日志器 */
    public static final Logger LOGGER = LogUtils.getLogger();

    public LoreRecord(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        LoreNetwork.register();
    }
}
