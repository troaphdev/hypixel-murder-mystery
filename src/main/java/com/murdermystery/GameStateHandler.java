package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GameStateHandler {
    
    private static boolean isInGame = false;
    private static int tickCounter = 0;
    private static boolean hasPolledCurrentWorld = false;
    private static boolean isAwaitingLocationResponse = false;
    
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
            
            // Handle /locraw response - hide ALL locraw related output
            if (isAwaitingLocationResponse) {
                // Check for JSON response (normal case)
                if (message.startsWith("{") && (message.contains("\"server\"") || message.contains("\"gametype\""))) {
                    handleLocrawResponse(message);
                    event.setCanceled(true); // Hide the raw JSON from chat
                    isAwaitingLocationResponse = false;
                    return;
                }
                
                // Check for error messages or other locraw responses
                if (message.toLowerCase().contains("locraw") || 
                    message.toLowerCase().contains("you are in") ||
                    message.toLowerCase().contains("unknown command") ||
                    message.toLowerCase().contains("server:") ||
                    message.toLowerCase().contains("gametype:") ||
                    message.toLowerCase().contains("mode:") ||
                    message.toLowerCase().contains("map:")) {
                    
                    System.out.println("Murder Mystery Helper: Hiding locraw related message: " + message);
                    event.setCanceled(true); // Hide any locraw related output
                    isAwaitingLocationResponse = false;
                    return;
                }
                
                // Stop waiting after 3 seconds to avoid hiding unrelated messages
                // This is handled by the tick counter in onClientTick
            }
            
            // Check for game start message (multiple variations)
            if (message.contains("Teaming with the Murderer is not allowed!") ||
                message.contains("Teaming with the Detective/Innocents is not allowed!") ||
                message.contains("Teaming with the Detective") && message.contains("not allowed")) {
                
                isInGame = true;
                System.out.println("Murder Mystery Helper: Game started detected via chat: " + message);
                
                // Clear all detection lists when starting a new game
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            
            // Check for game end messages (enhanced detection)
            if (isInGame && (message.contains("GAME OVER") || 
                           message.contains("Winner") || 
                           message.contains("won the game") ||
                           message.contains("Sending you to") ||
                           message.contains("You have been eliminated") ||
                           message.contains("Play Again") ||
                           message.contains("Thanks for playing") ||
                           message.contains("joined the lobby!"))) {
                
                isInGame = false;
                System.out.println("Murder Mystery Helper: Game ended detected via chat message: " + message);
                
                // Clear all detection lists when leaving game
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            
        } catch (Exception e) {
            System.err.println("Murder Mystery Helper: Error processing chat for game state: " + e.getMessage());
        }
    }
    
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        // Mark the current world as eligible for "/locraw" to be re-run
        hasPolledCurrentWorld = false;
        isAwaitingLocationResponse = false;
        System.out.println("Murder Mystery Helper: World changed, will poll location on next tick.");
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            // Player disconnected - reset game state
            if (isInGame) {
                isInGame = false;
                System.out.println("Murder Mystery Helper: Player disconnected, resetting game state.");
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            hasPolledCurrentWorld = false;
            isAwaitingLocationResponse = false;
            return;
        }
        
        // Poll location once per world after a short delay to ensure world is loaded
        if (!hasPolledCurrentWorld) {
            tickCounter++;
            if (tickCounter >= 20) { // Wait 1 second after world load
                mc.thePlayer.sendChatMessage("/locraw");
                hasPolledCurrentWorld = true;
                isAwaitingLocationResponse = true;
                tickCounter = 0;
                System.out.println("Murder Mystery Helper: Sent /locraw command to detect current location.");
            }
        } else if (isAwaitingLocationResponse) {
            // Timeout for locraw response to avoid hiding unrelated messages
            tickCounter++;
            if (tickCounter >= 60) { // Stop waiting after 3 seconds
                isAwaitingLocationResponse = false;
                tickCounter = 0;
                System.out.println("Murder Mystery Helper: Locraw response timeout, stopped waiting.");
            }
        } else {
            tickCounter = 0;
        }
    }
    
    private void handleLocrawResponse(String jsonMessage) {
        try {
            System.out.println("Murder Mystery Helper: Received locraw response: " + jsonMessage);
            
            // Simple JSON parsing - check if we're in a Murder Mystery game
            boolean isMurderMystery = jsonMessage.contains("\"gametype\":\"MURDER_MYSTERY\"") || 
                                    jsonMessage.contains("\"MURDER_MYSTERY\"") ||
                                    jsonMessage.contains("Murder Mystery");
            
            if (isMurderMystery) {
                // We're in Murder Mystery - could be lobby or game
                // The game start message will determine when we're actually playing
                System.out.println("Murder Mystery Helper: Detected Murder Mystery mode.");
            } else {
                // We're not in Murder Mystery at all - disable mod
                if (isInGame) {
                    isInGame = false;
                    System.out.println("Murder Mystery Helper: Left Murder Mystery, disabling mod features.");
                    
                    // Clear all detection lists when leaving Murder Mystery entirely
                    MurderDetectionHandler.clearLists();
                    TabListRenderer.resetDisplayNames();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Murder Mystery Helper: Error parsing locraw response: " + e.getMessage());
        }
    }
    

    
    // Public method for other classes to check if we're in a game
    public static boolean isInGame() {
        return isInGame;
    }
    
    // Method to manually set game state (for debugging or other triggers)
    public static void setInGame(boolean inGame) {
        boolean wasInGame = isInGame;
        isInGame = inGame;
        
        if (wasInGame != inGame) {
            if (inGame) {
                System.out.println("Murder Mystery Helper: Manual game state set to IN GAME");
            } else {
                System.out.println("Murder Mystery Helper: Manual game state set to OUT OF GAME");
                // Clear lists when leaving game
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
        }
    }
    
    // Reset method for when mod is disabled/enabled
    public static void reset() {
        isInGame = false;
        MurderDetectionHandler.clearLists();
        TabListRenderer.resetDisplayNames();
        System.out.println("Murder Mystery Helper: Game state reset.");
    }
} 