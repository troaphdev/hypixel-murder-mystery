package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
public class GoldItemGlowRenderer {
    
    // Distance range for color gradient: 1 block = fully close color, 45 blocks = fully far color
    private static final double MIN_DISTANCE = 1.0;   // Fully close color
    private static final double MAX_DISTANCE = 45.0;  // Fully far color
    
    // Far color: 004d04 (RGB: 0, 77, 4) - Dark Green
    private static final float FAR_RED = 0f / 255f;     // 0.0
    private static final float FAR_GREEN = 77f / 255f;  // 0.302
    private static final float FAR_BLUE = 4f / 255f;    // 0.016
    
    // Close color: 00ff0e (RGB: 0, 255, 14) - Bright Green
    private static final float CLOSE_RED = 0f / 255f;     // 0.0
    private static final float CLOSE_GREEN = 255f / 255f; // 1.0
    private static final float CLOSE_BLUE = 14f / 255f;   // 0.055
    
    // Safety limits to prevent crashes
    private static final double MAX_RENDER_DISTANCE = 100.0; // Don't render beyond this distance
    private static final int MAX_ITEMS_PER_FRAME = 50; // Limit items processed per frame
    
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // Only render gold item glow when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) {
                return;
            }
            
            // Set up OpenGL for glow rendering
            setupGlowRendering();
            
            // Get player position for relative rendering
            double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
            double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
            double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;
            
            // Safety counter to prevent too many items from being processed
            int itemsProcessed = 0;
            
            // Scan for gold items and render glow effect
            for (Object entity : mc.theWorld.loadedEntityList) {
                if (itemsProcessed >= MAX_ITEMS_PER_FRAME) {
                    break; // Safety limit reached
                }
                
                if (entity instanceof EntityItem) {
                    EntityItem itemEntity = (EntityItem) entity;
                    ItemStack itemStack = itemEntity.getEntityItem();
                    
                    if (itemStack != null && WeaponDetector.isGoldItem(itemStack)) {
                        // Calculate distance for color gradient (simplified calculation)
                        double itemX = itemEntity.lastTickPosX + (itemEntity.posX - itemEntity.lastTickPosX) * event.partialTicks;
                        double itemY = itemEntity.lastTickPosY + (itemEntity.posY - itemEntity.lastTickPosY) * event.partialTicks;
                        double itemZ = itemEntity.lastTickPosZ + (itemEntity.posZ - itemEntity.lastTickPosZ) * event.partialTicks;
                        
                        // Simple distance calculation between player and item
                        double deltaX = itemX - playerX;
                        double deltaY = itemY - playerY;
                        double deltaZ = itemZ - playerZ;
                        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                        
                        // Safety check: don't render items that are too far away
                        if (distance <= MAX_RENDER_DISTANCE && !Double.isNaN(distance) && !Double.isInfinite(distance)) {
                            // Render glow with distance-based color
                            renderGoldItemGlow(itemEntity, playerX, playerY, playerZ, event.partialTicks, distance);
                            itemsProcessed++;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Catch any exceptions to prevent crashes
            System.err.println("Error in GoldItemGlowRenderer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always clean up OpenGL state, even if an error occurred
            try {
                cleanupGlowRendering();
            } catch (Exception e) {
                System.err.println("Error cleaning up glow rendering: " + e.getMessage());
            }
        }
    }
    
    private void setupGlowRendering() {
        // Save current OpenGL state
        GlStateManager.pushMatrix();
        
        // Enable proper alpha blending to preserve colors
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        
        // Disable depth test to render through walls
        GlStateManager.disableDepth();
        
        // Disable textures
        GlStateManager.disableTexture2D();
        
        // Disable lighting
        GlStateManager.disableLighting();
        
        // Disable cull face to see glow from all angles
        GlStateManager.disableCull();
        
        // Disable alpha test for smooth blending
        GlStateManager.disableAlpha();
    }
    
    private void cleanupGlowRendering() {
        // Re-enable depth test
        GlStateManager.enableDepth();
        
        // Re-enable textures
        GlStateManager.enableTexture2D();
        
        // Re-enable lighting
        GlStateManager.enableLighting();
        
        // Re-enable cull face
        GlStateManager.enableCull();
        
        // Re-enable alpha test
        GlStateManager.enableAlpha();
        
        // Disable blending
        GlStateManager.disableBlend();
        
        // Reset color to white
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Restore matrix state
        GlStateManager.popMatrix();
    }
    
    private void renderGoldItemGlow(EntityItem itemEntity, double playerX, double playerY, double playerZ, float partialTicks, double distance) {
        try {
            // Calculate item's interpolated position
            double x = itemEntity.lastTickPosX + (itemEntity.posX - itemEntity.lastTickPosX) * partialTicks - playerX;
            double y = itemEntity.lastTickPosY + (itemEntity.posY - itemEntity.lastTickPosY) * partialTicks - playerY;
            double z = itemEntity.lastTickPosZ + (itemEntity.posZ - itemEntity.lastTickPosZ) * partialTicks - playerZ;
            
            // Safety checks for position values
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || 
                Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
                return; // Skip rendering if positions are invalid
            }
            
            // Create smaller bounding box around the item
            double size = 0.2; // Smaller radius for compact glow orbs
            AxisAlignedBB boundingBox = new AxisAlignedBB(
                x - size, y - size, z - size,
                x + size, y + size, z + size
            );
            
            // Calculate distance-based color with safety checks
            double normalizedDistance;
            if (distance <= MIN_DISTANCE) {
                normalizedDistance = 0.0; // Fully close color
            } else if (distance >= MAX_DISTANCE) {
                normalizedDistance = 1.0; // Fully far color
            } else {
                double range = MAX_DISTANCE - MIN_DISTANCE;
                if (range > 0.0) {
                    normalizedDistance = (distance - MIN_DISTANCE) / range;
                } else {
                    normalizedDistance = 0.0; // Fallback to close color
                }
            }
            
            // Clamp normalized distance to valid range
            normalizedDistance = Math.max(0.0, Math.min(1.0, normalizedDistance));
            
             // Interpolate between close color (0000FF - blue) and far color (FF0000 - red)
            float red = CLOSE_RED + (FAR_RED - CLOSE_RED) * (float)normalizedDistance;
            float green = CLOSE_GREEN + (FAR_GREEN - CLOSE_GREEN) * (float)normalizedDistance;
            float blue = CLOSE_BLUE + (FAR_BLUE - CLOSE_BLUE) * (float)normalizedDistance;
            
            // Clamp color values to valid range
            red = Math.max(0.0f, Math.min(1.0f, red));
            green = Math.max(0.0f, Math.min(1.0f, green));
            blue = Math.max(0.0f, Math.min(1.0f, blue));
            
            // Safety check for color values
            if (Float.isNaN(red) || Float.isNaN(green) || Float.isNaN(blue) ||
                Float.isInfinite(red) || Float.isInfinite(green) || Float.isInfinite(blue)) {
                // Use default safe color if calculation failed
                red = 1.0f;
                green = 1.0f;
                blue = 0.0f; // Yellow fallback
            }
            
            // Render multiple glow layers for a proper glow effect with distance-based color
            // Outer glow (largest, most transparent)
            GlStateManager.color(red, green, blue, 0.3F);
            drawFilledGlowBox(boundingBox, 1.4);
            
            // Middle glow
            GlStateManager.color(red, green, blue, 0.5F);
            drawFilledGlowBox(boundingBox, 1.2);
            
            // Inner glow (brightest)
            GlStateManager.color(red, green, blue, 0.8F);
            drawFilledGlowBox(boundingBox, 1.0);
            
        } catch (Exception e) {
            System.err.println("Error rendering gold item glow: " + e.getMessage());
        }
    }
    
    private void drawFilledGlowBox(AxisAlignedBB box, double scale) {
        try {
            // Scale the box if needed
            if (scale != 1.0) {
                double centerX = (box.minX + box.maxX) / 2.0;
                double centerY = (box.minY + box.maxY) / 2.0;
                double centerZ = (box.minZ + box.maxZ) / 2.0;
                
                double halfWidth = (box.maxX - box.minX) / 2.0 * scale;
                double halfHeight = (box.maxY - box.minY) / 2.0 * scale;
                double halfDepth = (box.maxZ - box.minZ) / 2.0 * scale;
                
                box = new AxisAlignedBB(
                    centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth,
                    centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth
                );
            }
            
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            
            // Bottom face (Y = minY)
            worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
            worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
            worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
            worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
            
            // Top face (Y = maxY)
            worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
            worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
            worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
            worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
            
            // North face (Z = minZ)
            worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
            worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
            worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
            worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
            
            // South face (Z = maxZ)
            worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
            worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
            worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
            worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
            
            // West face (X = minX)
            worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
            worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
            worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
            worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
            
            // East face (X = maxX)
            worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
            worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
            worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
            worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
            
            tessellator.draw();
            
        } catch (Exception e) {
            System.err.println("Error drawing glow box: " + e.getMessage());
        }
    }
} 