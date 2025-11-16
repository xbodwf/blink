package com.xbodw.blink.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FillerRenderer {
    
    /**
     * 渲染填充器的所有视觉效果
     */
    public static void renderFillerEffects(PoseStack poseStack, MultiBufferSource bufferSource) {
        BlockPos targetPos = FillerClientHandler.getCurrentTargetPos();
        BlockPos pos1 = FillerClientHandler.getPos1();
        BlockPos pos2 = FillerClientHandler.getPos2();
        
        // 渲染准心位置的方块描边
        if (targetPos != null) {
            float[] color = FillerClientHandler.getCurrentStateColor();
            renderAnimatedBlockOutline(poseStack, bufferSource, targetPos, 
                                     color[0], color[1], color[2], 0.8f);
        }
        
        // 渲染已选择的位置1
        if (pos1 != null) {
            renderAnimatedBlockOutline(poseStack, bufferSource, pos1, 
                                     0.0f, 1.0f, 0.0f, 1.0f); // 绿色
        }
        
        // 渲染已选择的位置2
        if (pos2 != null) {
            renderAnimatedBlockOutline(poseStack, bufferSource, pos2, 
                                     0.0f, 0.0f, 1.0f, 1.0f); // 蓝色
        }
        
        // 渲染区域描边
        if (pos1 != null && pos2 != null) {
            float[] regionColor = FillerClientHandler.getRegionColor();
            renderAnimatedRegionOutline(poseStack, bufferSource, pos1, pos2,
                                      regionColor[0], regionColor[1], regionColor[2], 0.6f);
        } else if (pos1 != null && targetPos != null) {
            // 如果只有位置1，显示从位置1到准心的预览区域
            renderAnimatedRegionOutline(poseStack, bufferSource, pos1, targetPos,
                                      0.0f, 1.0f, 1.0f, 0.3f); // 半透明青色预览
        }
    }
    
    // 渲染单个方块描边
    public static void renderBlockOutline(PoseStack poseStack, MultiBufferSource bufferSource, 
                                        BlockPos pos, float red, float green, float blue, float alpha) {
        if (pos == null) return;
        
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // 创建方块的AABB，稍微扩大一点避免Z-fighting
        AABB aabb = new AABB(pos).inflate(0.005);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        
        // 绘制方块描边
        renderAABBOutline(poseStack, vertexConsumer, aabb, red, green, blue, alpha);
        
        poseStack.popPose();
    }
    
    // 渲染区域描边
    public static void renderRegionOutline(PoseStack poseStack, MultiBufferSource bufferSource,
                                         BlockPos pos1, BlockPos pos2, float red, float green, float blue, float alpha) {
        if (pos1 == null || pos2 == null) return;
        
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        // 创建区域的AABB
        AABB aabb = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(0.005);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        
        // 绘制区域描边
        renderAABBOutline(poseStack, vertexConsumer, aabb, red, green, blue, alpha);
        
        poseStack.popPose();
    }
    
    // 渲染带动画效果的方块描边
    public static void renderAnimatedBlockOutline(PoseStack poseStack, MultiBufferSource bufferSource,
                                                BlockPos pos, float red, float green, float blue, float baseAlpha) {
        if (pos == null) return;
        
        // 创建呼吸效果
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time * 0.005) * 0.3 + 0.7); // 0.4 到 1.0 之间
        float alpha = baseAlpha * pulse;
        
        renderBlockOutline(poseStack, bufferSource, pos, red, green, blue, alpha);
    }
    
    // 渲染带动画效果的区域描边
    public static void renderAnimatedRegionOutline(PoseStack poseStack, MultiBufferSource bufferSource,
                                                 BlockPos pos1, BlockPos pos2, float red, float green, float blue, float baseAlpha) {
        if (pos1 == null || pos2 == null) return;
        
        // 创建呼吸效果
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time * 0.003) * 0.2 + 0.8); // 0.6 到 1.0 之间
        float alpha = baseAlpha * pulse;
        
        renderRegionOutline(poseStack, bufferSource, pos1, pos2, red, green, blue, alpha);
    }
    
    // 绘制AABB的线框
    private static void renderAABBOutline(PoseStack poseStack, VertexConsumer vertexConsumer, 
                                        AABB aabb, float red, float green, float blue, float alpha) {
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;
        
        // 底面的四条边
        addLine(poseStack, vertexConsumer, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);
        
        // 顶面的四条边
        addLine(poseStack, vertexConsumer, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        
        // 四条垂直边
        addLine(poseStack, vertexConsumer, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(poseStack, vertexConsumer, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }
    
    // 添加一条线
    private static void addLine(PoseStack poseStack, VertexConsumer vertexConsumer,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float red, float green, float blue, float alpha) {
        // 计算法向量
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }
        
        vertexConsumer.vertex(poseStack.last().pose(), x1, y1, z1)
                .color(red, green, blue, alpha)
                .normal(poseStack.last().normal(), dx, dy, dz)
                .endVertex();
        vertexConsumer.vertex(poseStack.last().pose(), x2, y2, z2)
                .color(red, green, blue, alpha)
                .normal(poseStack.last().normal(), dx, dy, dz)
                .endVertex();
    }
}