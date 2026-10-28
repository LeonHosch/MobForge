package ekuzo.mobforge.networking;

import ekuzo.mobforge.MobForge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class EditorPayloads {
	private EditorPayloads() {}
	private static final StreamCodec<RegistryFriendlyByteBuf, String> YAML = ByteBufCodecs.stringUtf8(1_000_000).cast();

	public record OpenEditor(String yaml) implements CustomPacketPayload {
		public static final Type<OpenEditor> TYPE = new Type<>(MobForge.id("open_editor"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenEditor> CODEC = StreamCodec.composite(YAML, OpenEditor::yaml, OpenEditor::new);
		@Override public Type<OpenEditor> type() { return TYPE; }
	}
	public record SaveTemplate(String yaml) implements CustomPacketPayload {
		public static final Type<SaveTemplate> TYPE = new Type<>(MobForge.id("save_template"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SaveTemplate> CODEC = StreamCodec.composite(YAML, SaveTemplate::yaml, SaveTemplate::new);
		@Override public Type<SaveTemplate> type() { return TYPE; }
	}
	public record DeleteTemplate(String id) implements CustomPacketPayload {
		public static final Type<DeleteTemplate> TYPE = new Type<>(MobForge.id("delete_template"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DeleteTemplate> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64).cast(), DeleteTemplate::id, DeleteTemplate::new);
		@Override public Type<DeleteTemplate> type() { return TYPE; }
	}
	public record OpenScriptEditor(String yaml, String templates) implements CustomPacketPayload {
		public static final Type<OpenScriptEditor> TYPE = new Type<>(MobForge.id("open_script_editor"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenScriptEditor> CODEC = StreamCodec.composite(YAML, OpenScriptEditor::yaml,
				ByteBufCodecs.stringUtf8(100_000).cast(), OpenScriptEditor::templates, OpenScriptEditor::new);
		@Override public Type<OpenScriptEditor> type() { return TYPE; }
	}
	public record SaveScript(String yaml) implements CustomPacketPayload {
		public static final Type<SaveScript> TYPE = new Type<>(MobForge.id("save_script"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SaveScript> CODEC = StreamCodec.composite(YAML, SaveScript::yaml, SaveScript::new);
		@Override public Type<SaveScript> type() { return TYPE; }
	}
	public record DeleteScript(String id) implements CustomPacketPayload {
		public static final Type<DeleteScript> TYPE = new Type<>(MobForge.id("delete_script"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DeleteScript> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64).cast(), DeleteScript::id, DeleteScript::new);
		@Override public Type<DeleteScript> type() { return TYPE; }
	}
}
