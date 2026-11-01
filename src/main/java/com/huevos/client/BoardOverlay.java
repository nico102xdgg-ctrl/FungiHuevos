package com.huevos.client;

import com.huevos.ClientData;
import com.huevos.Huevos;
import com.huevos.Teams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.List;

/** Timer arriba al centro y tabla de puntaje a la derecha (equipo con más puntos arriba). */
public class BoardOverlay {
    public static final int ROW_W = 96, ROW_H = 22, GAP = 3, MARGIN_RIGHT = 6, NUM_W = 28;

    public static void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        if (!ClientData.show || Minecraft.getInstance().options.hideGui) return;
        Font font = Minecraft.getInstance().font;

        // ---- timer
        int sec = Math.max(0, ClientData.seconds);
        String time = String.format("%02d:%02d", sec / 60, sec % 60);
        int tw = font.width(time);
        int bx = w / 2 - (tw + 16) / 2;
        g.fill(bx - 1, 5, bx + tw + 17, 23, 0xFF000000);
        g.fill(bx, 6, bx + tw + 16, 22, 0xAA101010);
        g.drawString(font, time, bx + 8, 10, sec <= 60 ? 0xFFFF5555 : 0xFFFFFFFF, true);

        // ---- tabla
        List<String> colors = new ArrayList<>(ClientData.colors);
        List<Integer> scores = new ArrayList<>(ClientData.scores);
        int n = Math.min(colors.size(), scores.size());
        if (n == 0) return;

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        order.sort((a, b) -> scores.get(b) - scores.get(a));

        int x = w - ROW_W - MARGIN_RIGHT;
        int y = h / 2 - (n * (ROW_H + GAP) - GAP) / 2;
        for (int i : order) {
            String c = colors.get(i);
            g.fill(x - 1, y - 1, x + ROW_W + 1, y + ROW_H + 1, 0xFF000000);
            g.fill(x, y, x + ROW_W, y + ROW_H, Teams.argb(c));
            g.fill(x, y, x + NUM_W, y + ROW_H, 0x66000000);

            String num = String.valueOf(scores.get(i));
            g.drawString(font, num, x + (NUM_W - font.width(num)) / 2, y + (ROW_H - 8) / 2, 0xFFFFFFFF, true);

            ResourceLocation icon = new ResourceLocation(Huevos.ID, "textures/gui/icon_" + c + ".png");
            g.blit(icon, x + NUM_W + 6, y + 3, 16, 16, 0f, 0f, 16, 16, 16, 16);

            String name = Character.toUpperCase(c.charAt(0)) + c.substring(1);
            g.drawString(font, name, x + NUM_W + 28, y + (ROW_H - 8) / 2, 0xFFFFFFFF, true);
            y += ROW_H + GAP;
        }
    }
}
