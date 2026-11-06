package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.sounds.JOrbisAudioStream;
import net.phoenix.core.integration.vocal_vibrancy.VocalVibrancyClient;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Mixin(JOrbisAudioStream.class)
public abstract class MixinOggAudioStream {

    @ModifyVariable(method = "readChunk", at = @At("HEAD"), argsOnly = true)
    private FloatConsumer phoenix$captureLivePCM(FloatConsumer original) {
        if (!VocalVibrancyClient.isAnyTracking()) return original;

        ByteBuffer pcm = ByteBuffer.allocate(16384).order(ByteOrder.LITTLE_ENDIAN);
        return sample -> {
            original.accept(sample);
            if (pcm.remaining() < 2) {
                pcm.flip();
                try {
                    VocalVibrancyClient.onPCMBuffer(pcm, 44100);
                } catch (Exception e) {
                    System.err.println("VocalVibrancy: PCM analysis error: " + e.getMessage());
                }
                pcm.clear();
            }
            pcm.putShort((short) (Math.max(-1f, Math.min(1f, sample)) * 32767f));
        };
    }
}
