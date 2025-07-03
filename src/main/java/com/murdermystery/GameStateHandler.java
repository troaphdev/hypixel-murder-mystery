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
                    System.out.println("Murder Mystery Helper: Hiding locraw related message: " + message);
                    event.setCanceled(true);
                    isAwaitingLocationResponse = false;
                    return;
                }
            }
            if (message.contains("Teaming with the Murderer is not allowed!") || message.contains("Teaming with the Detective/Innocents is not allowed!") || message.contains("Teaming with the Detective") && message.contains("not allowed")) {
                isInGame = true;
                System.out.println("Murder Mystery Helper: Game started detected via chat: " + message);
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            if (isInGame && (message.contains("GAME OVER") || message.contains("Winner") || message.contains("won the game") || message.contains("Sending you to") || message.contains("You have been eliminated") || message.contains("Play Again") || message.contains("Thanks for playing") || message.contains("joined the lobby!"))) {
                isInGame = false;
                System.out.println("Murder Mystery Helper: Game ended detected via chat message: " + message);
                MurderDetectionHandler.clearLists();
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
        System.out.println("Murder Mystery Helper: World changed, will poll location on next tick.");
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
                System.out.println("Murder Mystery Helper: Player disconnected, resetting game state.");
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            }
            hasPolledCurrentWorld = false;
            isAwaitingLocationResponse = false;
            return;
        }
        if (!hasPolledCurrentWorld) {
            if (++tickCounter >= 20) {
                mc.thePlayer.sendChatMessage("/locraw");
                hasPolledCurrentWorld = true;
                isAwaitingLocationResponse = true;
                tickCounter = 0;
                System.out.println("Murder Mystery Helper: Sent /locraw command to detect current location.");
            }
        } else if (isAwaitingLocationResponse) {
            if (++tickCounter >= 60) {
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
            boolean isMurderMystery;
            boolean isMurderDoubleUp;
            System.out.println("Murder Mystery Helper: Received locraw response: " + jsonMessage);
            
            // Enhanced debugging - show what we're looking for
            System.out.println("Murder Mystery Helper: Checking for Murder Mystery patterns...");
            
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
            
            System.out.println("Murder Mystery Helper: isMurderMystery=" + isMurderMystery + ", isMurderDoubleUp=" + isMurderDoubleUp);
            
            if (isMurderMystery || isMurderDoubleUp) {
                isInGame = true;
                if (isMurderDoubleUp) {
                    isDoubleUpMode = true;
                    System.out.println("Murder Mystery Helper: *** DETECTED MURDER MYSTERY DOUBLE UP MODE ***");
                } else {
                    isDoubleUpMode = false;
                    System.out.println("Murder Mystery Helper: *** DETECTED MURDER MYSTERY SOLO MODE ***");
                }
                // Notify MurderDetectionHandler about the mode change
                MurderDetectionHandler.setDoubleUpMode(isDoubleUpMode);
                System.out.println("Murder Mystery Helper: Notified MurderDetectionHandler. Double Up mode: " + isDoubleUpMode);
            } else if (isInGame) {
                isInGame = false;
                isDoubleUpMode = false;
                System.out.println("Murder Mystery Helper: Left Murder Mystery, disabling mod features.");
                MurderDetectionHandler.clearLists();
                TabListRenderer.resetDisplayNames();
            } else {
                System.out.println("Murder Mystery Helper: Not in Murder Mystery game based on locraw response.");
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
            if (inGame) {
                System.out.println("Murder Mystery Helper: Manual game state set to IN GAME");
            } else {
                isDoubleUpMode = false;
                System.out.println("Murder Mystery Helper: Manual game state set to OUT OF GAME");
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
        System.out.println("Murder Mystery Helper: Game state reset.");
    }
}
