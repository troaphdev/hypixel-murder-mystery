/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraftforge.client.event.ClientChatReceivedEvent
 *  net.minecraftforge.event.world.WorldEvent$Load
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$ClientTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package com.murdermystery;

import com.murdermystery.MurderDetectionHandler;
import com.murdermystery.TabListRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(value=Side.CLIENT)
public class GameStateHandler {
    private static boolean isInGame = false;
    private static boolean isInLobby = false; // Track if we're in a Murder Mystery lobby but game hasn't started
    private static boolean isDoubleUpMode = false;
    private static int tickCounter = 0;
    private static boolean hasPolledCurrentWorld = false;
    private static boolean isAwaitingLocationResponse = false;
    private static long lobbyJoinTime = 0; // Track when we joined the lobby
    private static final long AUTO_ACTIVATE_DELAY = 100; // 100ms delay for virtually instant activation

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null) {
                return;
            }
            String message = null;
            try {
                message = event.message.getUnformattedText();
            }
            catch (Exception e) {
                try {
                    message = event.message.toString();
                }
                catch (Exception e2) {
                    return;
                }
            }
            if (message == null || message.isEmpty()) {
                return;
            }
            

            if (isAwaitingLocationResponse) {
                if (message.startsWith("{") && (message.contains("\"server\"") || message.contains("\"gametype\""))) {
                    this.handleLocrawResponse(message);
                    event.setCanceled(true);
                    isAwaitingLocationResponse = false;
                    return;
                }
                if (message.toLowerCase().contains("locraw") || message.toLowerCase().contains("you are in") || message.toLowerCase().contains("unknown command") || message.toLowerCase().contains("server:") || message.toLowerCase().contains("gametype:") || message.toLowerCase().contains("mode:") || message.toLowerCase().contains("map:")) {
                    event.setCanceled(true);
                    isAwaitingLocationResponse = false;
                    return;
                }
            }
            
            // LOBBY DETECTION - Don't activate mod yet, just track that we're in a Murder Mystery lobby
            // Strip color codes for reliable detection (both § and & formats)
            String cleanMessage = message.replaceAll("[§&][0-9a-fk-or]", "");
            
            if (cleanMessage.contains("Teaming with the Murderer is not allowed!") || 
                cleanMessage.contains("Teaming with the Murderers is not allowed!") || // Doubles mode
                cleanMessage.contains("Teaming with the Detective/Innocents is not allowed!") ||
                cleanMessage.contains("Teaming with the Detectives/Innocents is not allowed!") || // If player is murderer
                (cleanMessage.contains("Teaming with the Detective") && cleanMessage.contains("not allowed")) ||
                (cleanMessage.contains("Teaming with the Murderer") && cleanMessage.contains("not allowed"))) {
                
                isInLobby = true;
                lobbyJoinTime = System.currentTimeMillis(); // Start the backup timer
                // DON'T set isInGame = true here - wait for actual game start
            }
            
            // MURDER MYSTERY SPECIFIC GAME START DETECTION - EARLY TRIGGERS
            if (isInLobby && !isInGame && (
                // PRIORITY: Game countdown - happens immediately when game starts
                cleanMessage.contains("The game starts in 1 second!") ||
                cleanMessage.contains("The game starts in 2 seconds!") ||
                cleanMessage.contains("The game starts in 3 seconds!") ||
                cleanMessage.contains("The game starts in 4 seconds!") ||
                cleanMessage.contains("The game starts in 5 seconds!") ||
                cleanMessage.contains("game starts in") ||
                cleanMessage.contains("Game starts in") ||
                cleanMessage.contains("Game begins") ||
                cleanMessage.contains("game begins") ||
                cleanMessage.contains("Starting in") ||
                cleanMessage.contains("starting in") ||
                // PRIORITY: Any countdown pattern - very early indicator
                cleanMessage.matches(".*\\d+\\s*seconds?.*") ||
                cleanMessage.matches(".*\\d+\\s*second.*") ||
                // PRIORITY: Game state changes - immediate
                cleanMessage.toLowerCase().contains("starting") ||
                cleanMessage.toLowerCase().contains("started") ||
                cleanMessage.toLowerCase().contains("begins") ||
                cleanMessage.toLowerCase().contains("begin") ||
                // PRIORITY: Early role assignments (if they appear early)
                cleanMessage.contains("You are a Murderer!") || 
                cleanMessage.contains("You are an Innocent!") || 
                cleanMessage.contains("You are a Detective!") ||
                cleanMessage.contains("You are the Murderer!") ||
                cleanMessage.contains("You are the Detective!") ||
                cleanMessage.contains("You are an innocent!") ||
                cleanMessage.contains("You are the innocent!") ||
                // PRIORITY: Early gameplay instructions
                cleanMessage.contains("Find and eliminate") ||
                cleanMessage.contains("Collect 10 gun powder") ||
                cleanMessage.contains("Hide from the murderer") ||
                // PRIORITY: Titles/actionbar changes
                cleanMessage.contains("MURDERER") ||
                cleanMessage.contains("DETECTIVE") ||
                cleanMessage.contains("INNOCENT")
            )) {
                isInGame = true;
                lobbyJoinTime = 0; // Reset timer since we detected game start
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            
            // BACKUP TRIGGERS - Weapon distribution (happens later, around 10s delay)
            else if (isInLobby && !isInGame && (
                // BACKUP: Weapon distribution - these happen too late but kept as backup
                cleanMessage.contains("The Murderer gets their sword in") ||
                cleanMessage.contains("The Murderer has received their sword!") ||
                cleanMessage.contains("murderer gets their sword") ||
                cleanMessage.contains("murderer has received") ||
                cleanMessage.contains("sword in") ||
                cleanMessage.contains("detective bow") ||
                cleanMessage.contains("Detective bow") ||
                cleanMessage.contains("bow in")
            )) {
                isInGame = true;
                lobbyJoinTime = 0; // Reset timer since we detected game start
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            
            // ADDITIONAL MURDER MYSTERY SPECIFIC TRIGGERS
            else if (isInLobby && !isInGame && (
                // Murder Mystery specific instruction messages
                cleanMessage.contains("Find and eliminate") ||
                cleanMessage.contains("Collect 10 gun powder") ||
                cleanMessage.contains("Hide from the murderer") ||
                cleanMessage.contains("find and eliminate") ||
                cleanMessage.contains("collect 10 gun powder") ||
                cleanMessage.contains("hide from the murderer") ||
                cleanMessage.contains("eliminate the murderer") ||
                cleanMessage.contains("Eliminate the murderer") ||
                // Murder Mystery specific weapon receiving messages  
                (cleanMessage.contains("You have received") && cleanMessage.contains("bow")) ||
                (cleanMessage.contains("received") && cleanMessage.contains("sword")) ||
                (cleanMessage.contains("You have received") && cleanMessage.contains("sword")) ||
                cleanMessage.contains("received a bow") ||
                cleanMessage.contains("received sword") ||
                // Murder Mystery specific grace period
                cleanMessage.contains("Grace period") ||
                cleanMessage.contains("Grace Period") ||
                cleanMessage.contains("grace period") ||
                // Murder Mystery specific gameplay messages
                cleanMessage.contains("Innocents win") ||
                cleanMessage.contains("Murderer wins") ||
                cleanMessage.contains("Detective wins") ||
                cleanMessage.contains("innocents win") ||
                cleanMessage.contains("murderer wins") ||
                cleanMessage.contains("detective wins") ||
                // Any weapon distribution timing
                cleanMessage.contains(" in ") && (cleanMessage.contains("second") || cleanMessage.contains("bow") || cleanMessage.contains("sword")) ||
                // Murder Mystery specific instructions that appear at game start
                cleanMessage.contains("Don't let") && cleanMessage.contains("see you") ||
                cleanMessage.contains("don't let") && cleanMessage.contains("see you") ||
                cleanMessage.contains("Stay hidden") ||
                cleanMessage.contains("stay hidden")
            )) {
                isInGame = true;
                lobbyJoinTime = 0; // Reset timer since we detected explicit game start
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            
            // GAME END DETECTION
            if (isInGame && (message.contains("GAME OVER") || message.contains("Winner") || message.contains("won the game") || message.contains("Sending you to") || message.contains("You have been eliminated") || message.contains("Play Again") || message.contains("Thanks for playing") || message.contains("joined the lobby!"))) {
                isInGame = false;
                isInLobby = false; // Reset lobby state when game ends
                lobbyJoinTime = 0; // Reset timer
                isDoubleUpMode = false; // Reset mode when game ends
                MurderDetectionHandler.clearLists();
                MurderDetectionHandler.setDoubleUpMode(false); // Ensure detection handler is also reset
                TabListRenderer.resetDisplayNames();
            }
        }
        catch (Exception e) {
            System.err.println("Murder Mystery Helper: Error processing chat for game state: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        hasPolledCurrentWorld = false;
        isAwaitingLocationResponse = false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            if (isInGame || isInLobby) {
                isInGame = false;
                isInLobby = false;
                lobbyJoinTime = 0; // Reset timer
                isDoubleUpMode = false;
                MurderDetectionHandler.clearLists();
                MurderDetectionHandler.setDoubleUpMode(false); // Ensure detection handler is also reset
                TabListRenderer.resetDisplayNames();
            }
            hasPolledCurrentWorld = false;
            isAwaitingLocationResponse = false;
            return;
        }
        if (!hasPolledCurrentWorld) {
            if (++tickCounter >= 5) { // Reduced from 20 to 5 ticks (0.25 seconds) for faster mode detection
                mc.thePlayer.sendChatMessage("/locraw");
                hasPolledCurrentWorld = true;
                isAwaitingLocationResponse = true;
                tickCounter = 0;
            }
        } else if (isAwaitingLocationResponse) {
            if (++tickCounter >= 60) {
                isAwaitingLocationResponse = false;
                tickCounter = 0;
            }
        } else {
            tickCounter = 0;
        }
        
        // BACKUP TIMER: Auto-activate mod if we've been in lobby for too long without explicit game start detection
        if (isInLobby && !isInGame && lobbyJoinTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lobbyJoinTime;
            

            
            if (elapsed >= AUTO_ACTIVATE_DELAY) {
                isInGame = true;
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
        }
    }

    private void handleLocrawResponse(String jsonMessage) {
        try {
            boolean isMurderMystery;
            boolean isMurderDoubleUp;
            
            // Check for regular Murder Mystery mode - try multiple patterns
            isMurderMystery = jsonMessage.contains("\"gametype\":\"MURDER_MYSTERY\"") || 
                             jsonMessage.contains("\"MURDER_MYSTERY\"") || 
                             jsonMessage.contains("Murder Mystery") ||
                             jsonMessage.toLowerCase().contains("murder");
            
            // Check for Double Up mode - try multiple possible patterns
            isMurderDoubleUp = jsonMessage.contains("\"gametype\":\"MURDER_DOUBLE_UP\"") || 
                              jsonMessage.contains("\"MURDER_DOUBLE_UP\"") ||
                              jsonMessage.contains("\"mode\":\"MURDER_DOUBLE_UP\"") ||
                              jsonMessage.contains("\"mode\":\"Double Up\"") ||
                              jsonMessage.contains("Double Up") ||
                              jsonMessage.toLowerCase().contains("double");
            
            if (isMurderMystery || isMurderDoubleUp) {
                // Only set the mode type, don't activate the mod yet
                // The mod will activate when the "Teaming" message appears (actual game start)
                if (isMurderDoubleUp) {
                    isDoubleUpMode = true;
                } else {
                    isDoubleUpMode = false;
                }
                // Notify MurderDetectionHandler about the mode change
                MurderDetectionHandler.setDoubleUpMode(isDoubleUpMode);
            } else if (isInGame || isInLobby) {
                // Player left Murder Mystery entirely
                isInGame = false;
                isInLobby = false;
                lobbyJoinTime = 0; // Reset timer
                isDoubleUpMode = false;
                MurderDetectionHandler.clearLists();
                MurderDetectionHandler.setDoubleUpMode(false); // Ensure detection handler is also reset
                TabListRenderer.resetDisplayNames();
            }
        }
        catch (Exception e) {
            System.err.println("Murder Mystery Helper: Error parsing locraw response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isInGame() {
        return isInGame;
    }

    public static boolean isDoubleUpMode() {
        return isDoubleUpMode;
    }

    public static boolean isInLobby() {
        return isInLobby;
    }

    public static void setInGame(boolean inGame) {
        boolean wasInGame = isInGame;
        isInGame = inGame;
        if (wasInGame != inGame) {
            if (!inGame) {
                isInLobby = false;
                lobbyJoinTime = 0; // Reset timer
                isDoubleUpMode = false;
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
        }
    }

    public static void reset() {
        isInGame = false;
        isInLobby = false;
        lobbyJoinTime = 0; // Reset timer
        isDoubleUpMode = false;
        MurderDetectionHandler.clearLists();
        TabListRenderer.resetDisplayNames();
    }
}
