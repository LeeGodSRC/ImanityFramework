/*
 * MIT License
 *
 * Copyright (c) 2020 - 2020 Imanity
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.imanity.framework.bukkit.listener;

import co.aikar.timings.TimedEventExecutor;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.imanity.framework.bukkit.listener.annotation.IgnoredFilters;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor
public class FilteredListener<T extends Plugin> implements Listener {

    @Getter
    private FilteredEventList checker;
    public T plugin;

    public FilteredListener(FilteredEventList checker, T plugin) {
        this.initial(plugin, checker);
    }

    public void initial(T plugin, FilteredEventList eventChecker) {
        this.plugin = plugin;
        this.checker = eventChecker;

        this.init(this.plugin);
    }

    private void init(Plugin plugin) {
        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
        Set<Method> methods;
        try {
            Method[] publicMethods = this.getClass().getMethods();
            Method[] privateMethods = this.getClass().getDeclaredMethods();
            ImmutableSet.Builder<Method> builder = ImmutableSet.builder();
            for (Method method : publicMethods) {
                builder.add(method);
            }
            for (Method method : privateMethods) {
                builder.add(method);
            }

            methods = builder.build();
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().severe("Plugin " + plugin.getDescription().getFullName() + " has failed to register events for " + this.getClass() + " because " + e.getMessage() + " does not exist.");
            return;
        }

        for (Method method : methods) {
            EventHandler eventHandler = method.getAnnotation(EventHandler.class);
            if (eventHandler == null) {
                continue;
            }
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                plugin.getLogger().severe(plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + this.getClass());
                continue;
            }
            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            Set<RegisteredListener> eventSet = ret.computeIfAbsent(eventClass, k -> new HashSet<>());

            boolean ignoredFilters = method.getAnnotation(IgnoredFilters.class) != null;

            EventExecutor executor = new TimedEventExecutor((listener, event) -> {
                try {
                    if (!eventClass.isAssignableFrom(event.getClass())) {
                        return;
                    }
                    if (!ignoredFilters && !checker.check(event)) {
                        return;
                    }
                    method.invoke(listener, event);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, plugin, method, eventClass);
            eventSet.add(new RegisteredListener(this, executor, eventHandler.priority(), plugin, eventHandler.ignoreCancelled()));
        }
        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : ret.entrySet()) {
            getEventListeners(getRegistrationClass(entry.getKey())).registerAll(entry.getValue());
        }
    }

    private static HandlerList getEventListeners(Class<? extends Event> type) {
        try {
            Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalPluginAccessException(e.toString());
        }
    }

    private static Class<? extends Event> getRegistrationClass(Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null
                    && !clazz.getSuperclass().equals(Event.class)
                    && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            } else {
                throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName() + ". Static getHandlerList method required!");
            }
        }
    }

}
