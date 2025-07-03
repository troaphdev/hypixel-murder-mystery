package com.murdermystery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class NametagRenderer {

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post event) {
        if (!(event.entity instanceof EntityPlayer)) {
            return;
        }
        
        // Only render nametags when in a Murder Mystery game
        if (!GameStateHandler.isInGame()) {
            return;
        }
        
        EntityPlayer player = (EntityPlayer) event.entity;
        Minecraft mc = Minecraft.getMinecraft();
        
        // Don't render nametag for the local player
        if (player == mc.thePlayer) {
            return;
        }
        
        // Get the player's name
        String displayName = player.getDisplayName().getFormattedText();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = player.getName(); // Fallback to username
        }
        
        // Calculate distance to determine if we should render
        double distance = mc.thePlayer.getDistanceToEntity(player);
        if (distance > 64.0) { // Don't render nametags beyond 64 blocks
            return;
        }
        
        // Determine the player's role and corresponding color
        String playerName = player.getName();
        int nametagColor = getNametagColor(playerName);
        String rankPrefix = getRankPrefix(playerName);
        

        
        // Get the color for this player and render with separate prefix/username rendering
        int textColor = getNametagColor(playerName);
        
        // Render the forced nametag with separate rendering for prefix and username
        renderSeparateColoredNametag(player, rankPrefix, displayName, event.x, event.y + player.height + 0.5, event.z, distance, textColor);
    }
    
    private int getNametagColor(String playerName) {
        // Check player role and return matching ESP color
        if (MurderDetectionHandler.getMurdererList().contains(playerName)) {
            // Red for murderers (same as ESP: RGB 255, 0, 0)
            return 0xFF0000;
        }
        
        if (MurderDetectionHandler.isPlayerGoldenDetective(playerName)) {
            // Gold for golden detectives (same as ESP: RGB 255, 204, 0)
            return 0xFFCC00;
        }
        
        if (MurderDetectionHandler.getDetectiveList().contains(playerName)) {
            // Light blue for detectives (same as ESP: RGB 0, 179, 255)
            return 0x00B3FF;
        }
        
        // Check if current player is murderer to show victims in lime
        if (TitleHandler.isPlayerMurderer()) {
            // Lime for victims when player is murderer (same as ESP: RGB 0, 255, 0)
            return 0x00FF00;
        }
        
        // Default white for unknown/neutral players
        return 0xFFFFFF;
    }
    
    private String getRankPrefix(String playerName) {
        // Check player role and return appropriate rank prefix
        if (MurderDetectionHandler.getMurdererList().contains(playerName)) {
            return "[MUR] ";
        }
        
        if (MurderDetectionHandler.isPlayerGoldenDetective(playerName)) {
            return "[DET] ";
        }
        
        if (MurderDetectionHandler.getDetectiveList().contains(playerName)) {
            return "[BOW] ";
        }
        
        // No prefix for other players
        return "";
    }
    
    private void renderSeparateColoredNametag(EntityPlayer player, String prefix, String username, double x, double y, double z, double distance, int color) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            RenderManager renderManager = mc.getRenderManager();
            FontRenderer fontRenderer = renderManager.getFontRenderer();
            
            // Calculate scale based on distance (closer = larger)
            float scale = (float) (0.02666667F * Math.max(1.0, Math.min(distance / 20.0, 3.0)));
            
            // Set up rendering state
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            
            // Face the camera
            GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-scale, -scale, scale);
            
            // Disable lighting and depth test for clean text rendering
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            
            // Calculate total text dimensions for background
            String fullText = prefix + username;
            int totalWidth = fontRenderer.getStringWidth(fullText);
            int textHeight = 8;
            
            // Draw background
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            
            // Semi-transparent black background
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(-totalWidth / 2 - 1, -1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(-totalWidth / 2 - 1, textHeight, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(totalWidth / 2 + 1, textHeight, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(totalWidth / 2 + 1, -1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            
            // Re-enable texture for text rendering
            GlStateManager.enableTexture2D();
            
            // Calculate starting position (centered)
            int startX = -totalWidth / 2;
            
            // Draw prefix with color (if exists)
            if (!prefix.isEmpty()) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Reset color state
                fontRenderer.drawStringWithShadow(prefix, startX, 0, color);
                startX += fontRenderer.getStringWidth(prefix);
            }
            
            // Draw username with THE SAME color
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Reset color state again
            fontRenderer.drawStringWithShadow(username, startX, 0, color);
            
            // Restore rendering state
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            
        } catch (Exception e) {
            System.err.println("Error rendering separate colored nametag: " + e.getMessage());
        }
    }
    
    private void renderColoredNametag(EntityPlayer player, String text, double x, double y, double z, double distance, int color) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            RenderManager renderManager = mc.getRenderManager();
            FontRenderer fontRenderer = renderManager.getFontRenderer();
            
            // Calculate scale based on distance (closer = larger)
            float scale = (float) (0.02666667F * Math.max(1.0, Math.min(distance / 20.0, 3.0)));
            
            // Set up rendering state
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            
            // Face the camera
            GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-scale, -scale, scale);
            
            // Disable lighting and depth test for clean text rendering
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            
            // Calculate text dimensions
            int textWidth = fontRenderer.getStringWidth(text);
            int textHeight = 8;
            
            // Draw background
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            
            // Semi-transparent black background
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(-textWidth / 2 - 1, -1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(-textWidth / 2 - 1, textHeight, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(textWidth / 2 + 1, textHeight, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(textWidth / 2 + 1, -1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            
            // Re-enable texture for text rendering
            GlStateManager.enableTexture2D();
            
            // Reset to clean state - same as tab list
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Use the EXACT same method as the working tab list
            fontRenderer.drawStringWithShadow(text, -textWidth / 2, 0, color);
            
            // Restore rendering state
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            
        } catch (Exception e) {
            System.err.println("Error rendering colored nametag: " + e.getMessage());
        }
    }
    
    private void renderForcedNametagSeparate(EntityPlayer player, String prefix, String username, double x, double y, double z, double distance, int textColor) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            RenderManager renderManager = mc.getRenderManager();
            FontRenderer fontRenderer = renderManager.getFontRenderer();
            
            // Calculate scale based on distance (closer = larger)
            float scale = (float) (0.02666667F * Math.max(1.0, Math.min(distance / 20.0, 3.0)));
            
            // Set up rendering state
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            
            // Face the camera
            GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-scale, -scale, scale);
            
            // Disable lighting and depth test for clean text rendering
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            
            // Calculate total text dimensions for background
            String fullText = prefix + username;
            int totalWidth = fontRenderer.getStringWidth(fullText);
            int textHeight = 8;
            
            // Draw background
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            
            // Semi-transparent black background
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(-totalWidth / 2 - 1, -1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(-totalWidth / 2 - 1, textHeight, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(totalWidth / 2 + 1, textHeight, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(totalWidth / 2 + 1, -1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            
            // Re-enable texture for text rendering
            GlStateManager.enableTexture2D();
            
            // Calculate starting position (centered)
            int startX = -totalWidth / 2;
            
            // Draw prefix in color (if exists)
            if (!prefix.isEmpty()) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                fontRenderer.drawString(prefix, startX, 0, textColor | 0xFF000000);
                startX += fontRenderer.getStringWidth(prefix);
            }
            
            // Draw username in the same color
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            fontRenderer.drawString(username, startX, 0, textColor | 0xFF000000);
            
            // Restore rendering state
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            
        } catch (Exception e) {
            System.err.println("Error rendering separate nametag: " + e.getMessage());
        }
    }
} 