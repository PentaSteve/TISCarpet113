package carpet.commands.lifetime;

import carpet.commands.lifetime.removal.RemovalReason;
import carpet.commands.lifetime.spawning.SpawningReason;
import carpet.commands.lifetime.utils.LifeTimeTrackerUtil;
import carpet.commands.lifetime.utils.SpecificDetailMode;
import carpet.commands.lifetime.utils.TrackedData;
import carpet.utils.Messenger;
import carpet.utils.TextUtil;
import carpet.utils.TranslatableBase;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.WorldServer;

import java.util.*;

public class LifeTimeWorldTracker extends TranslatableBase
{
	private final WorldServer world;
	private final Map<EntityType<?>, TrackedData> dataMap = Maps.newHashMap();
	// a counter which accumulates when spawning stage occurs
	// it's used to determine life time
	private long spawnStageCounter;

	public LifeTimeWorldTracker(WorldServer world)
	{
		super(LifeTimeTracker.getInstance().getTranslator());
		this.world = world;
	}

	public Map<EntityType<?>, TrackedData> getDataMap()
	{
		return this.dataMap;
	}

	public void initTracker()
	{
		this.dataMap.clear();
	}

	private Optional<TrackedData> getTrackedData(Entity entity)
	{
		if (LifeTimeTracker.willTrackEntity(entity))
		{
			return Optional.of(this.dataMap.computeIfAbsent(entity.getType(), (e -> new TrackedData())));
		}
		return Optional.empty();
	}

	public void onEntitySpawn(Entity entity, SpawningReason reason)
	{
		this.getTrackedData(entity).ifPresent(data -> data.updateSpawning(entity, reason));
	}

	public void onEntityRemove(Entity entity, RemovalReason reason)
	{
		this.getTrackedData(entity).ifPresent(data -> data.updateRemoval(entity, reason));
	}

	public void increaseSpawnStageCounter()
	{
		this.spawnStageCounter++;
	}

	public long getSpawnStageCounter()
	{
		return this.spawnStageCounter;
	}

	private List<ITextComponent> addIfEmpty(List<ITextComponent> list, ITextComponent text)
	{
		if (list.isEmpty())
		{
			list.add(text);
		}
		return list;
	}

	protected int print(CommandSource source, long ticks, EntityType<?> specificType, SpecificDetailMode detailMode)
	{
		// existence check
		TrackedData specificData = this.dataMap.get(specificType);
		if (this.dataMap.isEmpty() || (specificType != null && specificData == null))
		{
			return 0;
		}

		// dimension name header
		// Overworld (minecraft:overworld)
		List<ITextComponent> result = Lists.newArrayList();
		result.add(Messenger.c(
				"w  \n",
				TextUtil.attachFormatting(TextUtil.getDimensionNameText(this.world.getDimension().getType()), TextFormatting.BOLD, TextFormatting.GOLD),
				String.format("g  (%s)", this.world.getDimension().getType().toString())
		));

		if (specificType == null)
		{
			this.printAll(ticks, result);
		}
		else
		{
			this.printSpecific(ticks, specificType, specificData, detailMode, result);
		}
		Messenger.send(source, result);
		return 1;
	}

	private void printAll(long ticks, List<ITextComponent> result)
	{
		// sorted by spawn count
		// will being sorting by avg life time better?
		this.dataMap.entrySet().stream().
				sorted(Collections.reverseOrder(Comparator.comparingLong(a -> a.getValue().spawnCount))).
				forEach((entry) -> {
					EntityType<?> entityType = entry.getKey();
					TrackedData data = entry.getValue();
					List<ITextComponent> spawningReasons = data.getSpawningReasonsTexts(ticks, true);
					List<ITextComponent> removalReasons = data.getRemovalReasonsTexts(ticks, true);
					String currentCommandBase = String.format("/%s %s", LifeTimeTracker.getInstance().getCommandPrefix(), LifeTimeTrackerUtil.getEntityTypeDescriptor(entityType));
					// [Creeper] S: 76, (1513.3/h), L: 4gt / 815gt / 210.2gt
					result.add(Messenger.c(
							"g - [",
							TextUtil.getFancyText(
									null,
									entityType.getName(),
									Messenger.s(this.tr("detail_hint", "Click to show detail")),
									new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, currentCommandBase)
							),
							"g ] ",
							TextUtil.getFancyText(
									null,
									Messenger.c("e S", "g /", "r R"),
									Messenger.c(
											"e " + this.tr("Spawn Count"),
											"g  / ",
											"r " + this.tr("Removal Count")
									),
									null
							),
							"g : ",
							TextUtil.getFancyText(
									null,
									Messenger.c("e " + data.spawnCount),
									Messenger.s(
											Messenger.c(
													data.getSpawningCountText(ticks),
													"w " + (spawningReasons.isEmpty() ? "" : "\n"),
													Messenger.c(spawningReasons.toArray(new Object[0]))
											).getString()  // to reduce network load
									),
									new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%s %s", currentCommandBase, SpecificDetailMode.SPAWNING))
							),
							"g /",
							TextUtil.getFancyText(
									null,
									Messenger.c("r " + data.getRemovalCount()),
									Messenger.s(
											Messenger.c(
													data.getRemovalCountText(ticks),
													"w " + (removalReasons.isEmpty() ? "" : "\n"),
													Messenger.c(removalReasons.toArray(new Object[0]))
											).getString()  // to reduce network load
									),
									new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%s %s", currentCommandBase, SpecificDetailMode.REMOVAL))
							),
							"g , ",
							TextUtil.getFancyText(
									null,
									Messenger.c(
											"q L", "g : ",
											data.lifeTimeStatistic.getCompressedResult("g /")
									),
									Messenger.s(
											Messenger.c(
													String.format("q %s\n", this.tr("Life Time Overview")),
													data.lifeTimeStatistic.getResult("", true)
											).getString()  // to reduce network load
									),
									new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%s %s", currentCommandBase, SpecificDetailMode.LIFE_TIME))
							)
					));
				});
	}

	private void printSpecific(long ticks, EntityType<?> specificType, TrackedData specificData, SpecificDetailMode detailMode, List<ITextComponent> result)
	{
		boolean showLifeTime = detailMode == null || detailMode == SpecificDetailMode.LIFE_TIME;
		boolean showSpawning = detailMode == null || detailMode == SpecificDetailMode.SPAWNING;
		boolean showRemoval = detailMode == null || detailMode == SpecificDetailMode.REMOVAL;
		if (showSpawning)
		{
			result.add(specificData.getSpawningCountText(ticks));
		}
		if (showRemoval)
		{
			result.add(specificData.getRemovalCountText(ticks));
		}
		if (showLifeTime)
		{
			result.add(TextUtil.getFancyText(
					"q",
					Messenger.s(this.tr("Life Time Overview")),
					Messenger.s(this.tr("life_time_explain", "The amount of spawning stage passing between entity spawning and entity removal")),
					null
			));
			result.add(specificData.lifeTimeStatistic.getResult("", false));
		}
		if (showSpawning)
		{
			result.add(Messenger.s(this.tr("Reasons for spawning"), "e"));
			result.addAll(this.addIfEmpty(specificData.getSpawningReasonsTexts(ticks, false), Messenger.s("  N/A", "g")));
		}
		if (showRemoval)
		{
			result.add(Messenger.s(this.tr("Reasons for removal"), "r"));
			result.addAll(this.addIfEmpty(specificData.getRemovalReasonsTexts(ticks, false), Messenger.s("  N/A", "g")));
		}
	}
}
