package ekuzo.mobforge.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ekuzo.mobforge.client.screen.TemplateEditorScreen;
import ekuzo.mobforge.client.screen.ScriptEditorScreen;
import ekuzo.mobforge.networking.EditorPayloads;

public class MobForgeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(EditorPayloads.OpenEditor.TYPE, (payload, context) ->
				context.client().setScreenAndShow(new TemplateEditorScreen(context.client(), payload.yaml())));
		ClientPlayNetworking.registerGlobalReceiver(EditorPayloads.OpenScriptEditor.TYPE, (payload, context) ->
				context.client().setScreenAndShow(new ScriptEditorScreen(context.client(), payload.yaml(), payload.templates())));
	}
}
