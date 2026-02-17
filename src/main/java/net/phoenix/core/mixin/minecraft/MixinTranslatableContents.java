package net.phoenix.core.mixin.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(TranslatableContents.class)
public abstract class MixinTranslatableContents {

    @Shadow
    @Final
    @Mutable
    private String key;
    @Shadow
    @Final
    private Object[] args;

    @Unique
    private static final Pattern HEX_PATTERN = Pattern.compile("\\[#([A-Fa-f0-9]{6})\\]");

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("RETURN"))
    private void phoenixCore$afterInit(String key, String fallback, Object[] args, CallbackInfo ci) {
        if (this.args == null) return;

        for (Object arg : this.args) {
            String text = (arg instanceof Component c) ? c.getString() : String.valueOf(arg);

            if (text.contains("[#")) {
                Matcher m = HEX_PATTERN.matcher(text);
                if (m.find()) {
                    String hexCode = m.group(1);
                    // Prepend the Minecraft color formatting to the translation key
                    this.key = phoenixCore$convertToMcHex(hexCode) + this.key;
                    break;
                }
            }
        }
    }

    @Unique
    private static String phoenixCore$convertToMcHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }
}
