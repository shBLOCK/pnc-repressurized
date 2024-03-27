/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.block.entity.IGUIButtonSensitive;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

/**
 * Received on: SERVER
 * Sent when a GUI button is clicked.
 */
public record PacketGuiButton(String tag, boolean shiftHeld) implements CustomPacketPayload {
    public static final ResourceLocation ID = RL("gui_button");

    public PacketGuiButton(String tag) {
        this(tag, ClientUtils.hasShiftDown());
    }

    public PacketGuiButton(FriendlyByteBuf buffer) {
        this(buffer.readUtf(1024), buffer.readBoolean());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(tag);
        buffer.writeBoolean(shiftHeld);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static void handle(PacketGuiButton message, PlayPayloadContext ctx) {
        ctx.player().ifPresent(player -> ctx.workHandler().submitAsync(() -> {
            if (player instanceof ServerPlayer sp && sp.containerMenu instanceof IGUIButtonSensitive gbs) {
                gbs.handleGUIButtonPress(message.tag, message.shiftHeld, sp);
            }
        }));
    }
}
