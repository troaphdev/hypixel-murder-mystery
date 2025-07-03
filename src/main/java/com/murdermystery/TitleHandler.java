package com.murdermystery;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.item.ItemStack;
import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
public class TitleHandler {
    
    private static boolean isPlayerMurderer = false;
    private static String lastTitle = "";
    private static int titleCheckCounter = 0; // Separate counter for title checks only
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
                Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.ingameGUI == null) {
            return;
        }

        // Only check for murderer status when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            // If we're in lobby and detect a role title, trigger game start
            if (GameStateHandler.isInLobby()) {
                titleCheckCounter++;
                if (titleCheckCounter >= 10) {
                    titleCheckCounter = 0;
                    checkTitleForGameStart(mc);
                }
            } else {
                // Reset murderer status when not in game or lobby
                if (isPlayerMurderer) {
                    isPlayerMurderer = false;
                }
            }
            return;
        }
        
        // Check hotbar for murderer weapons IMMEDIATELY (every tick for instant detection)
        checkHotbarForMurdererWeapons(mc);
        
        // Title checking can be less frequent (every 10 ticks for performance)
        titleCheckCounter++;
        if (titleCheckCounter >= 10) {
            titleCheckCounter = 0;
            checkTitleForMurdererRole(mc);
        }
    }
    
    private void checkHotbarForMurdererWeapons(Minecraft mc) {
        if (mc.thePlayer.inventory == null) {
            return;
        }
        
        boolean foundMurdererWeapon = false;
        
        // Check hotbar slots (slots 0-8) - IMMEDIATE detection
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (WeaponDetector.isMurderWeapon(stack)) {
                foundMurdererWeapon = true;
                break;
            }
        }
        
        // Update murderer status based on hotbar contents - IMMEDIATE
        if (foundMurdererWeapon && !isPlayerMurderer) {
            setPlayerMurderer(true);
        } else if (!foundMurdererWeapon && isPlayerMurderer) {
            // Only reset if no other detection methods are active
            // This prevents flickering when switching items
            resetMurdererStatus();
        }
    }
    
    private void checkTitleForGameStart(Minecraft mc) {
        try {
            // Access the title display fields using reflection
            GuiIngame gui = mc.ingameGUI;
            
            // Try multiple possible field names for title
            String currentTitle = null;
            try {
                Field titleField = GuiIngame.class.getDeclaredField("displayedTitle");
                titleField.setAccessible(true);
                currentTitle = (String) titleField.get(gui);
            } catch (Exception e1) {
                try {
                    Field titleField = GuiIngame.class.getDeclaredField("field_175200_n"); // Obfuscated name
                    titleField.setAccessible(true);
                    currentTitle = (String) titleField.get(gui);
                } catch (Exception e2) {
                    // Title detection failed, skip this tick
                    return;
                }
            }
            
            if (currentTitle != null && !currentTitle.equals(lastTitle)) {
                lastTitle = currentTitle;
                
                // Check if the title contains role information indicating game start
                String unformattedTitle = currentTitle.replaceAll("[§&][0-9a-fk-or]", "");
                String upperTitle = unformattedTitle.toUpperCase();
                
                if (upperTitle.contains("ROLE:") || 
                    upperTitle.contains("YOU ARE") || 
                    upperTitle.contains("MURDERER") || 
                    upperTitle.contains("DETECTIVE") || 
                    upperTitle.contains("INNOCENT")) {
                    
                    // Trigger game start
                    GameStateHandler.setInGame(true);
                    System.out.println("Murder Mystery Helper: Game start detected via title! Mod activated.");
                    
                    // Also check if we're the murderer
                    if (upperTitle.contains("MURDERER")) {
                        setPlayerMurderer(true);
                    }
                }
            }
        } catch (Exception e) {
            // Reflection failed completely - title detection not available
        }
    }

    private void checkTitleForMurdererRole(Minecraft mc) {
        try {
            // Access the title display fields using reflection
            GuiIngame gui = mc.ingameGUI;
            
            // Try multiple possible field names for title
            String currentTitle = null;
            try {
                Field titleField = GuiIngame.class.getDeclaredField("displayedTitle");
                titleField.setAccessible(true);
                currentTitle = (String) titleField.get(gui);
            } catch (Exception e1) {
                try {
                    Field titleField = GuiIngame.class.getDeclaredField("field_175200_n"); // Obfuscated name
                    titleField.setAccessible(true);
                    currentTitle = (String) titleField.get(gui);
                } catch (Exception e2) {
                    // Title detection failed, skip this tick
                    return;
                }
            }
            
            if (currentTitle != null && !currentTitle.equals(lastTitle)) {
                lastTitle = currentTitle;
                
                // Check if the title contains "ROLE: MURDERER" (case insensitive)
                // Remove both § and & color codes to handle different formats
                String unformattedTitle = currentTitle.replaceAll("[§&][0-9a-fk-or]", "");
                if (unformattedTitle.toUpperCase().contains("ROLE: MURDERER")) {
                    setPlayerMurderer(true);
                }
            }
        } catch (Exception e) {
            // Reflection failed completely - title detection not available
        }
    }
    
    public static boolean isPlayerMurderer() {
        return isPlayerMurderer;
    }
    
    public static void setPlayerMurderer(boolean isMurderer) {
        isPlayerMurderer = isMurderer;
    }
    
    public static void resetMurdererStatus() {
        isPlayerMurderer = false;
        lastTitle = "";
    }
} 