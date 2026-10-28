package ekuzo.mobforge.client.screen;

import ekuzo.mobforge.networking.EditorPayloads;
import ekuzo.mobforge.script.MobScript;
import ekuzo.mobforge.script.MobScriptYaml;
import ekuzo.mobforge.template.MobTemplate;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ScriptEditorScreen extends Screen {
	private static final MobScriptYaml YAML = new MobScriptYaml();
	private static final List<String> AI_MODES = List.of("template_default", "vanilla", "target_position", "target_player", "nearest_player");
	private static final int SIDEBAR_WIDTH = 137;
	private final MobScript original;
	private final List<String> templates;
	private final List<StepDraft> steps = new ArrayList<>();
	private String scriptId;
	private int selected = -1;
	private String status = "";
	private boolean dirty;
	private boolean allowClose;

	public ScriptEditorScreen(Minecraft client, String yaml, String templateIds) {
		super(client, client.font, Component.literal("MobForge Script Editor"));
		original = YAML.decode(yaml); scriptId = original.id();
		templates = templateIds.isBlank() ? List.of() : Arrays.stream(templateIds.split("\\n")).filter(value -> !value.isBlank()).toList();
		for (MobScript.Step step : original.steps()) steps.add(StepDraft.from(step));
	}

	@Override protected void init() { rebuild(); }

	private void rebuild() {
		clearWidgets();
		int footerTop = height - 36;
		StepList stepList = new StepList(SIDEBAR_WIDTH - 12, Math.max(40, height - 111), 40); stepList.setX(6); stepList.populate(); addRenderableWidget(stepList);
		int small = (SIDEBAR_WIDTH - 16) / 2;
		addRenderableWidget(Button.builder(Component.literal("+ Spawn"), b -> addSpawn()).bounds(6, footerTop - 25, small, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+ Sleep"), b -> addSleep()).bounds(10 + small, footerTop - 25, small, 20).build());

		int contentX = SIDEBAR_WIDTH + 8; int contentWidth = Math.max(120, width - contentX - 8);
		if (selected >= 0 && selected < steps.size()) {
			Button up = Button.builder(Component.literal("↑"), b -> move(-1)).bounds(contentX, 38, 24, 20).build(); up.active = selected > 0;
			Button down = Button.builder(Component.literal("↓"), b -> move(1)).bounds(contentX + 28, 38, 24, 20).build(); down.active = selected + 1 < steps.size();
			addRenderableWidget(up); addRenderableWidget(down);
			addRenderableWidget(Button.builder(Component.literal("Remove step"), b -> removeStep()).bounds(contentX + 58, 38, 90, 20).build());
		}
		FormList form = new FormList(contentWidth, Math.max(40, height - 103), 64); form.setX(contentX); form.populate(formBlocks(contentWidth < 210)); addRenderableWidget(form);

		int footerWidth = Math.min(width - 16, 310); int footerX = (width - footerWidth) / 2; int buttonWidth = (footerWidth - 12) / 4; int y = height - 29;
		addRenderableWidget(Button.builder(Component.literal("Duplicate"), b -> duplicate()).bounds(footerX, y, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Delete"), b -> confirmDelete()).bounds(footerX + buttonWidth + 4, y, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(footerX + (buttonWidth + 4) * 2, y, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Save"), b -> save()).bounds(footerX + (buttonWidth + 4) * 3, y, buttonWidth, 20).build());
	}

	private List<FormBlock> formBlocks(boolean compact) {
		List<FormBlock> blocks = new ArrayList<>();
		if (selected < 0 || selected >= steps.size()) {
			section(blocks, "Script");
			row(blocks, compact, text("Script ID", "Lowercase file ID using letters, numbers, _ and -.", () -> scriptId, value -> scriptId = value));
			section(blocks, "Timeline");
			blocks.add(new NoteBlock(steps.isEmpty() ? "Add Spawn and Sleep steps with the buttons on the left." : steps.size() + " steps execute from top to bottom."));
			return blocks;
		}
		StepDraft step = steps.get(selected);
		if (step.type == StepType.SLEEP) {
			section(blocks, "Sleep break");
			row(blocks, compact, text("Duration", "Examples: 40t, 5s, 500ms, or 2m.", () -> step.sleep, value -> step.sleep = value));
			return blocks;
		}
		section(blocks, "Spawn wave");
		row(blocks, compact, new FormItem("Mob template", "Select a loaded MobForge template.", FieldType.TEMPLATE, List.of(), () -> step.template, value -> step.template = value, null),
				text("Count", "Number of entities spawned by this step.", () -> step.count, value -> step.count = value));
		section(blocks, "Spawn location");
		row(blocks, compact, text("X", "Base X coordinate.", () -> step.x, value -> step.x = value), text("Y", "Base Y coordinate.", () -> step.y, value -> step.y = value), text("Z", "Base Z coordinate.", () -> step.z, value -> step.z = value));
		blocks.add(new ActionBlock("Use current player position", this::usePlayerPosition));
		section(blocks, "Random offset");
		row(blocks, compact, text("Offset X", "Random ±X spread.", () -> step.ox, value -> step.ox = value), text("Offset Y", "Random ±Y spread.", () -> step.oy, value -> step.oy = value), text("Offset Z", "Random ±Z spread.", () -> step.oz, value -> step.oz = value));
		section(blocks, "AI override");
		row(blocks, compact, choice("AI mode", "Use the template AI or override it for this wave.", AI_MODES, () -> step.aiMode, value -> { step.aiMode = value; rebuild(); }),
				bool("Combat", "Allow combat to interrupt the objective.", () -> step.combat, value -> step.combat = value));
		switch (step.aiMode) {
			case "target_position" -> {
				row(blocks, compact, text("Target X", "Objective X coordinate.", () -> step.tx, value -> step.tx = value), text("Target Y", "Objective Y coordinate.", () -> step.ty, value -> step.ty = value), text("Target Z", "Objective Z coordinate.", () -> step.tz, value -> step.tz = value));
				row(blocks, compact, text("Arrival radius", "Stop within this distance.", () -> step.arrival, value -> step.arrival = value));
			}
			case "target_player" -> row(blocks, compact, text("Player", "Player name pursued by this wave.", () -> step.player, value -> step.player = value));
			case "nearest_player" -> row(blocks, compact, text("Search range", "Maximum nearest-player radius.", () -> step.range, value -> step.range = value));
			default -> { }
		}
		return blocks;
	}

	private AbstractWidget createWidget(FormItem item) {
		return switch (item.type) {
			case TEXT -> {
				EditBox box = new EditBox(font, 0, 0, 100, 20, Component.literal(item.label)); box.setMaxLength(256); box.setValue(item.get.get());
				box.setResponder(value -> { item.set.accept(value); changed(); }); yield box;
			}
			case TEMPLATE -> Button.builder(Component.literal(item.get.get().isBlank() ? "Choose template…" : item.get.get()), b -> minecraft.setScreenAndShow(
					new RegistryPickerScreen(minecraft, this, "Select mob template", templates, item.get.get(), value -> { item.set.accept(value); changed(); rebuild(); }))).bounds(0, 0, 100, 20).build();
			case BOOLEAN -> CycleButton.onOffBuilder(item.boolGet().get()).displayOnlyValue().create(0, 0, 100, 20, Component.literal(item.label), (button, value) -> { item.boolSet().accept(value); changed(); });
			case CHOICE -> CycleButton.builder(value -> Component.literal(value.replace('_', ' ')), item.get.get()).withValues(item.options).displayOnlyValue()
					.create(0, 0, 100, 20, Component.literal(item.label), (button, value) -> { item.set.accept(value); changed(); });
		};
	}

	private void changed() { dirty = true; status = ""; }
	private void addSpawn() { steps.add(StepDraft.spawn(minecraft)); selected = steps.size() - 1; changed(); rebuild(); }
	private void addSleep() { steps.add(StepDraft.sleep()); selected = steps.size() - 1; changed(); rebuild(); }
	private void removeStep() { if (selected < 0 || selected >= steps.size()) return; steps.remove(selected); selected = Math.min(selected, steps.size() - 1); changed(); rebuild(); }
	private void move(int direction) { int target = selected + direction; if (selected < 0 || target < 0 || target >= steps.size()) return; StepDraft step = steps.remove(selected); steps.add(target, step); selected = target; changed(); rebuild(); }
	private void usePlayerPosition() { if (minecraft.player == null || selected < 0) return; StepDraft step = steps.get(selected); step.x = decimal(minecraft.player.getX()); step.y = decimal(minecraft.player.getY()); step.z = decimal(minecraft.player.getZ()); changed(); rebuild(); }

	private MobScript buildScript() {
		List<MobScript.Step> built = new ArrayList<>(); for (StepDraft draft : steps) built.add(draft.build()); return new MobScript(scriptId.trim(), built);
	}
	private void save() {
		try { MobScript script = buildScript(); if (script.steps().isEmpty()) throw new IllegalArgumentException("add at least one spawn or sleep step"); ClientPlayNetworking.send(new EditorPayloads.SaveScript(YAML.encode(script))); dirty = false; status = "Sent to server for validation and saving."; }
		catch (Exception exception) { status = "Invalid: " + exception.getMessage(); }
	}
	private void duplicate() { scriptId = scriptId.endsWith("_copy") ? scriptId + "_2" : scriptId + "_copy"; selected = -1; changed(); status = "Duplicate ready—save to create it."; rebuild(); }
	private void confirmDelete() { minecraft.setScreenAndShow(new ConfirmScreen(result -> { if (result) { ClientPlayNetworking.send(new EditorPayloads.DeleteScript(original.id())); allowClose = true; minecraft.setScreenAndShow(null); } else minecraft.setScreenAndShow(this); }, Component.literal("Delete script?"), Component.literal("This permanently deletes " + original.id() + " and cancels its active runs."))); }
	@Override public void onClose() { if (!dirty || allowClose) { minecraft.setScreenAndShow(null); return; } minecraft.setScreenAndShow(new ConfirmScreen(result -> { if (result) { allowClose = true; minecraft.setScreenAndShow(null); } else minecraft.setScreenAndShow(this); }, Component.literal("Discard unsaved changes?"), Component.literal("Your script changes have not been saved."))); }

	@Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, 34, 0xCC101820); graphics.fill(0, 34, SIDEBAR_WIDTH, height - 36, 0x99101820); graphics.fill(SIDEBAR_WIDTH, 34, SIDEBAR_WIDTH + 1, height - 36, 0xFF52606D); graphics.fill(0, height - 36, width, height, 0xCC101820);
		graphics.text(font, title, 10, 12, 0xFFFFFFFF);
		String heading = selected < 0 || selected >= steps.size() ? "General" : "Step " + (selected + 1) + " — " + (steps.get(selected).type == StepType.SPAWN ? "Spawn" : "Sleep");
		graphics.text(font, heading, SIDEBAR_WIDTH + 10, 12, 0xFFFFFFFF);
		if (!status.isBlank()) graphics.text(font, truncate(status, Math.max(20, width - 235)), 225, 12, status.startsWith("Invalid") ? 0xFFFF7777 : 0xFF77FF9A);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private final class StepList extends ContainerObjectSelectionList<StepEntry> {
		StepList(int width, int height, int y) { super(ScriptEditorScreen.this.minecraft, width, height, y, 24); centerListVertically = false; }
		void populate() { addEntry(new StepEntry(-1, "General")); for (int i = 0; i < steps.size(); i++) addEntry(new StepEntry(i, (i + 1) + ". " + steps.get(i).summary())); }
		@Override public int getRowWidth() { return Math.max(80, getWidth() - 8); }
		@Override protected boolean entriesCanBeSelected() { return false; }
	}
	private final class StepEntry extends ContainerObjectSelectionList.Entry<StepEntry> {
		private final int index; private final Button button;
		StepEntry(int index, String label) { this.index = index; button = Button.builder(Component.literal(label), b -> { selected = index; status = ""; rebuild(); }).bounds(0, 0, 100, 20).build(); button.active = selected != index; }
		@Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) { button.setX(getContentX()); button.setY(getContentY() + 2); button.setWidth(getContentWidth()); button.extractRenderState(graphics, mouseX, mouseY, partialTick); }
		@Override public List<? extends NarratableEntry> narratables() { return List.of(button); } @Override public List<? extends GuiEventListener> children() { return List.of(button); } @Override public void visitWidgets(Consumer<AbstractWidget> consumer) { consumer.accept(button); }
	}

	private final class FormList extends ContainerObjectSelectionList<FormEntry> {
		FormList(int width, int height, int y) { super(ScriptEditorScreen.this.minecraft, width, height, y, 52); centerListVertically = false; }
		void populate(List<FormBlock> blocks) { for (FormBlock block : blocks) { if (block instanceof SectionBlock section) addEntry(new SectionEntry(section), 22); else if (block instanceof FieldBlock fields) addEntry(new FieldEntry(fields.fields), 52); else if (block instanceof ActionBlock action) addEntry(new ActionEntry(action), 25); else if (block instanceof NoteBlock note) addEntry(new NoteEntry(note), 24); } }
		@Override protected boolean entriesCanBeSelected() { return false; } @Override public int getRowWidth() { return Math.min(Math.max(120, getWidth() - 20), 640); }
	}
	private abstract static class FormEntry extends ContainerObjectSelectionList.Entry<FormEntry> { }
	private final class SectionEntry extends FormEntry {
		private final SectionBlock section; SectionEntry(SectionBlock section) { this.section = section; }
		@Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) { graphics.fill(getContentX(), getContentY() + 17, getContentRight(), getContentY() + 18, 0x6652606D); graphics.text(font, section.title, getContentX() + 3, getContentY() + 3, 0xFFFFFFFF); }
		@Override public List<? extends NarratableEntry> narratables() { return List.of(); } @Override public List<? extends GuiEventListener> children() { return List.of(); }
	}
	private final class FieldEntry extends FormEntry {
		private final List<FormItem> fields; private final List<AbstractWidget> widgets;
		FieldEntry(List<FormItem> fields) { this.fields = fields; widgets = fields.stream().map(ScriptEditorScreen.this::createWidget).toList(); }
		@Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) { int gap = 12; int groupWidth = Math.min(getContentWidth(), 620); int cap = fields.size() == 1 ? 150 : 300; int cellWidth = Math.min(cap, (groupWidth - gap * (fields.size() - 1)) / fields.size()); for (int i = 0; i < fields.size(); i++) { int x = getContentX() + i * (cellWidth + gap), y = getContentY(); FormItem field = fields.get(i); AbstractWidget widget = widgets.get(i); graphics.text(font, field.label, x, y + 1, 0xFFFFFFFF); graphics.textWithWordWrap(font, Component.literal(field.description), x, y + 11, cellWidth, 0xFF929AA3); widget.setX(x); widget.setY(y + 30); widget.setWidth(cellWidth); widget.extractRenderState(graphics, mouseX, mouseY, partialTick); } }
		@Override public List<? extends NarratableEntry> narratables() { return widgets; } @Override public List<? extends GuiEventListener> children() { return widgets; } @Override public void visitWidgets(Consumer<AbstractWidget> consumer) { widgets.forEach(consumer); }
	}
	private final class ActionEntry extends FormEntry {
		private final Button button; ActionEntry(ActionBlock action) { button = Button.builder(Component.literal(action.label), b -> action.action.run()).bounds(0, 0, 150, 20).build(); }
		@Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) { button.setX(getContentX()); button.setY(getContentY() + 2); button.extractRenderState(graphics, mouseX, mouseY, partialTick); } @Override public List<? extends NarratableEntry> narratables() { return List.of(button); } @Override public List<? extends GuiEventListener> children() { return List.of(button); } @Override public void visitWidgets(Consumer<AbstractWidget> consumer) { consumer.accept(button); }
	}
	private final class NoteEntry extends FormEntry {
		private final NoteBlock note; NoteEntry(NoteBlock note) { this.note = note; } @Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) { graphics.text(font, note.text, getContentX() + 3, getContentY() + 5, 0xFF929AA3); } @Override public List<? extends NarratableEntry> narratables() { return List.of(); } @Override public List<? extends GuiEventListener> children() { return List.of(); }
	}

	private static void section(List<FormBlock> blocks, String title) { blocks.add(new SectionBlock(title)); }
	private static void row(List<FormBlock> blocks, boolean compact, FormItem... items) { if (compact && items.length > 1) for (FormItem item : items) blocks.add(new FieldBlock(List.of(item))); else blocks.add(new FieldBlock(List.of(items))); }
	private static FormItem text(String label, String description, Supplier<String> get, Consumer<String> set) { return new FormItem(label, description, FieldType.TEXT, List.of(), get, set, null); }
	private static FormItem choice(String label, String description, List<String> options, Supplier<String> get, Consumer<String> set) { return new FormItem(label, description, FieldType.CHOICE, options, get, set, null); }
	private static FormItem bool(String label, String description, Supplier<Boolean> get, Consumer<Boolean> set) { return new FormItem(label, description, FieldType.BOOLEAN, List.of(), null, null, new BoolBinding(get, set)); }
	private String truncate(String value, int width) { if (font.width(value) <= width) return value; return font.plainSubstrByWidth(value, Math.max(1, width - font.width("…"))) + "…"; }
	private static String decimal(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }
	private enum FieldType { TEXT, TEMPLATE, BOOLEAN, CHOICE }
	private enum StepType { SPAWN, SLEEP }
	private interface FormBlock { }
	private record SectionBlock(String title) implements FormBlock { }
	private record FieldBlock(List<FormItem> fields) implements FormBlock { }
	private record ActionBlock(String label, Runnable action) implements FormBlock { }
	private record NoteBlock(String text) implements FormBlock { }
	private record BoolBinding(Supplier<Boolean> get, Consumer<Boolean> set) { }
	private record FormItem(String label, String description, FieldType type, List<String> options, Supplier<String> get, Consumer<String> set, BoolBinding bool) { private Supplier<Boolean> boolGet() { return bool.get; } private Consumer<Boolean> boolSet() { return bool.set; } }

	private static final class StepDraft {
		private StepType type; private String template = "", count = "1", x = "0", y = "64", z = "0", ox = "0", oy = "0", oz = "0", sleep = "5s";
		private String aiMode = "template_default", tx = "0", ty = "64", tz = "0", arrival = "2", player = "", range = "64"; private boolean combat = true;
		static StepDraft from(MobScript.Step step) { StepDraft draft = new StepDraft(); if (step instanceof MobScript.SleepStep sleep) { draft.type = StepType.SLEEP; draft.sleep = MobScriptYaml.formatDuration(sleep.ticks()); } else if (step instanceof MobScript.SpawnStep spawn) { draft.type = StepType.SPAWN; draft.template = spawn.template(); draft.count = String.valueOf(spawn.count()); draft.x = String.valueOf(spawn.location().x()); draft.y = String.valueOf(spawn.location().y()); draft.z = String.valueOf(spawn.location().z()); draft.ox = String.valueOf(spawn.offset().x()); draft.oy = String.valueOf(spawn.offset().y()); draft.oz = String.valueOf(spawn.offset().z()); if (spawn.aiOverride() != null) { MobTemplate.Ai ai = spawn.aiOverride(); draft.aiMode = ai.mode(); draft.combat = ai.combat(); if (ai.target() != null) { draft.tx = String.valueOf(ai.target().x()); draft.ty = String.valueOf(ai.target().y()); draft.tz = String.valueOf(ai.target().z()); } draft.arrival = String.valueOf(ai.arrivalRadius()); draft.player = ai.player() == null ? "" : ai.player(); draft.range = String.valueOf(ai.range()); } } return draft; }
		static StepDraft spawn(Minecraft minecraft) { StepDraft draft = new StepDraft(); draft.type = StepType.SPAWN; if (minecraft.player != null) { draft.x = decimal(minecraft.player.getX()); draft.y = decimal(minecraft.player.getY()); draft.z = decimal(minecraft.player.getZ()); } return draft; }
		static StepDraft sleep() { StepDraft draft = new StepDraft(); draft.type = StepType.SLEEP; return draft; }
		String summary() { return type == StepType.SLEEP ? "Sleep " + sleep : "Spawn " + (template.isBlank() ? "(choose template)" : template); }
		MobScript.Step build() { if (type == StepType.SLEEP) return new MobScript.SleepStep(MobScriptYaml.parseDuration(sleep)); MobTemplate.Ai ai = null; if (!"template_default".equals(aiMode)) { MobTemplate.Position target = "target_position".equals(aiMode) ? new MobTemplate.Position(Double.parseDouble(tx), Double.parseDouble(ty), Double.parseDouble(tz)) : null; ai = new MobTemplate.Ai(aiMode, combat, target, Double.parseDouble(arrival), "target_player".equals(aiMode) ? player : null, Double.parseDouble(range), Map.of()); } return new MobScript.SpawnStep(template, new MobScript.Position(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z)), new MobScript.Offset(Double.parseDouble(ox), Double.parseDouble(oy), Double.parseDouble(oz)), Integer.parseInt(count), ai); }
	}
}
