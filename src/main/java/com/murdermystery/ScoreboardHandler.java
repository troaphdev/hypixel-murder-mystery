package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

@SideOnly(Side.CLIENT)
public class ScoreboardHandler {
    
    private static int tickCounter = 0;
    private static boolean hasDetectedTotalWins = false;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Only check every 20 ticks (1 second) for performance
        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            hasDetectedTotalWins = false;
            return;
        }
        
        // Check scoreboard for "Total Wins:" text
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return;
        }
        
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // Sidebar slot
        if (objective == null) {
            return;
        }
        
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        boolean foundTotalWins = false;
        
        for (Score score : scores) {
            if (score == null || score.getPlayerName() == null) {
                continue;
            }
            
            String scoreText = score.getPlayerName();
            // Remove color codes and check for "Total Wins:"
            String cleanText = scoreText.replaceAll("§[0-9a-fk-or]", "");
            
            if (cleanText.contains("Total Wins:")) {
                foundTotalWins = true;
                break;
            }
        }
        
        // If we found "Total Wins:" and haven't already detected it this session
        if (foundTotalWins && !hasDetectedTotalWins) {
            hasDetectedTotalWins = true;
                                System.out.println("Murder Mystery Helper: Detected 'Total Wins:' on scoreboard, clearing lists and resetting tab list.");
            MurderDetectionHandler.clearLists();
            TabListRenderer.resetDisplayNames();
        } else if (!foundTotalWins) {
            // Reset detection flag when "Total Wins:" is no longer visible
            hasDetectedTotalWins = false;
        }
    }
} 