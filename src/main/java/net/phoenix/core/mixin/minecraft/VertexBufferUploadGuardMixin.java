package net.phoenix.core.mixin.minecraft;

import net.phoenix.core.PhoenixCore;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferUploadGuardMixin {

    private static boolean phoenixcore$warned;

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void phoenixcore$skipFreedMesh(MeshData mesh, CallbackInfo ci) {
        try {
            mesh.vertexBuffer();
        } catch (IllegalStateException freed) {
            if (!phoenixcore$warned) {
                phoenixcore$warned = true;
                PhoenixCore.LOGGER.warn("[Guard] Skipped uploading a mesh whose buffer was already freed " +
                        "(a ModularUI structure preview being rebuilt). Not logging further occurrences.");
            }
            try {
                mesh.close();
            } catch (RuntimeException ignored) {

            }
            ci.cancel();
        }
    }
}
