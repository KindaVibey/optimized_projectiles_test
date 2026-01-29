package com.testgunmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.testgunmod.entity.BulletEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Bullet Model - A realistic bullet with pointed tip, cylindrical body, and brass casing
 *
 * Model created programmatically (Blockbench-compatible format)
 * Total: 3 main parts (tip, body, casing) for optimal performance
 */
public class BulletModel extends EntityModel<BulletEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("testgunmod", "bullet"), "main");

    private final ModelPart root;
    private final ModelPart tip;
    private final ModelPart body;
    private final ModelPart casing;

    public BulletModel(ModelPart root) {
        this.root = root;
        this.tip = root.getChild("tip");
        this.body = root.getChild("body");
        this.casing = root.getChild("casing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Pointed TIP (front of bullet) - small cone shape
        // This is the projectile tip that pierces
        PartDefinition tip = partdefinition.addOrReplaceChild("tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // Main BODY (middle section) - cylindrical projectile body
        // Slightly thicker than tip
        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 5)
                        .addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // Brass CASING (back section) - the cartridge casing
        // Slightly wider than body with rim at back
        PartDefinition casing = partdefinition.addOrReplaceChild("casing",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, -2.0F, 8.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        // Rim at the back
                        .texOffs(16, 0)
                        .addBox(-2.5F, -2.5F, 11.5F, 5.0F, 5.0F, 0.5F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(BulletEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Bullets don't animate - they just fly straight
        // Rotation is handled by the renderer based on velocity
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        // Render all parts
        tip.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        casing.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}