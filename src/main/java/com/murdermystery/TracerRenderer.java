package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
public class TracerRenderer {
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        // Only render tracers when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        
        // Get murderer list
        List<String> murderers = MurderDetectionHandler.getMurdererList();
        if (murderers.isEmpty()) {
            return;
        }
        
        // Set up OpenGL for tracer rendering
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // Render through walls
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        
        // Get camera position
        double cameraX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double cameraY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks + mc.thePlayer.getEyeHeight();
        double cameraZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;
        
        // Get render view position for coordinate transformation
        double renderViewX = mc.getRenderManager().viewerPosX;
        double renderViewY = mc.getRenderManager().viewerPosY;
        double renderViewZ = mc.getRenderManager().viewerPosZ;
        
        // Draw tracers to each murderer
        for (String murdererName : murderers) {
            EntityPlayer murderer = mc.theWorld.getPlayerEntityByName(murdererName);
            if (murderer == null || murderer == mc.thePlayer) {
                continue;
            }
            
            // Calculate murderer position (interpolated)
            double murdererX = murderer.lastTickPosX + (murderer.posX - murderer.lastTickPosX) * event.partialTicks;
            double murdererY = murderer.lastTickPosY + (murderer.posY - murderer.lastTickPosY) * event.partialTicks;
            double murdererZ = murderer.lastTickPosZ + (murderer.posZ - murderer.lastTickPosZ) * event.partialTicks;
            
            // Calculate direction vector from camera to murderer center
            double deltaX = murdererX - cameraX;
            double deltaY = (murdererY + 0.9) - cameraY; // Target center of hitbox (half of 1.8 height)
            double deltaZ = murdererZ - cameraZ;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            
            // Calculate line thickness based on distance (closer = thicker)
            float lineWidth = Math.max(1.0F, 8.0F - (float)(distance / 10.0) * 0.7F);
            GL11.glLineWidth(lineWidth);
            
            // Normalize direction vector
            if (distance > 0) {
                deltaX /= distance;
                deltaY /= distance;
                deltaZ /= distance;
            }
            
            // Stop the line before the hitbox to prevent overlap
            // Hitbox radius is 0.3 blocks, so stop 0.8 blocks before center for clean gap
            double stopDistance = Math.max(1.0, distance - 0.8);
            double endX = cameraX + deltaX * stopDistance;
            double endY = cameraY + deltaY * stopDistance;
            double endZ = cameraZ + deltaZ * stopDistance;
            
            // Transform to render space coordinates
            double startX = cameraX - renderViewX;
            double startY = cameraY - renderViewY;
            double startZ = cameraZ - renderViewZ;
            
            double renderEndX = endX - renderViewX;
            double renderEndY = endY - renderViewY;
            double renderEndZ = endZ - renderViewZ;
            
            // Start tessellator for this line
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer renderer = tessellator.getWorldRenderer();
            
            renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            
            // Draw red tracer line (stops before hitbox)
            renderer.pos(startX, startY, startZ).color(1.0F, 0.0F, 0.0F, 0.8F).endVertex(); // Bright red, start
            renderer.pos(renderEndX, renderEndY, renderEndZ).color(1.0F, 0.0F, 0.0F, 0.8F).endVertex(); // Bright red, before hitbox
            
            // Render this line
            tessellator.draw();
        }
        
        // Reset line width and restore OpenGL state
        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
} 