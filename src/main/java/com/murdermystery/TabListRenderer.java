package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class TabListRenderer extends Gui {
    
    private static final Map<String, String> cachedTabNames = new HashMap<String, String>();
    private int tickCounter = 0;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        
        // Update cached names every 5 ticks
        tickCounter++;
        if (tickCounter >= 5) {
            tickCounter = 0;
            updateCachedTabNames();
        }
    }
    
    @SubscribeEvent
    public void onRenderTabList(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            return;
        }
        
        // Only render custom tab list when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.gameSettings.keyBindPlayerList.isKeyDown()) {
            return;
        }
        
        // Cancel the default tab list rendering
        event.setCanceled(true);
        
        // Render our custom tab list
        renderCustomTabList(mc);
    }
    
    private void renderCustomTabList(Minecraft mc) {
        try {
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            FontRenderer fontRenderer = mc.fontRendererObj;
            
            // Get list of all players
            List<String> playerNames = new ArrayList<String>();
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player != null) {
                    playerNames.add(player.getName());
                }
            }
            
            if (playerNames.isEmpty()) {
                return;
            }
            
            // Calculate tab list dimensions
            int maxNameWidth = 0;
            for (String playerName : playerNames) {
                String formattedName = getFormattedTabName(playerName);
                int nameWidth = fontRenderer.getStringWidth(formattedName);
                if (nameWidth > maxNameWidth) {
                    maxNameWidth = nameWidth;
                }
            }
            
            int tabWidth = Math.max(maxNameWidth + 10, 200);
            int tabHeight = (playerNames.size() * 12) + 20;
            
            // Center the tab list
            int x = (scaledResolution.getScaledWidth() - tabWidth) / 2;
            int y = 20;
            
            // Draw background
            drawRect(x, y, x + tabWidth, y + tabHeight, 0x80000000); // Semi-transparent black
            
            // Draw title
            String title = "Murder Mystery Players";
            int titleWidth = fontRenderer.getStringWidth(title);
            int titleX = x + (tabWidth - titleWidth) / 2;
            fontRenderer.drawStringWithShadow(title, titleX, y + 5, 0xFFFFFF);
            
            // Draw player list
            int playerY = y + 20;
            for (String playerName : playerNames) {
                String rankPrefix = getRankPrefix(playerName);
                int textColor = getPlayerColor(playerName);
                
                // Draw rank prefix in color (if exists)
                int currentX = x + 5;
                if (!rankPrefix.isEmpty()) {
                    fontRenderer.drawStringWithShadow(rankPrefix, currentX, playerY, textColor);
                    currentX += fontRenderer.getStringWidth(rankPrefix);
                }
                
                // Draw username in white (consistent with nametags)
                fontRenderer.drawStringWithShadow(playerName, currentX, playerY, 0xFFFFFF);
                playerY += 12;
            }
            
        } catch (Exception e) {
            System.err.println("Error rendering custom tab list: " + e.getMessage());
        }
    }
    
    private void updateCachedTabNames() {
        Minecraft mc = Minecraft.getMinecraft();
        
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null) {
                continue;
            }
            
            String playerName = player.getName();
            
            // Get role-based formatting
            String rankPrefix = getRankPrefix(playerName);
            
            // Create formatted name and cache it (without color codes since we'll apply color manually)
            String formattedName = rankPrefix + playerName;
            cachedTabNames.put(playerName, formattedName);
        }
    }
    
    // Public method to get formatted name for a player
    public static String getFormattedTabName(String playerName) {
        return cachedTabNames.getOrDefault(playerName, playerName);
    }
    
    private int getPlayerColor(String playerName) {
        // Check player role and return color (same as nametag colors)
        if (MurderDetectionHandler.getMurdererList().contains(playerName)) {
            return 0xFF0000; // Red
        }
        
        String firstBowHolder = MurderDetectionHandler.getFirstBowHolder();
        if (playerName.equals(firstBowHolder)) {
            return 0xFFCC00; // Gold
        }
        
        if (MurderDetectionHandler.getDetectiveList().contains(playerName)) {
            return 0x00B3FF; // Light blue
        }
        
        if (TitleHandler.isPlayerMurderer()) {
            return 0x00FF00; // Lime for victims when player is murderer
        }
        
        return 0xFFFFFF; // White
    }
    

    
    private String getRankPrefix(String playerName) {
        // Check player role and return appropriate rank prefix (same as nametag)
        if (MurderDetectionHandler.getMurdererList().contains(playerName)) {
            return "[MUR] ";
        }
        
        String firstBowHolder = MurderDetectionHandler.getFirstBowHolder();
        if (playerName.equals(firstBowHolder)) {
            return "[DET] ";
        }
        
        if (MurderDetectionHandler.getDetectiveList().contains(playerName)) {
            return "[BOW] ";
        }
        
        // No prefix for other players
        return "";
    }
    
    // Reset cached names when leaving the game/server
    public static void resetDisplayNames() {
        cachedTabNames.clear();
        System.out.println("Murder Mystery Helper: Cleared cached tab list names.");
    }
} 