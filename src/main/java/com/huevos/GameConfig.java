package com.huevos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bases (zonas) por color y puntos de aparición de huevos. Se guarda en config/huevos.json */
public class GameConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static GameConfig INSTANCE = new GameConfig();

    public Map<String, int[]> bases = new HashMap<>();
    public List<int[]> spawns = new ArrayList<>();

    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve("huevos.json"); }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                GameConfig c = GSON.fromJson(Files.readString(p), GameConfig.class);
                if (c != null) {
                    if (c.bases == null) c.bases = new HashMap<>();
                    if (c.spawns == null) c.spawns = new ArrayList<>();
                    INSTANCE = c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try { Files.writeString(path(), GSON.toJson(INSTANCE)); } catch (Exception e) { e.printStackTrace(); }
    }

    public static AABB zone(String color) {
        int[] b = INSTANCE.bases.get(color);
        if (b == null) return null;
        return new AABB(Math.min(b[0], b[3]), Math.min(b[1], b[4]), Math.min(b[2], b[5]),
                Math.max(b[0], b[3]) + 1, Math.max(b[1], b[4]) + 1, Math.max(b[2], b[5]) + 1);
    }
}
