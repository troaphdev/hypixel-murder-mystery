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
    private static boolean isDoubleUpMode = false;
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
            if (message.contains("Teaming with the Murderer is not allowed!") || message.contains("Teaming with the Detective/Innocents is not allowed!") || message.contains("Teaming with the Detective") && message.contains("not allowed")) {
                isInGame = true;
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            if (isInGame && (message.contains("GAME OVER") || message.contains("Winner") || message.contains("won the game") || message.contains("Sending you to") || message.contains("You have been eliminated") || message.contains("Play Again") || message.contains("Thanks for playing") || message.contains("joined the lobby!"))) {
                isInGame = false;
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
            if (isInGame) {
                isInGame = false;
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
                isInGame = true;
                if (isMurderDoubleUp) {
                    isDoubleUpMode = true;
                } else {
                    isDoubleUpMode = false;
                }
                // Notify MurderDetectionHandler about the mode change
                MurderDetectionHandler.setDoubleUpMode(isDoubleUpMode);
            } else if (isInGame) {
                isInGame = false;
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

    public static void setInGame(boolean inGame) {
        boolean wasInGame = isInGame;
        isInGame = inGame;
        if (wasInGame != inGame) {
            if (!inGame) {
                isDoubleUpMode = false;
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
        }
    }

    public static void reset() {
        isInGame = false;
        isDoubleUpMode = false;
        MurderDetectionHandler.clearLists();
        TabListRenderer.resetDisplayNames();
    }
}
