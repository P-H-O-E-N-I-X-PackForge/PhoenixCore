package net.phoenix.core.client.renderer.entity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class TetoSlimeModel<T extends Entity> extends HierarchicalModel<T> {

    private final ModelPart root;

    public TetoSlimeModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {}

    private static void cube(PartDefinition parent, String name, int u, int v, float x0, float y0, float z0, float x1,
                             float y1, float z1, boolean mirror) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(x0, -y1, z0, x1 - x0, y1 - y0, z1 - z0),
                PartPose.offset(0.0f, 24.0f, 0.0f));
    }

    private static void turned(PartDefinition parent, String name, int u, int v, float x0, float y0, float z0, float x1,
                               float y1, float z1, float pivotX, float pivotY, float pivotZ, boolean mirror) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(x0 - pivotX, pivotY - y1, z0 - pivotZ,
                        x1 - x0, y1 - y0, z1 - z0),
                PartPose.offsetAndRotation(pivotX, 24.0f - pivotY, pivotZ, 0.0f, (float) -Math.PI / 2.0f, 0.0f));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition inner = root.addOrReplaceChild("inner", CubeListBuilder.create(), PartPose.ZERO);
        cube(inner, "core", 44, 24, -5.5f, 1, -5.5f, 5.5f, 12, 5.5f, false);
        cube(inner, "core_top", 44, 46, -4.5f, 12, -4.5f, 4.5f, 13, 4.5f, false);

        PartDefinition things = root.addOrReplaceChild("things", CubeListBuilder.create(), PartPose.ZERO);

        turned(things, "tail_r1", 52, 0, -2.5f, 11, 5, 3.5f, 14, 11, 0.5f, 10.5f, 0.0f, false);
        turned(things, "tail_r2", 52, 9, -2, 8, 5.5f, 3, 11, 10.5f, 0.5f, 10.5f, 0.0f, false);
        turned(things, "tail_r3", 52, 17, -1.5f, 5, 6, 2.5f, 8, 10, 0.5f, 10.5f, 0.0f, false);
        turned(things, "tail_l3", 52, 17, -1, 5, -11.5f, 3, 8, -7.5f, 1.0f, 9.5f, 0.0f, true);
        turned(things, "tail_l2", 52, 9, -1.5f, 8, -12, 3.5f, 11, -7, 1.0f, 9.5f, 0.0f, true);
        turned(things, "tail_l1", 52, 0, -2, 11, -12.5f, 4, 14, -6.5f, 1.0f, 9.5f, 0.0f, false);

        cube(things, "top", 33, 24, -4, 13, 0, 4, 21, 0, false);

        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createOuterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition outer = mesh.getRoot().addOrReplaceChild("outer", CubeListBuilder.create(), PartPose.ZERO);
        cube(outer, "shell_wide", 0, 0, -6.5f, 1, -6.5f, 6.5f, 12, 6.5f, false);
        cube(outer, "shell_tall", 0, 24, -5.5f, 0, -5.5f, 5.5f, 14, 5.5f, false);
        return LayerDefinition.create(mesh, 128, 128);
    }
}
