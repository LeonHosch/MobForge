package ekuzo.mobforge.ai;

import ekuzo.mobforge.entity.MobForgeEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MobAiRegistry {
	private static final Map<String, MobAiController> CONTROLLERS = new LinkedHashMap<>();
	private MobAiRegistry() {}
	public static void initialize() {
		register(new VanillaAiController()); register(new TargetPositionAiController());
		register(new TargetPlayerAiController()); register(new NearestPlayerAiController());
	}
	public static void register(MobAiController controller) { CONTROLLERS.put(controller.id(), controller); }
	public static void tick(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level) || !mob.entityTags().contains(MobForgeEntityData.MARKER)) return;
		MobForgeEntityData.aiMode(mob).ifPresent(mode -> {
			VanillaGoalStateManager.applyMode(mob, mode);
			MobAiController controller = CONTROLLERS.get(mode);
			if (controller != null) controller.tick(level, mob);
		});
	}
}
