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

import me.desht.pneumaticcraft.common.tubemodules.RedstoneModule;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

/**
 * Received on: CLIENT
 * Sent by server to sync up the settings of a redstone module
 */
public record PacketSyncRedstoneModuleToClient(ModuleLocator locator, RedstoneModule.EnumRedstoneDirection dir, int outputLevel, int inputLevel, int channel) implements TubeModulePacket<RedstoneModule> {
    public static final ResourceLocation ID = RL("sync_redstone_module_to_client");

    public static PacketSyncRedstoneModuleToClient create(RedstoneModule module) {
        return new PacketSyncRedstoneModuleToClient(
                ModuleLocator.forModule(module),
                module.getRedstoneDirection(),
                module.getRedstoneLevel(),
                module.getInputLevel(),
                module.getColorChannel()
        );
    }

    public static PacketSyncRedstoneModuleToClient fromNetwork(FriendlyByteBuf buffer) {
        return new PacketSyncRedstoneModuleToClient(
                ModuleLocator.fromNetwork(buffer),
                buffer.readEnum(RedstoneModule.EnumRedstoneDirection.class),
                buffer.readByte(),
                buffer.readByte(),
                buffer.readByte()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        locator.write(buf);
        buf.writeEnum(dir);
        buf.writeByte(outputLevel);
        buf.writeByte(inputLevel);
        buf.writeByte(channel);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onModuleUpdate(RedstoneModule module, Player player) {
        module.setColorChannel(channel);
        module.setRedstoneDirection(dir);
        module.setOutputLevel(outputLevel);
        module.setInputLevel(inputLevel);
    }
}
