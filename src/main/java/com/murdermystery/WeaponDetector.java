package com.murdermystery;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.util.HashSet;
import java.util.Set;

public class WeaponDetector {
    
    // Murder weapon item IDs as specified by the user
    private static final Set<Integer> MURDER_WEAPON_IDS = new HashSet<Integer>();
    
    static {
        // Initialize the weapon IDs from the provided list
        int[] weaponIds = {
            267, 272, 268, 283, 276, 256, 273, 277, 280, 271, 32, 369, 406, 285, 400, 260, 421, 19, 398, 391, 396, 352, 279, 409, 364, 405, 366, 2258, 294, 293, 359, 333, 382, 340, 166, 2256, 2257, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266, 2267, 357, 297, 381, 325, 318, 286, 334
        };
        
        for (int id : weaponIds) {
            MURDER_WEAPON_IDS.add(id);
        }
        
        // Special handling for items with metadata
        // 175:4 (Double Tall Grass), 349:1 (Cooked Fish), 350:1 (Cooked Salmon), 6:3 (Birch Sapling), 263:1 (Charcoal), 351:1 (Red Dye), 351:4 (Lapis Lazuli)
        // These will be handled in the detection method
    }
    
    public static boolean isMurderWeapon(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        
        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }
        
        int itemId = Item.getIdFromItem(item);
        int metadata = itemStack.getMetadata();
        
        // Check basic item IDs
        if (MURDER_WEAPON_IDS.contains(itemId)) {
            return true;
        }
        
        // Check items with specific metadata
        if (itemId == 175 && metadata == 4) return true; // Double Tall Grass
        if (itemId == 349 && metadata == 1) return true; // Cooked Fish
        if (itemId == 350 && metadata == 1) return true; // Cooked Salmon
        if (itemId == 6 && metadata == 3) return true;   // Birch Sapling
        if (itemId == 263 && metadata == 1) return true; // Charcoal
        if (itemId == 351 && metadata == 1) return true; // Red Dye
        if (itemId == 351 && metadata == 4) return true; // Lapis Lazuli
        
        return false;
    }
    
    public static boolean hasAnyMurderWeapon(net.minecraft.entity.player.EntityPlayer player) {
        if (player == null || player.inventory == null) {
            return false;
        }
        
        // Check main inventory
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (isMurderWeapon(stack)) {
                return true;
            }
        }
        
        // Check armor slots
        for (int i = 0; i < player.inventory.armorInventory.length; i++) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (isMurderWeapon(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isDetectiveWeapon(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        
        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }
        
        int itemId = Item.getIdFromItem(item);
        
        // Check for bow (261) or arrow (262)
        return itemId == 261 || itemId == 262;
    }
    
    public static boolean hasAnyDetectiveWeapon(net.minecraft.entity.player.EntityPlayer player) {
        if (player == null || player.inventory == null) {
            return false;
        }
        
        // Check main inventory
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (isDetectiveWeapon(stack)) {
                return true;
            }
        }
        
        // Check armor slots
        for (int i = 0; i < player.inventory.armorInventory.length; i++) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (isDetectiveWeapon(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isGoldItem(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        
        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }
        
        int itemId = Item.getIdFromItem(item);
        
        // Check for gold items (ingot, nugget, block)
        return itemId == 266 || itemId == 371 || itemId == 41;
    }
} 