package com.huevos;

import net.minecraft.ChatFormatting;

public final class Teams {
    private Teams() {}

    public static ChatFormatting fmt(String c) {
        return switch (c) {
            case "rojo" -> ChatFormatting.RED;
            case "azul" -> ChatFormatting.BLUE;
            case "verde" -> ChatFormatting.GREEN;
            case "amarillo" -> ChatFormatting.YELLOW;
            default -> ChatFormatting.WHITE;
        };
    }

    public static int argb(String c) {
        return switch (c) {
            case "rojo" -> 0xFFD62F3A;
            case "azul" -> 0xFF2F57D6;
            case "verde" -> 0xFF22B53A;
            case "amarillo" -> 0xFFE8B00B;
            default -> 0xFF888888;
        };
    }

    public static boolean valid(String c) {
        for (String s : Huevos.COLORS) if (s.equals(c)) return true;
        return false;
    }
}
