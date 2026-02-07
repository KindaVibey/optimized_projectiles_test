package com.testgunmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.testgunmod.entity.BulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Bullet renderer using custom 3D model
 */
public class BulletRenderer extends EntityRenderer<BulletEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("testgunmod", "textures/entity/bullet.png");

    private final BulletModel model;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.model = new BulletModel(context.bakeLayer(BulletModel.LAYER_LOCATION));
    }

    @Override
    public void render(BulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // Get velocity for rotation
        Vec3 velocity = entity.getDeltaMovement();

        // Calculate rotation angles to align bullet with velocity
        float pitch;
        float yaw;

        // Handle vertical shots specially to avoid division by zero
        double horizontalDist = velocity.horizontalDistance();
        if (horizontalDist < 0.001) {
            // Near vertical shot - set pitch directly
            if (velocity.y > 0) {
                pitch = -90.0f;  // Straight up (negative because Minecraft's pitch is inverted)
            } else {
                pitch = 90.0f; // Straight down
            }
            yaw = 0.0f; // No yaw for vertical shots
        } else {
            // Normal case - calculate using atan2
            // Note: Minecraft's X rotation (pitch) is NEGATIVE for looking up
            pitch = -(float)(Mth.atan2(velocity.y, horizontalDist) * (180.0 / Math.PI));
            yaw = (float)(Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));
        }

        // Apply rotations to align bullet with flight path
        // Add a small local-space Y offset (1px) before rotating so horizontal shots align
        // This offset rotates with the bullet, preserving vertical alignment.
        poseStack.translate(0.0D, 1.0D / 16.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Render the model
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return TEXTURE;
    }
}