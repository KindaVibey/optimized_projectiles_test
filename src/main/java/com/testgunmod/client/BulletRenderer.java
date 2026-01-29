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
        float pitch = -((float)(Mth.atan2(velocity.y, velocity.horizontalDistance()) * (180.0 / Math.PI)));
        float yaw = (float)(Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));

        // Apply rotations to align bullet with flight path
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        // Scale the model to appropriate size
        poseStack.scale(0.08f, 0.08f, 0.08f);

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