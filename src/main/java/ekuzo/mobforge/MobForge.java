package ekuzo.mobforge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.resources.Identifier;

import ekuzo.mobforge.ai.MobAiRegistry;
import ekuzo.mobforge.command.MobForgeCommand;
import ekuzo.mobforge.networking.MobForgeNetworking;
import ekuzo.mobforge.template.MobTemplateRegistry;
import ekuzo.mobforge.template.MobTemplateValidator;
import ekuzo.mobforge.script.MobScriptExecutor;
import ekuzo.mobforge.script.MobScriptRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobForge implements ModInitializer {
	public static final String MOD_ID = "mobforge";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final MobTemplateRegistry TEMPLATES = new MobTemplateRegistry();
	public static final MobScriptRegistry SCRIPTS = new MobScriptRegistry();
	public static final MobScriptExecutor SCRIPT_EXECUTOR = new MobScriptExecutor();
	public static final java.util.Set<String> ATTRIBUTE_NAMES = MobTemplateValidator.ATTRIBUTES;

	@Override
	public void onInitialize() {
		MobAiRegistry.initialize();
		MobForgeCommand.initialize();
		MobForgeNetworking.initialize();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			TEMPLATES.reload();
			SCRIPTS.reload();
		});
		ServerTickEvents.END_SERVER_TICK.register(SCRIPT_EXECUTOR::tick);
		LOGGER.info("MobForge initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
