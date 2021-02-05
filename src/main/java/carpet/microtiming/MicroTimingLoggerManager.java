package carpet.microtiming;

import carpet.CarpetServer;
import carpet.logging.LoggerRegistry;
import carpet.microtiming.enums.BlockUpdateType;
import carpet.microtiming.enums.EventType;
import carpet.microtiming.enums.TickStage;
import carpet.microtiming.events.*;
import carpet.microtiming.tickstages.TickStageExtraBase;
import carpet.microtiming.utils.MicroTimingUtil;
import carpet.settings.CarpetSettings;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEventData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class MicroTimingLoggerManager
{
    private static MicroTimingLoggerManager instance;

    private final Map<WorldServer, MicroTimingLogger> loggers = new Reference2ObjectArrayMap<>();
    private final MicroTimingLogger dummyLoggerForTranslate = new MicroTimingLogger(null);
    private long lastFlushTime;

    public MicroTimingLoggerManager(MinecraftServer minecraftServer)
    {
        this.lastFlushTime = -1;
        for (WorldServer world : minecraftServer.getWorlds())
        {
            this.loggers.put(world, world.getMicroTickLogger());
        }
    }

    public static MicroTimingLoggerManager getInstance()
    {
        return instance;
    }

    public Map<WorldServer, MicroTimingLogger> getLoggers()
    {
        return loggers;
    }

    public static boolean isLoggerActivated()
    {
        return CarpetSettings.microTiming && LoggerRegistry.__microTiming && instance != null;
    }

    public static void attachServer(MinecraftServer minecraftServer)
    {
        instance = new MicroTimingLoggerManager(minecraftServer);
        CarpetServer.LOGGER.debug("Attached MicroTick loggers to " + instance.loggers.size() + " worlds");
    }

    public static void detachServer()
    {
        instance = null;
        CarpetServer.LOGGER.debug("Detached MicroTick loggers");
    }

    public static Optional<MicroTimingLogger> getWorldLogger(World world)
    {
        if (instance != null && world instanceof WorldServer)
        {
            return Optional.of(((WorldServer) world).getMicroTickLogger());
        }
        return Optional.empty();
    }

    public static String tr(String key, String text, boolean autoFormat)
    {
        return instance.dummyLoggerForTranslate.tr(key, text, autoFormat);
    }

    public static String tr(String key, String text)
    {
        return instance.dummyLoggerForTranslate.tr(key, text);
    }

    public static String tr(String key)
    {
        return instance.dummyLoggerForTranslate.tr(key);
    }

    /*
     * -------------------------
     *  General Event Operation
     * -------------------------
     */

    public static void onEvent(World world, BlockPos pos, Supplier<BaseEvent> supplier, BiFunction<World, BlockPos, Optional<EnumDyeColor>> woolGetter)
    {
        if (isLoggerActivated())
        {
            getWorldLogger(world).ifPresent(logger -> logger.addMessage(world, pos, supplier.get(), woolGetter));
        }
    }

    public static void onEvent(World world, BlockPos pos, Supplier<BaseEvent> supplier)
    {
        if (isLoggerActivated())
        {
            getWorldLogger(world).ifPresent(logger -> logger.addMessage(world, pos, supplier.get()));
        }
    }

    /*
     * ----------------------------------
     *  Block Update and Block Operation
     * ----------------------------------
     */

    public static void onBlockUpdate(World world, BlockPos pos, Block fromBlock, BlockUpdateType updateType, EnumFacing exceptSide, EventType eventType)
    {
        onEvent(world, pos, () -> new DetectBlockUpdateEvent(eventType, fromBlock, updateType, () -> updateType.getUpdateOrderList(exceptSide)), MicroTimingUtil::getEndRodWoolColor);
    }

    public static void onSetBlockState(World world, BlockPos pos, IBlockState oldState, IBlockState newState, Boolean returnValue, int flags, EventType eventType)
    {
        if (isLoggerActivated())
        {
            if (oldState.getBlock() == newState.getBlock())
            {
                getWorldLogger(world).ifPresent(logger -> logger.onSetBlockState(world, pos, oldState, newState, returnValue, flags, eventType));
            }
        }
    }

    /*
     * -----------
     *  Tile Tick
     * -----------
     */

    public static void onExecuteTileTickEvent(World world, NextTickListEntry<Block> event, EventType eventType)
    {
        onEvent(world, event.position, () -> new ExecuteTileTickEvent(eventType, event));
    }

    public static void onScheduleTileTickEvent(World world, Block block, BlockPos pos, int delay, TickPriority priority, Boolean success)
    {
        onEvent(world, pos, () -> new ScheduleTileTickEvent(block, pos, delay, priority, success));
    }

    /*
     * -------------
     *  Block Event
     * -------------
     */

    public static void onExecuteBlockEvent(World world, BlockEventData blockAction, Boolean returnValue, ExecuteBlockEventEvent.FailInfo failInfo, EventType eventType)
    {
        onEvent(world, blockAction.getPosition(), () -> new ExecuteBlockEventEvent(eventType, blockAction, returnValue, failInfo));
    }

    public static void onScheduleBlockEvent(World world, BlockEventData blockAction, boolean success)
    {
        onEvent(world, blockAction.getPosition(), () -> new ScheduleBlockEventEvent(blockAction, success));
    }

    /*
     * ------------------
     *  Component things
     * ------------------
     */

    public static void onEmitBlockUpdate(World world, Block block, BlockPos pos, EventType eventType, String methodName)
    {
        onEvent(world, pos, () -> new EmitBlockUpdateEvent(eventType, block, methodName));
    }

    public static void onEmitBlockUpdateRedstoneDust(World world, Block block, BlockPos pos, EventType eventType, String methodName, Collection<BlockPos> updateOrder)
    {
        onEvent(world, pos, () -> new EmitBlockUpdateRedstoneDustEvent(eventType, block, methodName, pos, updateOrder));
    }

    /*
     * ------------
     *  Tick Stage
     * ------------
     */

    public static void setTickStage(World world, TickStage stage)
    {
        getWorldLogger(world).ifPresent(logger -> logger.setTickStage(stage));
    }
    public static void setTickStage(TickStage stage)
    {
        if (instance != null)
        {
            for (MicroTimingLogger logger : instance.loggers.values())
            {
                logger.setTickStage(stage);
            }
        }
    }

    public static void setTickStageDetail(World world, String detail)
    {
        getWorldLogger(world).ifPresent(logger -> logger.setTickStageDetail(detail));
    }

    public static void setTickStageExtra(World world, TickStageExtraBase stage)
    {
        getWorldLogger(world).ifPresent(logger -> logger.setTickStageExtra(stage));
    }
    public static void setTickStageExtra(TickStageExtraBase stage)
    {
        if (instance != null)
        {
            for (MicroTimingLogger logger : instance.loggers.values())
            {
                logger.setTickStageExtra(stage);
            }
        }
    }

    /*
     * ------------
     *  Interfaces
     * ------------
     */

    private void flush(long gameTime) // needs to call at the end of a gt
    {
        if (gameTime != this.lastFlushTime)
        {
            this.lastFlushTime = gameTime;
            for (MicroTimingLogger logger : this.loggers.values())
            {
                logger.flushMessages();
            }
        }
    }

    public static void flushMessages(long gameTime) // needs to call at the end of a gt
    {
        if (instance != null && isLoggerActivated())
        {
            instance.flush(gameTime);
        }
    }

    public static Optional<TickStage> getTickStage(World world)
    {
        return getWorldLogger(world).map(MicroTimingLogger::getTickStage);
    }
}
