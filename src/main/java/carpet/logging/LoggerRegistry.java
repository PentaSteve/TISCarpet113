package carpet.logging;

import carpet.logging.commandblock.CommandBlockLogger;
import carpet.logging.lifetime.LifeTimeHUDLogger;
import carpet.logging.microtiming.utils.MicroTimingStandardCarpetLogger;
import carpet.logging.tickwarp.TickWarpHUDLogger;
import carpet.settings.CarpetSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LoggerRegistry
{
    // Map from logger names to loggers.
    private static Map<String, Logger> loggerRegistry = new HashMap<>();
    // Map from player names to the set of names of the logs that player is subscribed to.
    public static Map<String, Map<String, String>> playerSubscriptions = new HashMap<>();
    //statics to quickly asses if its worth even to call each one
    public static boolean __tnt;
    public static boolean __projectiles;
    public static boolean __fallingBlocks;
    public static boolean __kills;
    public static boolean __tps;
    public static boolean __counter;
    public static boolean __mobcaps;
    public static boolean __damage;
    public static boolean __packets;
    public static boolean __weather;
    public static boolean __pathfinding;
    public static boolean __chunkdebug;
    public static boolean __villagecount;
    public static boolean __memory;
    public static boolean __microTiming;
    public static boolean __autosave;
    public static boolean __commandBlock;
    public static boolean __tickWarp;
    public static boolean __lifeTime;
    public static boolean __savestate;

    public static void initLoggers()
    {
        registerLogger("tnt", new Logger("tnt", "brief", new String[]{"brief", "full"}));
        registerLogger("projectiles", new Logger("projectiles", "brief",  new String[]{"brief", "full", "visualize"}));
        registerLogger("fallingBlocks",new Logger("fallingBlocks", "brief", new String[]{"brief", "full"}));
        registerLogger("kills", new Logger("kills", null, null));
        registerLogger("damage", new Logger("damage", "all", new String[]{"all","players","me"}));
        registerLogger("weather", new Logger("weather", null, null));
        registerLogger( "pathfinding", new Logger("pathfinding", "20", new String[]{"2", "5", "10"}));

        registerLogger("tps", new HUDLogger("tps", null, null));
        registerLogger("packets", new HUDLogger("packets", null, null));
        registerLogger("counter",new HUDLogger("counter","white", Arrays.stream(EnumDyeColor.values()).map(Object::toString).toArray(String[]::new)));
        registerLogger("mobcaps", new HUDLogger("mobcaps", "dynamic",new String[]{"dynamic", "overworld", "nether","end"}));

        // TISCM loggers
        registerLogger("chunkdebug", new Logger("chunkdebug",null, null));
        registerLogger("villagecount", new HUDLogger("villagecount", null, null));
        registerLogger("memory", new HUDLogger("memory", null, null));
        registerLogger("microTiming", MicroTimingStandardCarpetLogger.getInstance());
        registerLogger("autosave", new HUDLogger("autosave", null, null));
        registerLogger(CommandBlockLogger.NAME, new Logger(CommandBlockLogger.NAME, "throttled", new String[]{"throttled", "all"}));
        registerLogger(TickWarpHUDLogger.NAME, new HUDLogger(TickWarpHUDLogger.NAME, "bar", new String[]{"bar", "value"}));
        registerLogger(LifeTimeHUDLogger.NAME, LifeTimeHUDLogger.getInstance().getHUDLogger());
        registerLogger("savestate", new Logger("savestate",null, null));
    }

    /**
     * Gets the logger with the given name. Returns null if no such logger exists.
     */
    public static Logger getLogger(String name) { return loggerRegistry.get(name); }

    /**
     * Gets the set of logger names.
     */
    public static Set<String> getLoggerNames() { return loggerRegistry.keySet(); }

    /**
     * Subscribes the player with name playerName to the log with name logName.
     */
    public static void subscribePlayer(String playerName, String logName, String option)
    {
        if (!playerSubscriptions.containsKey(playerName)) playerSubscriptions.put(playerName, new HashMap<>());
        Logger log = loggerRegistry.get(logName);
        if (option == null) option = log.getDefault();
        playerSubscriptions.get(playerName).put(logName,option);
        log.addPlayer(playerName, option);
        log.dumpPersistantSubs();
    }

    /**
     * Unsubscribes the player with name playerName from the log with name logName.
     */
    public static void unsubscribePlayer(String playerName, String logName)
    {
        if (playerSubscriptions.containsKey(playerName))
        {
            Map<String,String> subscriptions = playerSubscriptions.get(playerName);
            subscriptions.remove(logName);
            loggerRegistry.get(logName).removePlayer(playerName);
            if (subscriptions.size() == 0) playerSubscriptions.remove(playerName);
            loggerRegistry.get(logName).dumpPersistantSubs();
        }
    }

    /**
     * If the player is not subscribed to the log, then subscribe them. Otherwise, unsubscribe them.
     */
    public static boolean togglePlayerSubscription(String playerName, String logName)
    {
        if (playerSubscriptions.containsKey(playerName) && playerSubscriptions.get(playerName).containsKey(logName))
        {
            unsubscribePlayer(playerName, logName);
            return false;
        }
        else
        {
            subscribePlayer(playerName, logName, null);
            return true;
        }
    }

    /**
     * Get the set of logs the current player is subscribed to.
     */
    public static Map<String,String> getPlayerSubscriptions(String playerName)
    {
        if (playerSubscriptions.containsKey(playerName))
        {
            return playerSubscriptions.get(playerName);
        }
        return null;
    }

    protected static void setAccess(Logger logger)
    {
        String name = logger.getLogName();
        boolean value = logger.hasOnlineSubscribers();
        try
        {
            Field f = LoggerRegistry.class.getDeclaredField("__"+name);
            f.setBoolean(null, value);
        }
        catch (IllegalAccessException e)
        {
            CarpetSettings.LOG.error("Cannot change logger quick access field");
        }
        catch (NoSuchFieldException e)
        {
            CarpetSettings.LOG.error("Wrong logger name");
        }
    }
    /**
     * Called when the server starts. Creates the logs used by Carpet mod.
     */
    private static void registerLogger(String name, Logger logger)
    {
        loggerRegistry.put(name, logger);
        setAccess(logger);
    }

    public static void playerConnected(EntityPlayer player)
    {
        for(Logger log: loggerRegistry.values() )
        {
            log.onPlayerConnect(player);
        }

    }
    public static void playerDisconnected(EntityPlayer player)
    {
        for(Logger log: loggerRegistry.values() )
        {
            log.onPlayerDisconnect(player);
        }
    }



}
