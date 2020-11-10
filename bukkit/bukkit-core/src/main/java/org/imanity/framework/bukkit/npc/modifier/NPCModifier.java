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

package org.imanity.framework.bukkit.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.imanity.framework.bukkit.npc.NPC;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An NPCModifier queues packets for NPC modification which can then be send to players via the {@link NPCModifier#send(Player...)} method.
 */
public class NPCModifier {

    public static final int MINECRAFT_VERSION = ProtocolLibrary.getProtocolManager().getMinecraftVersion().getMinor();

    protected NPC npc;

    private final List<PacketContainer> packetContainers = new CopyOnWriteArrayList<>();

    public NPCModifier(NPC npc) {
        this.npc = npc;
    }

    protected PacketContainer newContainer(PacketType packetType) {
        return this.newContainer(packetType, true);
    }

    protected PacketContainer newContainer(PacketType packetType, boolean withEntityId) {
        PacketContainer packetContainer = new PacketContainer(packetType);

        if (withEntityId) {
            packetContainer.getIntegers().write(0, this.npc.getEntityId());
        }
        this.packetContainers.add(packetContainer);

        return packetContainer;
    }

    protected PacketContainer lastContainer() {
        return this.packetContainers.get(this.packetContainers.size() - 1);
    }

    protected PacketContainer lastContainer(PacketContainer def) {
        if (this.packetContainers.isEmpty()) {
            return def;
        }
        return this.lastContainer();
    }

    /**
     * Sends the queued modifications to all players
     */
    public void send() {
        this.send(Bukkit.getOnlinePlayers().toArray(new Player[0]));
    }

    /**
     * Sends the queued modifications to certain players
     *
     * @param targetPlayers the players which should see the modification
     */
    public void send(Player... targetPlayers) {
        for (Player targetPlayer : targetPlayers) {
            try {
                for (PacketContainer packetContainer : this.packetContainers) {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(targetPlayer, packetContainer);
                }
            } catch (InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        this.packetContainers.clear();
    }

}