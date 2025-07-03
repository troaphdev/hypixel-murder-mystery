package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class MurderListRenderer extends Gui {
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        
        // Only render murder list when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        
        // Only render if there are murderers detected
        if (!MurderDetectionHandler.hasMurderers()) {
            return;
        }
        
        List<String> murderers = MurderDetectionHandler.getMurdererList();
        if (murderers.isEmpty()) {
            return;
        }
        
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fontRenderer = mc.fontRendererObj;
        
        // Calculate dimensions
        String title = "Murderer List";
        int titleWidth = fontRenderer.getStringWidth(title);
        
        // Find the longest player name for proper width calculation
        int maxNameWidth = titleWidth;
        for (String name : murderers) {
            int nameWidth = fontRenderer.getStringWidth(name);
            if (nameWidth > maxNameWidth) {
                maxNameWidth = nameWidth;
            }
        }
        
        // UI dimensions
        int padding = 6;
        int lineHeight = 10;
        int uiWidth = maxNameWidth + (padding * 2);
        int uiHeight = (padding * 2) + lineHeight + (murderers.size() * lineHeight);
        
        // Center the UI at the top of the screen
        int x = (sr.getScaledWidth() - uiWidth) / 2;
        int y = 10; // 10 pixels from the top
        
        // Draw semi-transparent background
        int backgroundColor = 0x88000000; // Semi-transparent black
        drawRect(x, y, x + uiWidth, y + uiHeight, backgroundColor);
        
        // Draw border
        int borderColor = 0xFF555555; // Gray border
        // Top border
        drawRect(x, y, x + uiWidth, y + 1, borderColor);
        // Bottom border
        drawRect(x, y + uiHeight - 1, x + uiWidth, y + uiHeight, borderColor);
        // Left border
        drawRect(x, y, x + 1, y + uiHeight, borderColor);
        // Right border
        drawRect(x + uiWidth - 1, y, x + uiWidth, y + uiHeight, borderColor);
        
        // Draw title
        int titleX = x + (uiWidth - titleWidth) / 2;
        int titleY = y + padding;
        fontRenderer.drawString(title, titleX, titleY, 0xFFFFFF); // White text
        
        // Draw player names
        int playerY = titleY + lineHeight;
        for (String playerName : murderers) {
            int nameWidth = fontRenderer.getStringWidth(playerName);
            int nameX = x + (uiWidth - nameWidth) / 2; // Center each name
            fontRenderer.drawString(playerName, nameX, playerY, 0xFFFF5555); // Light red text
            playerY += lineHeight;
        }
    }
} 