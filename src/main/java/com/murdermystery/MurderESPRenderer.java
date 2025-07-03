package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
public class MurderESPRenderer {
    
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // Only render ESP when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        
        // Allow ESP rendering if we have detected targets OR if the player is the murderer (to show green victim ESP)
        if (!MurderDetectionHandler.hasAnyTargets() && !TitleHandler.isPlayerMurderer()) {
            return;
        }
        
        List<String> murderers = MurderDetectionHandler.getMurdererList();
        List<String> detectives = MurderDetectionHandler.getDetectiveList();
        List<EntityArmorStand> droppedBows = MurderDetectionHandler.getDroppedBowList();
        
        // Set up OpenGL for ESP rendering
        setupESPRendering();
        
        // Get player position for relative rendering
        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;
        
        // Ensure consistent line width by setting it before each render
        GL11.glLineWidth(2.0F);
        
        // Render ESP for each target (murderers, detectives, and victims)
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player == mc.thePlayer) {
                continue;
            }
            
            String playerName = player.getName();
            boolean isGoldenDetective = MurderDetectionHandler.isPlayerGoldenDetective(playerName);
            
            // Check if current player is the murderer
            if (TitleHandler.isPlayerMurderer()) {
                // Player is murderer - show victims and detectives
                if (detectives.contains(playerName)) {
                    // Check if this is a golden detective
                    if (isGoldenDetective) {
                        // Gold ESP for golden detective
                        renderPlayerESP(player, playerX, playerY, playerZ, event.partialTicks, "firstbow");
                    } else {
                        // Light blue ESP for regular detectives
                        renderPlayerESP(player, playerX, playerY, playerZ, event.partialTicks, "detective");
                    }
                } else {
                    // Lime ESP for all other players (potential victims)
                    renderPlayerESP(player, playerX, playerY, playerZ, event.partialTicks, "victim");
                }
            } else {
                // Normal mode - show murderers and detectives
                if (murderers.contains(playerName)) {
                    // Red ESP for murderers (highest priority)
                    renderPlayerESP(player, playerX, playerY, playerZ, event.partialTicks, "murderer");
                } else if (detectives.contains(playerName)) {
                    // Check if this is a golden detective
                    if (isGoldenDetective) {
                        // Gold ESP for golden detective
                        renderPlayerESP(player, playerX, playerY, playerZ, event.partialTicks, "firstbow");
                    } else {
                        // Light blue ESP for regular detectives
                        renderPlayerESP(player, playerX, playerY, playerZ, event.partialTicks, "detective");
                    }
                }
            }
        }
        
        // Render ESP for dropped detective bows
        for (EntityArmorStand armorStand : droppedBows) {
            if (armorStand != null && !armorStand.isDead) {
                renderDroppedBowESP(armorStand, playerX, playerY, playerZ, event.partialTicks);
            }
        }
        
        // Clean up OpenGL state
        cleanupESPRendering();
    }
    
    private void setupESPRendering() {
        // Save current OpenGL state
        GlStateManager.pushMatrix();
        
        // Enable blending for transparency
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        
        // Disable depth test to render through walls
        GlStateManager.disableDepth();
        
        // Disable textures
        GlStateManager.disableTexture2D();
        
        // Disable lighting
        GlStateManager.disableLighting();
        
        // Disable cull face to see lines from all angles
        GlStateManager.disableCull();
        
        // Set consistent line width regardless of distance
        GL11.glLineWidth(2.0F);
        
        // Enable line smooth for better appearance
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        
        // Disable alpha test
        GlStateManager.disableAlpha();
    }
    
    private void cleanupESPRendering() {
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
        
        // Reset line width
        GL11.glLineWidth(1.0F);
        
        // Disable line smooth
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        
        // Reset color to white
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Restore matrix state
        GlStateManager.popMatrix();
    }
    
    private void renderDroppedBowESP(EntityArmorStand armorStand, double playerX, double playerY, double playerZ, float partialTicks) {
        // Ensure consistent line width for this dropped bow
        GL11.glLineWidth(2.0F);
        
        // Calculate armor stand's interpolated position
        double x = armorStand.lastTickPosX + (armorStand.posX - armorStand.lastTickPosX) * partialTicks - playerX;
        double y = armorStand.lastTickPosY + (armorStand.posY - armorStand.lastTickPosY) * partialTicks - playerY;
        double z = armorStand.lastTickPosZ + (armorStand.posZ - armorStand.lastTickPosZ) * partialTicks - playerZ;
        
        // Create a square bounding box for the dropped bow (1x1x1 cube)
        double size = 0.5; // Half of 1.0 for radius
        AxisAlignedBB boundingBox = new AxisAlignedBB(
            x - size, y, z - size,
            x + size, y + (size * 2), z + size
        );
        
        // Set golden color for dropped detective bow
        GlStateManager.color(1.0F, 0.8F, 0.0F, 1.0F); // Golden color
        
        drawWireframeBox(boundingBox);
    }
    
    private void renderPlayerESP(EntityPlayer player, double playerX, double playerY, double playerZ, float partialTicks, String playerType) {
        // Ensure consistent line width for this player
        GL11.glLineWidth(2.0F);
        
        // Calculate player's interpolated position
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - playerX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - playerY;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - playerZ;
        
        // Create bounding box around the player (accurate Minecraft player dimensions: 0.6x1.8x0.6)
        AxisAlignedBB boundingBox = new AxisAlignedBB(
            x - 0.3, y, z - 0.3,
            x + 0.3, y + 1.8, z + 0.3
        );
        
        // Set color and render type based on player type
        if (playerType.equals("murderer")) {
            // Bright red color for murderers (solid)
            GlStateManager.color(1.0F, 0.0F, 0.0F, 0.5F);
            drawFilledBox(boundingBox);
        } else if (playerType.equals("detective")) {
            // Light blue color for detectives (solid)
            GlStateManager.color(0.0F, 0.7F, 1.0F, 0.5F);
            drawFilledBox(boundingBox);
        } else if (playerType.equals("firstbow")) {
            // Gold color for first bow holder (solid)
            GlStateManager.color(1.0F, 0.8F, 0.0F, 0.5F);
            drawFilledBox(boundingBox);
        } else if (playerType.equals("victim")) {
            // Lime color for victims (wireframe only)
            GlStateManager.color(0.0F, 1.0F, 0.0F, 1.0F);
            drawWireframeBox(boundingBox);
        }
    }
    
    private void drawWireframeBox(AxisAlignedBB box) {
        // Ensure line width is consistent before drawing
        GL11.glLineWidth(2.0F);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        
        // Bottom face
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        
        // Top face
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        
        // Vertical edges
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        
        tessellator.draw();
    }
    
    private void drawFilledBox(AxisAlignedBB box) {
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
    }
} 