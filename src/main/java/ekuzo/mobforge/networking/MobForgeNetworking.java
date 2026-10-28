package ekuzo.mobforge.networking;

import ekuzo.mobforge.MobForge;
import ekuzo.mobforge.template.MobTemplate;
import ekuzo.mobforge.template.MobTemplateYaml;
import ekuzo.mobforge.script.MobScript;
import ekuzo.mobforge.script.MobScriptYaml;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class MobForgeNetworking {
	private static final MobTemplateYaml YAML = new MobTemplateYaml();
	private static final MobScriptYaml SCRIPT_YAML = new MobScriptYaml();
	private MobForgeNetworking() {}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(EditorPayloads.OpenEditor.TYPE, EditorPayloads.OpenEditor.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditorPayloads.SaveTemplate.TYPE, EditorPayloads.SaveTemplate.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditorPayloads.DeleteTemplate.TYPE, EditorPayloads.DeleteTemplate.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EditorPayloads.OpenScriptEditor.TYPE, EditorPayloads.OpenScriptEditor.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditorPayloads.SaveScript.TYPE, EditorPayloads.SaveScript.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditorPayloads.DeleteScript.TYPE, EditorPayloads.DeleteScript.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.SaveTemplate.TYPE, (payload, context) -> context.server().execute(() -> save(payload, context.player())));
		ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.DeleteTemplate.TYPE, (payload, context) -> context.server().execute(() -> delete(payload, context.player())));
		ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.SaveScript.TYPE, (payload, context) -> context.server().execute(() -> saveScript(payload, context.player())));
		ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.DeleteScript.TYPE, (payload, context) -> context.server().execute(() -> deleteScript(payload, context.player())));
	}

	public static boolean open(ServerPlayer player, MobTemplate template) {
		if (!ServerPlayNetworking.canSend(player, EditorPayloads.OpenEditor.TYPE)) return false;
		ServerPlayNetworking.send(player, new EditorPayloads.OpenEditor(YAML.encode(template)));
		return true;
	}
	public static boolean openScript(ServerPlayer player, MobScript script) {
		if (!ServerPlayNetworking.canSend(player, EditorPayloads.OpenScriptEditor.TYPE)) return false;
		String templates = String.join("\n", MobForge.TEMPLATES.all().stream().map(MobTemplate::id).toList());
		ServerPlayNetworking.send(player, new EditorPayloads.OpenScriptEditor(SCRIPT_YAML.encode(script), templates)); return true;
	}

	private static boolean authorized(ServerPlayer player) { return player.permissions().hasPermission(Permissions.COMMANDS_ADMIN); }
	private static void save(EditorPayloads.SaveTemplate payload, ServerPlayer player) {
		if (!authorized(player)) { player.sendSystemMessage(Component.literal("You do not have permission to edit MobForge templates.")); return; }
		try {
			MobTemplate template = YAML.decode(payload.yaml());
			MobForge.TEMPLATES.save(template);
			player.sendSystemMessage(Component.literal("Saved MobForge template " + template.id() + "."));
		} catch (Exception exception) { player.sendSystemMessage(Component.literal("MobForge save failed: " + exception.getMessage())); }
	}
	private static void delete(EditorPayloads.DeleteTemplate payload, ServerPlayer player) {
		if (!authorized(player)) { player.sendSystemMessage(Component.literal("You do not have permission to edit MobForge templates.")); return; }
		try { player.sendSystemMessage(Component.literal(MobForge.TEMPLATES.delete(payload.id()) ? "Deleted MobForge template " + payload.id() + "." : "Template was not found.")); }
		catch (Exception exception) { player.sendSystemMessage(Component.literal("MobForge delete failed: " + exception.getMessage())); }
	}
	private static void saveScript(EditorPayloads.SaveScript payload, ServerPlayer player) {
		if (!authorized(player)) { player.sendSystemMessage(Component.literal("You do not have permission to edit MobForge scripts.")); return; }
		try { MobScript script = SCRIPT_YAML.decode(payload.yaml()); MobForge.SCRIPTS.save(script); player.sendSystemMessage(Component.literal("Saved MobForge script " + script.id() + ".")); }
		catch (Exception exception) { player.sendSystemMessage(Component.literal("MobForge script save failed: " + exception.getMessage())); }
	}
	private static void deleteScript(EditorPayloads.DeleteScript payload, ServerPlayer player) {
		if (!authorized(player)) { player.sendSystemMessage(Component.literal("You do not have permission to edit MobForge scripts.")); return; }
		try {
			MobForge.SCRIPT_EXECUTOR.cancel(payload.id());
			player.sendSystemMessage(Component.literal(MobForge.SCRIPTS.delete(payload.id()) ? "Deleted MobForge script " + payload.id() + "." : "Script was not found."));
		} catch (Exception exception) { player.sendSystemMessage(Component.literal("MobForge script delete failed: " + exception.getMessage())); }
	}
}
