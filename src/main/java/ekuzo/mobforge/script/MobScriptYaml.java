package ekuzo.mobforge.script;

import ekuzo.mobforge.template.MobTemplate;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MobScriptYaml {
	private static final Pattern DURATION = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(ms|t|ticks?|s|sec(?:onds?)?|m|min(?:utes?)?)?", Pattern.CASE_INSENSITIVE);
	private final Load loader = new Load(LoadSettings.builder().setLabel("MobForge script").setCodePointLimit(1_000_000).build());
	private final Dump dumper = new Dump(DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).setIndent(2).build());

	public MobScript decode(String yaml) {
		Map<String, Object> root = map(loader.loadFromString(yaml), "root");
		String id = string(root.get("id"), "id");
		Object rawSteps = root.get("steps");
		if (!(rawSteps instanceof List<?> steps)) throw new IllegalArgumentException("steps must be a list");
		List<MobScript.Step> decoded = new ArrayList<>();
		for (int i = 0; i < steps.size(); i++) decoded.add(step(steps.get(i), "steps[" + i + "]"));
		return new MobScript(id, decoded);
	}

	private MobScript.Step step(Object value, String field) {
		Map<String, Object> step = map(value, field);
		if (step.containsKey("spawn")) return spawn(map(step.get("spawn"), field + ".spawn"), field);
		if (step.containsKey("sleep")) return new MobScript.SleepStep(duration(step.get("sleep"), field + ".sleep"));
		String type = string(step.get("type"), field + ".type");
		return switch (type) {
			case "spawn" -> spawn(step, field);
			case "sleep" -> new MobScript.SleepStep(duration(step.get("duration"), field + ".duration"));
			default -> throw new IllegalArgumentException(field + ".type must be spawn or sleep");
		};
	}

	private MobScript.SpawnStep spawn(Map<String, Object> value, String field) {
		String template = string(value.get("template"), field + ".template");
		Map<String, Object> location = map(value.get("location"), field + ".location");
		MobScript.Position position = new MobScript.Position(number(location.get("x"), field + ".location.x"),
				number(location.get("y"), field + ".location.y"), number(location.get("z"), field + ".location.z"));
		MobScript.Offset offset = offset(value.get("offset"), field + ".offset");
		int count = integer(value.getOrDefault("count", 1), field + ".count");
		return new MobScript.SpawnStep(template, position, offset, count, ai(value.get("ai"), field + ".ai"));
	}

	private MobTemplate.Ai ai(Object value, String field) {
		if (value == null) return null;
		Map<String, Object> ai = map(value, field);
		String mode = string(ai.get("mode"), field + ".mode");
		MobTemplate.Position target = null;
		if (ai.get("target") != null) {
			Map<String, Object> position = map(ai.get("target"), field + ".target");
			target = new MobTemplate.Position(number(position.get("x"), field + ".target.x"), number(position.get("y"), field + ".target.y"), number(position.get("z"), field + ".target.z"));
		}
		Map<String, Object> settings = new LinkedHashMap<>(ai);
		List.of("mode", "combat", "target", "arrival_radius", "player", "range").forEach(settings::remove);
		return new MobTemplate.Ai(mode, bool(ai.get("combat"), true, field + ".combat"), target,
				number(ai.getOrDefault("arrival_radius", 2), field + ".arrival_radius"), ai.get("player") == null ? null : String.valueOf(ai.get("player")),
				number(ai.getOrDefault("range", 64), field + ".range"), settings);
	}

	public String encode(MobScript script) {
		Map<String, Object> root = new LinkedHashMap<>(); root.put("id", script.id());
		List<Object> steps = new ArrayList<>();
		for (MobScript.Step step : script.steps()) {
			if (step instanceof MobScript.SleepStep sleep) steps.add(Map.of("sleep", formatDuration(sleep.ticks())));
			else if (step instanceof MobScript.SpawnStep spawn) {
				Map<String, Object> body = new LinkedHashMap<>(); body.put("template", spawn.template());
				body.put("location", position(spawn.location().x(), spawn.location().y(), spawn.location().z()));
				if (!spawn.offset().equals(MobScript.Offset.NONE)) body.put("offset", position(spawn.offset().x(), spawn.offset().y(), spawn.offset().z()));
				body.put("count", spawn.count());
				if (spawn.aiOverride() != null) body.put("ai", ai(spawn.aiOverride()));
				steps.add(Map.of("spawn", body));
			}
		}
		root.put("steps", steps); return dumper.dumpToString(root);
	}

	private static Map<String, Object> position(double x, double y, double z) { Map<String, Object> map = new LinkedHashMap<>(); map.put("x", x); map.put("y", y); map.put("z", z); return map; }
	private static Map<String, Object> ai(MobTemplate.Ai ai) {
		Map<String, Object> map = new LinkedHashMap<>(); map.put("mode", ai.mode()); map.put("combat", ai.combat());
		if (ai.target() != null) map.put("target", position(ai.target().x(), ai.target().y(), ai.target().z()));
		if ("target_position".equals(ai.mode())) map.put("arrival_radius", ai.arrivalRadius());
		if (ai.player() != null) map.put("player", ai.player());
		if ("nearest_player".equals(ai.mode())) map.put("range", ai.range());
		map.putAll(ai.settings()); return map;
	}

	private MobScript.Offset offset(Object value, String field) {
		if (value == null) return MobScript.Offset.NONE;
		if (value instanceof Number || value instanceof String) {
			double radius = number(value, field); return new MobScript.Offset(radius, 0, radius);
		}
		Map<String, Object> map = map(value, field);
		return new MobScript.Offset(number(map.getOrDefault("x", 0), field + ".x"), number(map.getOrDefault("y", 0), field + ".y"), number(map.getOrDefault("z", 0), field + ".z"));
	}

	public static long parseDuration(Object value) { return duration(value, "duration"); }
	public static String formatDuration(long ticks) { return ticks % 20 == 0 ? (ticks / 20) + "s" : ticks + "t"; }
	private static long duration(Object value, String field) {
		if (value == null) throw new IllegalArgumentException(field + " is required");
		if (value instanceof Number number) return Math.max(0, Math.round(number.doubleValue() * 20.0));
		Matcher matcher = DURATION.matcher(String.valueOf(value).trim());
		if (!matcher.matches()) throw new IllegalArgumentException(field + " must look like 40t, 5s, 500ms, or 2m");
		double amount = Double.parseDouble(matcher.group(1)); String unit = matcher.group(2);
		if (unit == null || unit.equalsIgnoreCase("s") || unit.toLowerCase().startsWith("sec")) return Math.round(amount * 20.0);
		if (unit.equalsIgnoreCase("ms")) return Math.round(amount / 50.0);
		if (unit.toLowerCase().startsWith("m")) return Math.round(amount * 1200.0);
		return Math.round(amount);
	}

	private static Map<String, Object> map(Object value, String field) {
		if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException(field + " must be a map");
		Map<String, Object> result = new LinkedHashMap<>(); raw.forEach((key, item) -> result.put(String.valueOf(key), item)); return result;
	}
	private static String string(Object value, String field) { if (value == null || String.valueOf(value).isBlank()) throw new IllegalArgumentException(field + " is required"); return String.valueOf(value); }
	private static double number(Object value, String field) { if (value instanceof Number number) return number.doubleValue(); try { return Double.parseDouble(String.valueOf(value)); } catch (Exception exception) { throw new IllegalArgumentException(field + " must be a number"); } }
	private static int integer(Object value, String field) { double number = number(value, field); if (number != Math.rint(number)) throw new IllegalArgumentException(field + " must be an integer"); return (int) number; }
	private static boolean bool(Object value, boolean fallback, String field) { if (value == null) return fallback; if (value instanceof Boolean bool) return bool; if ("true".equalsIgnoreCase(String.valueOf(value))) return true; if ("false".equalsIgnoreCase(String.valueOf(value))) return false; throw new IllegalArgumentException(field + " must be true or false"); }
}
