package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.HashSet;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GoldItemHider {
    
    private static Set<EntityItem> hiddenItems = new HashSet<EntityItem>();
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        
        // Reset all previously hidden items
        for (EntityItem item : hiddenItems) {
            if (item != null && !item.isDead) {
                item.setInvisible(false);
            }
        }
        hiddenItems.clear();
        
        // Hide gold items by making them invisible
        for (Object entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityItem) {
                EntityItem entityItem = (EntityItem) entity;
                ItemStack stack = entityItem.getEntityItem();
                
                // Hide gold items by making them invisible
                if (WeaponDetector.isGoldItem(stack)) {
                    entityItem.setInvisible(true);
                    hiddenItems.add(entityItem);
                }
            }
        }
    }
} 