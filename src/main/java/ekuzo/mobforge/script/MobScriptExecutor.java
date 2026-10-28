package ekuzo.mobforge.script;

import ekuzo.mobforge.MobForge;
import ekuzo.mobforge.entity.MobForgeEntityData;
import ekuzo.mobforge.entity.MobForgeSpawner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MobScriptExecutor {
	private final Map<UUID, ActiveRun> active = new LinkedHashMap<>();
	private long clock;

	public UUID run(MobScript script, ServerLevel level) {
		UUID id = UUID.randomUUID();
		active.put(id, new ActiveRun(id, script, level));
		MobForge.LOGGER.info("Started MobForge script {} (run {}).", script.id(), id);
		return id;
	}

	public int cancel(String scriptId) {
		int before = active.size();
		active.entrySet().removeIf(entry -> entry.getValue().script.id().equals(scriptId));
		return before - active.size();
	}

	public int activeRuns(String scriptId) {
		return (int) active.values().stream().filter(run -> run.script.id().equals(scriptId)).count();
	}

	public void tick(MinecraftServer server) {
		clock++;
		Iterator<ActiveRun> iterator = active.values().iterator();
		while (iterator.hasNext()) {
			ActiveRun run = iterator.next();
			try {
				if (advance(run)) {
					iterator.remove();
					MobForge.LOGGER.info("Completed MobForge script {} (run {}).", run.script.id(), run.id);
				}
			} catch (RuntimeException exception) {
				iterator.remove();
				MobForge.LOGGER.warn("Canceled MobForge script {} (run {}): {}", run.script.id(), run.id, exception.getMessage());
			}
		}
	}

	private boolean advance(ActiveRun run) {
		if (clock < run.wakeTick) return false;
		while (run.step < run.script.steps().size()) {
			MobScript.Step step = run.script.steps().get(run.step++);
			if (step instanceof MobScript.SleepStep sleep) {
				run.wakeTick = clock + sleep.ticks();
				if (sleep.ticks() > 0) return false;
			} else if (step instanceof MobScript.SpawnStep spawn) {
				spawn(run, spawn);
			}
		}
		return true;
	}

	private void spawn(ActiveRun run, MobScript.SpawnStep step) {
		var template = MobForge.TEMPLATES.get(step.template()).orElseThrow(() -> new IllegalStateException("template disappeared after reload: " + step.template()));
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < step.count(); i++) {
			double x = step.location().x() + spread(random, step.offset().x());
			double y = step.location().y() + spread(random, step.offset().y());
			double z = step.location().z() + spread(random, step.offset().z());
			Entity entity = MobForgeSpawner.spawn(run.level, template, x, y, z);
			MobForgeEntityData.setScript(entity, run.script.id(), run.id.toString());
			if (step.aiOverride() != null) MobForgeEntityData.setAi(entity, step.aiOverride());
		}
	}

	private static double spread(ThreadLocalRandom random, double offset) { return offset == 0 ? 0 : random.nextDouble(-offset, offset); }

	private static final class ActiveRun {
		private final UUID id;
		private final MobScript script;
		private final ServerLevel level;
		private int step;
		private long wakeTick;
		private ActiveRun(UUID id, MobScript script, ServerLevel level) { this.id = id; this.script = script; this.level = level; }
	}
}
