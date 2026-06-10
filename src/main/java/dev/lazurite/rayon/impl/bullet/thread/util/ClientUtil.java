package dev.lazurite.rayon.impl.bullet.thread.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class ClientUtil {
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static boolean isPaused() {
        if (isClient()) {
            return Minecraft.getInstance().isPaused();
        }
        return false;
    }

    public static boolean isConnectedToServer() {
        if (isClient()) {
            return Minecraft.getInstance().getConnection() != null;
        }
        return false;
    }
}
