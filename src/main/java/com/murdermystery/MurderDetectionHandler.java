package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class MurderDetectionHandler {
    
    private static final List<String> murdererList = new ArrayList<String>();
    private static final Set<String> detectedMurderers = new HashSet<String>();
    private static final List<String> detectiveList = new ArrayList<String>();
    private static final Set<String> detectedDetectives = new HashSet<String>();
    private static final List<EntityArmorStand> droppedBowList = new ArrayList<EntityArmorStand>();
    private static final Set<Integer> detectedDroppedBows = new HashSet<Integer>();
    private static String lastWorldName = "";
    private static long lastLobbyChangeTime = 0;
    private static Set<String> lastPlayerSet = new HashSet<String>();
    private static long lastClearTime = 0;
    private static final long CLEAR_COOLDOWN = 2000; // Reduced to 2 seconds cooldown after clearing
    private static String firstBowHolder = null; // First player to hold a bow (repeatable bow holder)
    private static String secondBowHolder = null; // Second player to hold a bow in Double Up mode
    private static boolean isDoubleUpMode = false; // Track if we're in Double Up mode
    
    // Detective check variables
    private static long detectiveCheckScheduledTime = 0;
    private static boolean detectiveCheckScheduled = false;
    private static final long DETECTIVE_CHECK_DELAY = 100; // 5 ticks (100ms) delay
    private static boolean playerIsStartingDetective = false;
    
    // Performance optimization - only scan dropped bows every 5 ticks
    private static int droppedBowScanCounter = 0;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Only detect players when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        
        // Handle scheduled detective check
        if (detectiveCheckScheduled) {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= detectiveCheckScheduledTime) {
                performDetectiveCheck();
                detectiveCheckScheduled = false;
            }
        }
        
        // ULTRA CONSERVATIVE: Only clear on actual world dimension change
        // No automatic clearing based on time or player changes during active gameplay
        String currentWorldName = getWorldIdentifier(mc.theWorld);
        if (!currentWorldName.equals(lastWorldName)) {
            System.out.println("Murder Mystery Helper: World/dimension changed, clearing lists.");
            clearMurdererList();
            lastWorldName = currentWorldName;
            lastLobbyChangeTime = System.currentTimeMillis();
        }
        
        // Check if we're still in cooldown period (reduced to 2 seconds)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClearTime < CLEAR_COOLDOWN) {
            return;
        }
        
        // IMMEDIATE scanning for murderers (no throttling)
        scanForMurderers();
        
        // Scan dropped bows less frequently for performance (every 5 ticks)
        droppedBowScanCounter++;
        if (droppedBowScanCounter >= 5) {
            droppedBowScanCounter = 0;
            scanForDroppedBows();
        }
    }
    
    private void performDetectiveCheck() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        
        // Check if the current player has detective weapons (bow/arrow)
        if (WeaponDetector.hasAnyDetectiveWeapon(mc.thePlayer)) {
            playerIsStartingDetective = true;
            System.out.println("Murder Mystery Helper: Current player is the starting detective! Disabling golden highlighting for this round.");
        } else {
            playerIsStartingDetective = false;
            System.out.println("Murder Mystery Helper: Current player is NOT the starting detective. Golden highlighting enabled.");
        }
    }
    
    public static void scheduleDetectiveCheck() {
        detectiveCheckScheduledTime = System.currentTimeMillis() + DETECTIVE_CHECK_DELAY;
        detectiveCheckScheduled = true;
        System.out.println("Murder Mystery Helper: Detective check scheduled for " + DETECTIVE_CHECK_DELAY + "ms from now.");
    }

    public static void setDoubleUpMode(boolean doubleUp) {
        isDoubleUpMode = doubleUp;
        if (doubleUp) {
            System.out.println("Murder Mystery Helper: Double Up mode enabled - will track 2 golden detectives.");
        } else {
            System.out.println("Murder Mystery Helper: Solo mode enabled - will track 1 golden detective.");
        }
    }
    
    private void scanForMurderers() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.getNetHandler() == null) {
            return;
        }
        
        // Get all players in the world - IMMEDIATE scanning
        List<EntityPlayer> worldPlayers = mc.theWorld.playerEntities;
        
        // Scan for new murderers and detectives (players currently holding weapons)
        for (EntityPlayer player : worldPlayers) {
            if (player == null || player.getName() == null) {
                continue;
            }
            
            // Skip the current player
            if (player == mc.thePlayer) {
                continue;
            }
            
            String playerName = player.getName();
            
            // Check if player has murder weapons (highest priority) - IMMEDIATE
            if (WeaponDetector.hasAnyMurderWeapon(player)) {
                // Add to murderer list if not already present
                if (!detectedMurderers.contains(playerName)) {
                    detectedMurderers.add(playerName);
                    murdererList.add(playerName);
                    System.out.println("Murder Mystery Helper: Detected " + playerName + " with a murder weapon!");
                }
            }
            // Check if player has detective weapons (only if not already a murderer)
            else if (WeaponDetector.hasAnyDetectiveWeapon(player) && !detectedMurderers.contains(playerName)) {
                
                // Golden detective logic - different behavior for Solo vs Double Up
                if (!playerIsStartingDetective) {
                    if (isDoubleUpMode) {
                        // Double Up mode: track first 2 bow holders
                        if (firstBowHolder == null) {
                            firstBowHolder = playerName;
                            System.out.println("Murder Mystery Helper: " + playerName + " is the first golden detective (Double Up mode)!");
                        } else if (secondBowHolder == null && !playerName.equals(firstBowHolder)) {
                            secondBowHolder = playerName;
                            System.out.println("Murder Mystery Helper: " + playerName + " is the second golden detective (Double Up mode)!");
                        }
                    } else {
                        // Solo mode: track only first bow holder
                        if (firstBowHolder == null) {
                            firstBowHolder = playerName;
                            System.out.println("Murder Mystery Helper: " + playerName + " is the first bow holder (Solo mode)!");
                        }
                    }
                }
                
                // Add to detective list if not already present
                if (!detectedDetectives.contains(playerName)) {
                    detectedDetectives.add(playerName);
                    detectiveList.add(playerName);
                    System.out.println("Murder Mystery Helper: Detected " + playerName + " with a bow/arrow (Detective)!");
                }
            }
        }
        
        // NOTE: We do NOT remove players based on whether they're in worldPlayers
        // because players can be unloaded when they're far away but still in the lobby.
        // Players are only removed when the world/lobby changes (handled by clearMurdererList)
    }
    
    private void scanForDroppedBows() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        
        // Clear old entries from the list to prevent memory leaks (Java 1.6 compatible)
        for (int i = droppedBowList.size() - 1; i >= 0; i--) {
            EntityArmorStand armorStand = droppedBowList.get(i);
            if (armorStand.isDead) {
                droppedBowList.remove(i);
                detectedDroppedBows.remove(armorStand.getEntityId());
            }
        }
        
        // Scan for armor stands holding bows
        for (Object entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand armorStand = (EntityArmorStand) entity;
                
                // Skip if already detected
                if (detectedDroppedBows.contains(armorStand.getEntityId())) {
                    continue;
                }
                
                // Check if the armor stand is holding a bow
                if (isArmorStandHoldingBow(armorStand)) {
                    detectedDroppedBows.add(armorStand.getEntityId());
                    droppedBowList.add(armorStand);
                    System.out.println("Murder Mystery Helper: Detected dropped detective bow at position: " + 
                        armorStand.posX + ", " + armorStand.posY + ", " + armorStand.posZ);
                }
            }
        }
    }
    
    private boolean isArmorStandHoldingBow(EntityArmorStand armorStand) {
        if (armorStand == null) {
            return false;
        }
        
        // Only consider invisible armor stands (dropped detective bows are invisible)
        if (!armorStand.isInvisible()) {
            return false;
        }
        
        // Check main hand (right hand)
        ItemStack mainHandItem = armorStand.getHeldItem();
        if (WeaponDetector.isDetectiveWeapon(mainHandItem)) {
            return true;
        }
        
        // Check equipment slots for bow/arrow
        for (int i = 0; i < 5; i++) { // 0-3 armor slots, 4 main hand
            ItemStack equipment = armorStand.getEquipmentInSlot(i);
            if (WeaponDetector.isDetectiveWeapon(equipment)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String getWorldIdentifier(World world) {
        // Create a unique identifier for the current world/lobby
        // This helps detect when the player switches lobbies
        if (world == null) {
            return "";
        }
        
        // Use only world seed - stable within the same lobby
        // Do NOT use player position as it changes when moving around the map!
        return String.valueOf(world.getSeed());
    }
    
    private void clearMurdererList() {
        murdererList.clear();
        detectedMurderers.clear();
        detectiveList.clear();
        detectedDetectives.clear();
        droppedBowList.clear();
        detectedDroppedBows.clear();
        firstBowHolder = null;
        secondBowHolder = null;
        lastPlayerSet.clear();
        lastClearTime = System.currentTimeMillis();
        // Reset detective-related flags
        playerIsStartingDetective = false;
        detectiveCheckScheduled = false;
        detectiveCheckScheduledTime = 0;
        TitleHandler.resetMurdererStatus();
        System.out.println("Murder Mystery Helper: All detection lists cleared. Cooldown active for 2 seconds.");
    }
    
    public static void clearLists() {
        murdererList.clear();
        detectedMurderers.clear();
        detectiveList.clear();
        detectedDetectives.clear();
        droppedBowList.clear();
        detectedDroppedBows.clear();
        firstBowHolder = null;
        secondBowHolder = null;
        lastPlayerSet.clear();
        lastClearTime = System.currentTimeMillis();
        // Reset detective-related flags
        playerIsStartingDetective = false;
        detectiveCheckScheduled = false;
        detectiveCheckScheduledTime = 0;
        TitleHandler.resetMurdererStatus();
        System.out.println("Murder Mystery Helper: Lists cleared by chat message detection. Cooldown active for 2 seconds.");
    }
    
    public static List<String> getMurdererList() {
        return new ArrayList<String>(murdererList);
    }
    
    public static boolean hasMurderers() {
        return !murdererList.isEmpty();
    }
    
    public static List<String> getDetectiveList() {
        return new ArrayList<String>(detectiveList);
    }
    
    public static boolean hasDetectives() {
        return !detectiveList.isEmpty();
    }
    
    public static List<EntityArmorStand> getDroppedBowList() {
        return new ArrayList<EntityArmorStand>(droppedBowList);
    }
    
    public static boolean hasDroppedBows() {
        return !droppedBowList.isEmpty();
    }
    
    public static boolean hasAnyTargets() {
        return !murdererList.isEmpty() || !detectiveList.isEmpty() || !droppedBowList.isEmpty();
    }
    
    public static String getFirstBowHolder() {
        // Don't show golden detective if the current player is the starting detective
        if (playerIsStartingDetective) {
            return null;
        }
        return firstBowHolder;
    }

    public static String getSecondBowHolder() {
        if (playerIsStartingDetective || !isDoubleUpMode) {
            return null;
        }
        return secondBowHolder;
    }

    public static boolean isPlayerGoldenDetective(String playerName) {
        if (playerIsStartingDetective || playerName == null) {
            return false;
        }
        if (isDoubleUpMode) {
            return playerName.equals(firstBowHolder) || playerName.equals(secondBowHolder);
        } else {
            return playerName.equals(firstBowHolder);
        }
    }
    
    public static void updateGoldenDetectiveFromBowPickup() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || droppedBowList.isEmpty()) {
            return;
        }
        
        // Don't update golden detective if the current player is the starting detective
        if (playerIsStartingDetective) {
            System.out.println("Murder Mystery Helper: Bow pickup detected, but current player is starting detective. Not updating golden detective.");
            return;
        }
        
        String closestPlayer = findClosestPlayerToDroppedBows();
        if (closestPlayer != null) {
            if (isDoubleUpMode) {
                // Double Up mode: assign to first available slot
                if (firstBowHolder == null) {
                    firstBowHolder = closestPlayer;
                    System.out.println("Murder Mystery Helper: " + closestPlayer + " is now the first golden detective (picked up bow - Double Up mode)!");
                } else if (secondBowHolder == null && !closestPlayer.equals(firstBowHolder)) {
                    secondBowHolder = closestPlayer;
                    System.out.println("Murder Mystery Helper: " + closestPlayer + " is now the second golden detective (picked up bow - Double Up mode)!");
                } else {
                    System.out.println("Murder Mystery Helper: Bow pickup detected but both golden detective slots are filled in Double Up mode.");
                }
            } else {
                // Solo mode: only one golden detective
                firstBowHolder = closestPlayer;
                System.out.println("Murder Mystery Helper: " + closestPlayer + " is now the golden detective (picked up bow - Solo mode)!");
            }
            
            // Clear the dropped bow list since someone picked it up
            droppedBowList.clear();
            detectedDroppedBows.clear();
            System.out.println("Murder Mystery Helper: Cleared dropped bow list (bow was picked up).");
        }
    }
    
    private static String findClosestPlayerToDroppedBows() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || droppedBowList.isEmpty()) {
            return null;
        }
        
        String closestPlayerName = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Check all players against all dropped bows
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player.getName() == null) {
                continue;
            }
            
            // Include the current player in the search
            // If they pick up the bow, they should be marked as golden detective too
            
            // Find closest distance to any dropped bow
            for (EntityArmorStand droppedBow : droppedBowList) {
                if (droppedBow == null || droppedBow.isDead) {
                    continue;
                }
                
                double distance = player.getDistanceToEntity(droppedBow);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayerName = player.getName();
                }
            }
        }
        
        // Only consider it a pickup if the player is within reasonable range (10 blocks)
        if (closestDistance <= 10.0) {
            return closestPlayerName;
        }
        
        return null;
    }
} 