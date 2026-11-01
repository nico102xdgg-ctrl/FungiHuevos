package com.huevos;

import java.util.ArrayList;
import java.util.List;

/** Estado de la tabla de puntaje en el cliente (sin imports de cliente). */
public class ClientData {
    public static volatile boolean show = false;
    public static volatile List<String> colors = new ArrayList<>();
    public static volatile List<Integer> scores = new ArrayList<>();
    public static volatile int seconds = 0;

    public static void set(boolean s, List<String> c, List<Integer> sc, int sec) {
        show = s; colors = c; scores = sc; seconds = sec;
    }
}
