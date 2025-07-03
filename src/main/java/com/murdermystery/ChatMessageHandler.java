package com.murdermystery;

import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ChatMessageHandler {
    
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null) {
                return;
            }
            
            String message = null;
            try {
                message = event.message.getUnformattedText();
            } catch (Exception e) {
                // If we can't get the unformatted text, try toString
                try {
                    message = event.message.toString();
                } catch (Exception e2) {
                    return; // Give up if we can't get any text
                }
            }
            
            if (message == null || message.isEmpty()) {
                return;
            }
            
            // Check for Hypixel's "Sending you to" message
            if (message.length() >= 13 && message.startsWith("Sending you to")) {
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            
            // Check for murderer assignment message "&e You get your sword in"
            // Remove color codes to handle different formatting - safer approach
            String unformattedMessage = removeColorCodes(message);
            if (unformattedMessage != null && unformattedMessage.contains("You get your sword in")) {
                TitleHandler.setPlayerMurderer(true);
            }
            
            // Check for "The Murderer has received their sword!" message
            // This is when detectives get their bows, so we need to check if we're the detective
            if (unformattedMessage != null && unformattedMessage.contains("The Murderer has received their sword!")) {
                MurderDetectionHandler.scheduleDetectiveCheck();
            }
            
            // Check for bow pickup messages - mark closest player as golden detective
            if (unformattedMessage != null && 
                (unformattedMessage.contains("picked up the bow") || 
                 unformattedMessage.contains("has picked up") || 
                 unformattedMessage.contains("bow has been picked up") ||
                 unformattedMessage.contains("picked up a bow") ||
                 unformattedMessage.contains("found the bow") ||
                 (unformattedMessage.toLowerCase().contains("bow") && unformattedMessage.toLowerCase().contains("pick")))) {
                MurderDetectionHandler.updateGoldenDetectiveFromBowPickup();
            }
        } catch (Exception e) {
            // Catch any unexpected errors to prevent crashes
        }
    }
    
    /**
     * Safely remove color codes from a string
     */
    private String removeColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        try {
            // Safer color code removal - handle each character individually
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if ((c == '\u00A7' || c == '&') && i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    // Check if next character is a valid color code
                    if ((next >= '0' && next <= '9') || 
                        (next >= 'a' && next <= 'f') || 
                        (next >= 'k' && next <= 'o') || 
                        next == 'r') {
                        i++; // Skip both the color symbol and the code
                        continue;
                    }
                }
                result.append(c);
            }
            return result.toString();
        } catch (Exception e) {
            // If color code removal fails, return original string
            return input;
        }
    }
} 