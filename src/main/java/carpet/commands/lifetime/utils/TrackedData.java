package carpet.commands.lifetime.utils;

import carpet.commands.lifetime.LifeTimeTracker;
import carpet.commands.lifetime.removal.RemovalReason;
import carpet.commands.lifetime.spawning.SpawningReason;
import carpet.utils.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A lifetime tracking tracked data per mob type
 */
public class TrackedData extends TranslatableBase
{
	// spawning
	public final Map<SpawningReason, Integer> spawningReasons = Maps.newHashMap();
	public long spawnCount;
	// removal
	public final Map<RemovalReason, LifeTimeStatistic> removalReasons = Maps.newHashMap();
	public final LifeTimeStatistic lifeTimeStatistic = new LifeTimeStatistic();

	public TrackedData()
	{
		super(LifeTimeTracker.getInstance().getTranslator());
	}

	public void updateSpawning(Entity entity, SpawningReason reason)
	{
		this.spawnCount++;
		this.spawningReasons.put(reason, this.spawningReasons.getOrDefault(reason, 0) + 1);
	}

	public void updateRemoval(Entity entity, RemovalReason reason)
	{
		this.lifeTimeStatistic.update(entity);
		this.removalReasons.computeIfAbsent(reason, r -> new LifeTimeStatistic()).update(entity);
	}

	public long getRemovalCount()
	{
		return this.removalReasons.values().stream().mapToLong(stat -> stat.count).sum();
	}

	/**
	 * Spawn Count: xxxxx <divider> Removal Count yyyyy
	 */
	public ITextComponent getSpawningCountText(long ticks)
	{
		return Messenger.c(
				"q " + this.tr("Spawn Count"),
				"g : ",
				CounterUtil.ratePerHourText(this.spawnCount, ticks, "wgg")
		);
	}

	/**
	 * Removal Count: xxxxx <divider> Removal Count yyyyy
	 */
	public ITextComponent getRemovalCountText(long ticks)
	{
		return Messenger.c(
				"q " + this.tr("Removal Count"),
				"g : ",
				CounterUtil.ratePerHourText(this.getRemovalCount(), ticks, "wgg")
		);
	}

	/**
	 * - AAA: 50, (100/h) 25%
	 * @param reason spawning reason or removal reason
	 */
	private ITextComponent getReasonWithRate(AbstractReason reason, long ticks, long count, long total)
	{
		double percentage = 100.0D * count / total;
		return Messenger.c(
				"g - ",
				reason.toText(),
				"g : ",
				CounterUtil.ratePerHourText(count, ticks, "wgg"),
				"w  ",
				TextUtil.attachHoverText(Messenger.s(String.format("%.1f%%", percentage)), Messenger.s(String.format("%.6f%%", percentage)))

		);
	}

	/**
	 * Reasons for spawning
	 * - AAA: 50, (100/h) 25%
	 * - BBB: 150, (300/h) 75%
	 *
	 * @param hoverMode automatically insert a new line text between lines or not for hover text display
	 * @return might be a empty list
	 */
	public List<ITextComponent> getSpawningReasonsTexts(long ticks, boolean hoverMode)
	{
		List<ITextComponent> result = Lists.newArrayList();
		List<Map.Entry<SpawningReason, Integer>> entryList = Lists.newArrayList(this.spawningReasons.entrySet());
		entryList.sort(Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getValue)));

		// Title for hover mode
		if (!entryList.isEmpty() && hoverMode)
		{
			result.add(Messenger.s(this.tr("Reasons for spawning"), "e"));
		}

		entryList.forEach(entry -> {
			// added to upper result which will be sent by Messenger.send
			// so each element will be in a separate line
			if (hoverMode)
			{
				result.add(Messenger.s("\n"));
			}

			result.add(this.getReasonWithRate(entry.getKey(), ticks, entry.getValue(), this.spawnCount));
		});
		return result;
	}

	/**
	 * Reasons for removal
	 * - AAA: 50, (100/h) 25%
	 *   - Minimum life time: xx1 gt
	 *   - Maximum life time: yy1 gt
	 *   - Average life time: zz1 gt
	 * - BBB: 150, (300/h) 75%
	 *   - Minimum life time: xx2 gt
	 *   - Maximum life time: yy2 gt
	 *   - Average life time: zz2 gt
	 *
	 * @param hoverMode automatically insert a new line text between lines or not for hover text display
	 * @return might be a empty list
	 */
	public List<ITextComponent> getRemovalReasonsTexts(long ticks, boolean hoverMode)
	{
		List<ITextComponent> result = Lists.newArrayList();
		List<Map.Entry<RemovalReason, LifeTimeStatistic>> entryList = Lists.newArrayList(this.removalReasons.entrySet());
		entryList.sort(Collections.reverseOrder(Comparator.comparingLong(a -> a.getValue().count)));

		// Title for hover mode
		if (!entryList.isEmpty() && hoverMode)
		{
			result.add(Messenger.s(this.tr("Reasons for removal"), "r"));
		}

		entryList.forEach(entry -> {
			RemovalReason reason = entry.getKey();
			LifeTimeStatistic statistic = entry.getValue();

			// added to upper result which will be sent by Messenger.send
			// so each element will be in a separate line
			if (hoverMode)
			{
				result.add(Messenger.s("\n"));
			}

			result.add(Messenger.c(
					this.getReasonWithRate(reason, ticks, statistic.count, this.lifeTimeStatistic.count),
					"w \n",
					statistic.getResult("  ", hoverMode)
			));
		});
		return result;
	}
}
