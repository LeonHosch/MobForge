package ekuzo.mobforge.script;

import ekuzo.mobforge.template.MobTemplate;

import java.util.List;

public record MobScript(String id, List<Step> steps) {
	public MobScript { steps = steps == null ? List.of() : List.copyOf(steps); }

	public sealed interface Step permits SpawnStep, SleepStep { }
	public record Position(double x, double y, double z) { }
	public record Offset(double x, double y, double z) {
		public static final Offset NONE = new Offset(0, 0, 0);
	}
	public record SpawnStep(String template, Position location, Offset offset, int count, MobTemplate.Ai aiOverride) implements Step {
		public SpawnStep { offset = offset == null ? Offset.NONE : offset; }
	}
	public record SleepStep(long ticks) implements Step { }
}
