package com.example;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
	private static final int FULL_BRIGHT_DURATION = 220;
	private static final int FULL_BRIGHT_REAPPLY_THRESHOLD = 40;

	private static KeyMapping toggleHudKey;
	private static KeyMapping toggleSprintKey;
	private static KeyMapping toggleFullBrightKey;

	private static boolean hudEnabled = true;
	private static boolean sprintEnabled = true;
	private static boolean fullBrightEnabled = false;
	private static boolean fullBrightAppliedByMod = false;

	@Override
	public void onInitializeClient() {
		toggleHudKey = registerKey("key.frosty-client.toggle_hud", GLFW.GLFW_KEY_RIGHT_SHIFT);
		toggleSprintKey = registerKey("key.frosty-client.toggle_sprint", GLFW.GLFW_KEY_R);
		toggleFullBrightKey = registerKey("key.frosty-client.toggle_fullbright", GLFW.GLFW_KEY_F);

		ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);

		HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) -> {
			if (!hudEnabled) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			if (client.font == null) {
				return;
			}

			drawContext.drawString(client.font, "Frosty Client", 6, 6, 0x66CCFF);
			drawContext.drawString(client.font, keyLabel(toggleSprintKey) + " Sprint: " + asState(sprintEnabled), 6, 18, 0xFFFFFF);
			drawContext.drawString(client.font, keyLabel(toggleFullBrightKey) + " FullBright: " + asState(fullBrightEnabled), 6, 30, 0xFFFFFF);
			drawContext.drawString(client.font, keyLabel(toggleHudKey) + " HUD: " + asState(hudEnabled), 6, 42, 0xFFFFFF);
		});
	}

	private KeyMapping registerKey(String translationKey, int keyCode) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(
			translationKey,
			InputConstants.Type.KEYSYM,
			keyCode,
			KeyMapping.Category.MISC
		));
	}

	private void handleClientTick(Minecraft client) {
		while (toggleHudKey.consumeClick()) {
			hudEnabled = !hudEnabled;
		}
		while (toggleSprintKey.consumeClick()) {
			sprintEnabled = !sprintEnabled;
		}
		while (toggleFullBrightKey.consumeClick()) {
			fullBrightEnabled = !fullBrightEnabled;
		}

		if (client.player == null || client.options == null) {
			return;
		}

		if (sprintEnabled && client.options.keyUp.isDown() && !client.player.horizontalCollision) {
			client.player.setSprinting(true);
		}

		MobEffectInstance existingNightVision = client.player.getEffect(MobEffects.NIGHT_VISION);
		if (fullBrightEnabled) {
			if (existingNightVision == null || existingNightVision.getDuration() <= FULL_BRIGHT_REAPPLY_THRESHOLD) {
				client.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, FULL_BRIGHT_DURATION, 0, false, false, false));
				fullBrightAppliedByMod = true;
			}
		} else if (fullBrightAppliedByMod && existingNightVision != null && existingNightVision.getDuration() <= FULL_BRIGHT_DURATION) {
			client.player.removeEffect(MobEffects.NIGHT_VISION);
			fullBrightAppliedByMod = false;
		}
	}

	private String asState(boolean state) {
		return state ? "ON" : "OFF";
	}

	private String keyLabel(KeyMapping keyMapping) {
		Component translated = keyMapping.getTranslatedKeyMessage();
		return "[" + translated.getString() + "]";
	}
}
