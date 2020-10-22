package org.imanity.framework.discord;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.imanity.framework.boot.FrameworkBootable;
import org.imanity.framework.command.CommandProvider;
import org.imanity.framework.command.CommandService;
import org.imanity.framework.command.InternalCommandEvent;
import org.imanity.framework.discord.activity.ActivityProvider;
import org.imanity.framework.discord.command.DiscordCommandEvent;
import org.imanity.framework.discord.impl.DiscordCommandProvider;
import org.imanity.framework.discord.impl.DiscordListenerComponentHolder;
import org.imanity.framework.plugin.component.ComponentRegistry;
import org.imanity.framework.plugin.service.Autowired;
import org.imanity.framework.plugin.service.IService;
import org.imanity.framework.plugin.service.Service;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

@Service(name = "discord")
@Getter
public class DiscordService implements IService {

    public static final Logger LOGGER = LogManager.getLogger(DiscordService.class);

    public static final String
            TOKEN = "discord.token",
            LIGHT = "discord.light",
            GUILD = "discord.guild",
            ACTIVITY_UPDATE_TICKS = "discord.activityUpdateTicks",
            USE_DEFAULT_COMMAND_PROVIDER = "discord.command.useDefaultProvider";

    @Autowired
    private FrameworkBootable bootable;
    @Autowired
    private CommandService commandService;

    private TreeSet<ActivityProvider> activityProviders;
    private List<ListenerAdapter> listenerAdapters;

    private long guildId;

    private JDA jda;

    private Function<Member, @Nullable String> prefixProvider;

    @Override
    public void preInit() {
        this.listenerAdapters = new ArrayList<>();
        this.activityProviders = new TreeSet<>((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));

        ComponentRegistry.registerComponentHolder(new DiscordListenerComponentHolder());
    }

    @Override
    public void init() {
        LOGGER.info("Attempting to Login into discord...");

        this.withPrefixProvider(member -> "!");

        String token = this.bootable.get(TOKEN, null);
        Preconditions.checkNotNull(token, "The token couldn't be found! please add [discord.token] into framework bootable configuration!");

        try {
            JDABuilder builder;

            if (this.bootable.getBoolean(LIGHT, false)) {
                builder = JDABuilder.createLight(token);
            } else {
                builder = JDABuilder.createDefault(token);
            }

            for (ListenerAdapter adapter : this.listenerAdapters) {
                builder.addEventListeners(adapter);
            }
            this.listenerAdapters.clear();
            this.listenerAdapters = null;

            this.jda = builder.build();
            this.jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            this.bootable.handleError(e);
        }

        this.guildId = this.bootable.getLong(GUILD, -1);

        if (this.bootable.getBoolean(USE_DEFAULT_COMMAND_PROVIDER, true)) {
            this.commandService.withProvider(new DiscordCommandProvider());
        }

        int activityUpdateTicks = this.bootable.getInteger(ACTIVITY_UPDATE_TICKS, 20);
        this.bootable.getTaskScheduler().runAsyncRepeated(this::updateActivity, activityUpdateTicks);

        LOGGER.info("Logging into discord bot successful. discord: " + this.jda.getSelfUser().getName());
    }

    public boolean isLoggedIn() {
        return this.jda != null;
    }

    @Nullable
    public Guild getGuild() {
        Preconditions.checkArgument(this.guildId != -1, "The Guild ID hasn't been set!");
        return this.jda.getGuildById(this.guildId);
    }

    @Nullable
    public Member getMemberById(long id) {
        Guild guild = this.getGuild();
        Preconditions.checkNotNull(guild, "The Guild is null!");

        return guild.getMemberById(id);
    }

    public void registerListener(ListenerAdapter listener) {
        if (this.isLoggedIn()) {
            this.jda.addEventListener(listener);
        } else {
            this.listenerAdapters.add(listener);
        }
    }

    public void withPrefixProvider(Function<Member, @Nullable String> prefixProvider) {
        this.prefixProvider = prefixProvider;
    }

    private void updateActivity() {

        for (ActivityProvider activityProvider : this.activityProviders) {
            Activity activity = activityProvider.activity();

            if (activity != null) {
                Activity current = this.jda.getPresence().getActivity();
                if (current == null || !current.equals(activity)) {
                    this.jda.getPresence().setActivity(activity);
                }
                break;
            }
        }
    }

    public void handleMessageReceived(Member member, Message message, MessageChannel channel) {
        String rawMessage = message.getContentRaw();

        String prefix = this.getPrefixProvider().apply(member);

        // Disable if prefix is null for length is 0
        if (prefix == null || prefix.length() == 0) {
            return;
        }


        // Doesn't match to prefix
        if (!rawMessage.startsWith(prefix)) {
            return;
        }

        DiscordCommandEvent commandEvent = new DiscordCommandEvent(member, rawMessage.substring(1), channel);
        commandService.evalCommand(commandEvent);
    }
}