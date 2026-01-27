package com.testgunmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.testgunmod.entity.BulletEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Bullet renderer with 3D model support
 *
 * Supports three rendering modes:
 * 1. Simple geometry (default)
 * 2. Item model (using any Minecraft item)
 * 3. Custom 3D model (JSON model file)
 */
public class BulletRenderer extends EntityRenderer<BulletEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("testgunmod", "textures/entity/bullet.png");

    // Choose rendering mode
    private static final RenderMode MODE = RenderMode.CUSTOM_MODEL;
    // For ITEM_MODEL mode: change this to your desired item
    private static final ItemStack BULLET_ITEM = new ItemStack(Items.GOLD_NUGGET);

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(BulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // Get velocity for smooth rotation
        Vec3 velocity = entity.getDeltaMovement();

        // Calculate rotation angles
        float pitch = -((float)(Mth.atan2(velocity.y, velocity.horizontalDistance()) * (180.0 / Math.PI)));
        float yaw = (float)(Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));

        // Apply rotations to align bullet with velocity
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        // Render based on mode
        switch (MODE) {
            case SIMPLE_GEOMETRY:
                poseStack.scale(0.1f, 0.1f, 0.3f); // Elongated bullet
                renderSimpleGeometry(poseStack, buffer, packedLight);
                break;

            case ITEM_MODEL:
                poseStack.scale(0.3f, 0.3f, 0.3f);
                renderItemModel(poseStack, buffer, packedLight, entity);
                break;

            case CUSTOM_MODEL:
                poseStack.scale(0.15f, 0.15f, 0.15f);
                renderCustomModel(poseStack, buffer, packedLight);
                break;
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * Render simple geometry - default mode, best performance
     */
    private void renderSimpleGeometry(PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Gold/brass colored bullet
        int r = 255, g = 200, b = 100, a = 255;

        // Front face (pointed tip)
        vertex(consumer, pose, normal, 0, 0, 0, 0.5f, 0.5f, 0, 0, 1, light, r, g, b, a);

        // Create elongated hexagonal prism for bullet shape
        float radius = 0.5f;
        int sides = 6;

        for (int i = 0; i < sides; i++) {
            float angle1 = (float) (2 * Math.PI * i / sides);
            float angle2 = (float) (2 * Math.PI * (i + 1) / sides);

            float x1 = radius * Mth.cos(angle1);
            float y1 = radius * Mth.sin(angle1);
            float x2 = radius * Mth.cos(angle2);
            float y2 = radius * Mth.sin(angle2);

            // Side face
            vertex(consumer, pose, normal, x1, y1, 0.1f, 0, 0, x1, y1, 0, light, r, g, b, a);
            vertex(consumer, pose, normal, x2, y2, 0.1f, 1, 0, x2, y2, 0, light, r, g, b, a);
            vertex(consumer, pose, normal, x2, y2, 1, 1, 1, x2, y2, 0, light, r, g, b, a);
            vertex(consumer, pose, normal, x1, y1, 1, 0, 1, x1, y1, 0, light, r, g, b, a);

            // Front tip
            vertex(consumer, pose, normal, x1, y1, 0.1f, 0, 0, 0, 0, 1, light, r, g, b, a);
            vertex(consumer, pose, normal, 0, 0, 0, 0.5f, 0.5f, 0, 0, 1, light, r, g, b, a);
            vertex(consumer, pose, normal, x2, y2, 0.1f, 1, 0, 0, 0, 1, light, r, g, b, a);

            // Back face
            vertex(consumer, pose, normal, x1, y1, 1, 0, 1, 0, 0, -1, light, r, g, b, a);
            vertex(consumer, pose, normal, x2, y2, 1, 1, 1, 0, 0, -1, light, r, g, b, a);
            vertex(consumer, pose, normal, 0, 0, 1, 0.5f, 0.5f, 0, 0, -1, light, r, g, b, a);
        }
    }

    /**
     * Render using an item model - good for quick prototyping
     */
    private void renderItemModel(PoseStack poseStack, MultiBufferSource buffer, int light, BulletEntity entity) {
        // Render any item as the bullet
        Minecraft.getInstance().getItemRenderer().renderStatic(
                BULLET_ITEM,
                ItemDisplayContext.GROUND,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
    }

    /**
     * Render custom 3D model from JSON file
     *
     * To use this mode:
     * 1. Create a JSON model at: assets/testgunmod/models/entity/bullet.json
     * 2. Model format is same as block/item models
     * 3. Set MODE to CUSTOM_MODEL
     */
    private void renderCustomModel(PoseStack poseStack, MultiBufferSource buffer, int light) {
        // Load the custom model
        ResourceLocation modelLocation = new ResourceLocation("testgunmod", "entity/bullet");
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);

        if (model != null) {
            renderBakedModel(poseStack, buffer, light, model);
        } else {
            // Fallback to simple geometry if model not found
            renderSimpleGeometry(poseStack, buffer, light);
        }
    }

    /**
     * Render a baked model (loaded from JSON)
     */
    private void renderBakedModel(PoseStack poseStack, MultiBufferSource buffer, int light, BakedModel model) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        RandomSource random = RandomSource.create();

        // Render all faces
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(null, direction, random);
            renderQuads(poseStack, consumer, quads, light);
        }

        // Render faces without direction
        List<BakedQuad> quads = model.getQuads(null, null, random);
        renderQuads(poseStack, consumer, quads, light);
    }

    /**
     * Render a list of quads by manually unpacking vertex data
     */
    private void renderQuads(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads, int light) {
        for (BakedQuad quad : quads) {
            // Get raw vertex data array
            int[] vertexData = quad.getVertices();
            Vec3i normalVec = quad.getDirection().getNormal();

            // Each vertex has 8 ints of data (position, color, UV, etc.)
            // There are 4 vertices per quad
            for (int i = 0; i < 4; i++) {
                int offset = i * 8; // 8 ints per vertex

                // Unpack position (stored as float bits)
                float x = Float.intBitsToFloat(vertexData[offset]);
                float y = Float.intBitsToFloat(vertexData[offset + 1]);
                float z = Float.intBitsToFloat(vertexData[offset + 2]);

                // Unpack color (packed as ABGR format)
                int colorABGR = vertexData[offset + 3];
                int r = colorABGR & 0xFF;
                int g = (colorABGR >> 8) & 0xFF;
                int b = (colorABGR >> 16) & 0xFF;
                int a = (colorABGR >> 24) & 0xFF;

                // Unpack UV coordinates (stored as float bits)
                float u = Float.intBitsToFloat(vertexData[offset + 4]);
                float v = Float.intBitsToFloat(vertexData[offset + 5]);

                // Add vertex to consumer
                consumer.vertex(poseStack.last().pose(), x, y, z)
                        .color(r, g, b, a)
                        .uv(u, v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light)
                        .normal(poseStack.last().normal(), normalVec.getX(), normalVec.getY(), normalVec.getZ())
                        .endVertex();
            }
        }
    }

    /**
     * Helper method for vertex definition
     */
    private void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                        float x, float y, float z, float u, float v,
                        float normalX, float normalY, float normalZ, int light,
                        int r, int g, int b, int a) {
        consumer.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return TEXTURE;
    }

    /**
     * Rendering mode enum
     */
    private enum RenderMode {
        SIMPLE_GEOMETRY,  // Built-in geometry (fastest)
        ITEM_MODEL,       // Use any item model (easy to customize)
        CUSTOM_MODEL      // Load from JSON model file (most flexible)
    }
}