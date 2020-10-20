package org.imanity.framework;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.imanity.framework.command.ICommandExecutor;
import org.imanity.framework.config.CoreConfig;
import org.imanity.framework.data.DataHandler;
import org.imanity.framework.database.Mongo;
import org.imanity.framework.database.MySQL;
import org.imanity.framework.events.IEventHandler;
import org.imanity.framework.exception.OptionNotEnabledException;
import org.imanity.framework.libraries.Library;
import org.imanity.framework.libraries.LibraryHandler;
import org.imanity.framework.locale.Locale;
import org.imanity.framework.locale.LocaleHandler;
import org.imanity.framework.locale.player.LocaleData;
import org.imanity.framework.player.IPlayerBridge;
import org.imanity.framework.data.PlayerData;
import org.imanity.framework.plugin.service.Autowired;
import org.imanity.framework.plugin.service.ServiceHandler;
import org.imanity.framework.redis.ImanityRedis;
import org.imanity.framework.redis.server.enums.ServerState;
import org.imanity.framework.task.ITaskScheduler;
import java.util.EnumSet;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImanityCommon {

    private static final Set<Library> GLOBAL_LIBRARIES = EnumSet.of(Library.REDISSON, Library.MONGO_DB, Library.HIKARI_CP, Library.YAML, Library.CAFFEINE);

    public static ImanityBridge BRIDGE;
    public static CoreConfig CORE_CONFIG;
    public static ServiceHandler SERVICE_HANDLER;

    @Autowired
    public static LocaleHandler LOCALE_HANDLER;
    public static LibraryHandler LIBRARY_HANDLER;
    public static String METADATA_PREFIX = "Imanity_";

    public static ICommandExecutor COMMAND_EXECUTOR;
    public static IEventHandler EVENT_HANDLER;
    public static ITaskScheduler TASK_SCHEDULER;

    @Autowired
    public static MySQL SQL;

    @Autowired
    public static Mongo MONGO;

    @Autowired
    public static ImanityRedis REDIS;

    private static boolean librariesInitialized, bridgeInitialized;

    public static void init() {
        ImanityCommon.CORE_CONFIG = new CoreConfig();
        ImanityCommon.CORE_CONFIG.loadAndSave();

        ImanityCommon.loadLibraries();

        ImanityCommon.SERVICE_HANDLER = new ServiceHandler();
        ImanityCommon.SERVICE_HANDLER.registerServices();
        ImanityCommon.SERVICE_HANDLER.init();
    }

    public static void loadLibraries() {

        if (ImanityCommon.librariesInitialized) {
            return;
        }
        ImanityCommon.librariesInitialized = true;

        getLogger().info("Loading Libraries");

        ImanityCommon.LIBRARY_HANDLER = new LibraryHandler();
        ImanityCommon.LIBRARY_HANDLER.downloadLibraries(GLOBAL_LIBRARIES);
        ImanityCommon.LIBRARY_HANDLER.obtainClassLoaderWith(GLOBAL_LIBRARIES);

        try {
            Class.forName("com.google.common.collect.ImmutableList");
        } catch (ClassNotFoundException ex) {
            // Below 1.8
            EnumSet<Library> guava = EnumSet.of(Library.GUAVA);
            ImanityCommon.LIBRARY_HANDLER.downloadLibraries(guava);
            ImanityCommon.LIBRARY_HANDLER.obtainClassLoaderWith(guava);
        }

        try {
            Class.forName("it.unimi.dsi.fastutil.Arrays");
        } catch (ClassNotFoundException ex) {
            EnumSet<Library> guava = EnumSet.of(Library.FAST_UTIL);
            ImanityCommon.LIBRARY_HANDLER.downloadLibraries(guava);
            ImanityCommon.LIBRARY_HANDLER.obtainClassLoaderWith(guava);
        }
    }

    public static Logger getLogger() {
        return ImanityCommon.BRIDGE.getLogger();
    }

    public static <T> T getService(Class<T> type) {
        return (T) SERVICE_HANDLER.getServiceInstance(type);
    }

    public static void registerAutowired(Object instance) {
        SERVICE_HANDLER.registerAutowired(instance);
    }

    public static void shutdown() {
        if (ImanityCommon.CORE_CONFIG.USE_REDIS) {
            ImanityCommon.REDIS.getServerHandler().changeServerState(ServerState.STOPPING);
        }

        DataHandler.shutdown();
        ImanityCommon.SERVICE_HANDLER.stop();
    }

    public static String translate(Object player, String key) {
        if (!ImanityCommon.CORE_CONFIG.USE_LOCALE) {
            throw new OptionNotEnabledException("use_locale", "config.yml");
        }
        LocaleData localeData = DataHandler.getPlayerData(player, LocaleData.class);
        Locale locale;
        if (localeData == null || localeData.getLocale() == null) {
            locale = ImanityCommon.LOCALE_HANDLER.getDefaultLocale();
        } else {
            locale = localeData.getLocale();
        }
        return locale.get(key);
    }

    public static Builder builder() {
        if (ImanityCommon.bridgeInitialized) {
            throw new IllegalStateException("Already build!");
        }

        ImanityCommon.bridgeInitialized = true;
        return new Builder();
    }

    public static class Builder {

        private ImanityBridge bridge;
        private ICommandExecutor commandExecutor;
        private IEventHandler eventHandler;
        private ITaskScheduler taskScheduler;
        private IPlayerBridge playerBridge;

        public Builder bridge(ImanityBridge bridge) {
            this.bridge = bridge;
            return this;
        }

        public Builder commandExecutor(ICommandExecutor commandExecutor) {
            this.commandExecutor = commandExecutor;
            return this;
        }

        public Builder eventHandler(IEventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public Builder taskScheduler(ITaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
            return this;
        }

        public Builder playerBridge(IPlayerBridge playerBridge) {
            this.playerBridge = playerBridge;
            return this;
        }

        public void init() {
            if (this.bridge != null) {
                ImanityCommon.BRIDGE = this.bridge;
            }
            if (this.commandExecutor != null) {
                ImanityCommon.COMMAND_EXECUTOR = this.commandExecutor;
            }
            if (this.eventHandler != null) {
                ImanityCommon.EVENT_HANDLER = this.eventHandler;
            }
            if (this.taskScheduler != null) {
                ImanityCommon.TASK_SCHEDULER = this.taskScheduler;
            }
            if (this.playerBridge != null) {
                PlayerData.PLAYER_BRIDGE = this.playerBridge;
            }
            ImanityCommon.init();
        }

    }

}
