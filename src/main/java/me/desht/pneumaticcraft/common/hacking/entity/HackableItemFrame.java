package me.desht.pneumaticcraft.common.hacking.entity;

import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IHackableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;

import java.util.List;

public class HackableItemFrame implements IHackableEntity {
    @Override
    public String getId() {
        return "itemFrame";
    }

    @Override
    public boolean canHack(Entity entity, PlayerEntity player) {
        return entity instanceof ItemFrameEntity && !((ItemFrameEntity) entity).getDisplayedItem().isEmpty();
    }

    @Override
    public void addHackInfo(Entity entity, List<String> curInfo, PlayerEntity player) {
        curInfo.add("Hack to detach item");
    }

    @Override
    public void addPostHackInfo(Entity entity, List<String> curInfo, PlayerEntity player) {
        curInfo.add("Item detached!");
    }

    @Override
    public int getHackTime(Entity entity, PlayerEntity player) {
        return 60;
    }

    @Override
    public void onHackFinished(Entity entity, PlayerEntity player) {
        if (!entity.world.isRemote) {
            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 0.1f);
        }
    }

    @Override
    public boolean afterHackTick(Entity entity) {
        return false;
    }
}
