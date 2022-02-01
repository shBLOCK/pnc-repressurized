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

package me.desht.pneumaticcraft.common.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.network.NetworkHooks;

public class EntityVortex extends ThrowableProjectile {
    private int hitCounter = 0;

    // clientside: rendering X offset of vortex, depends on which hand the vortex was fired from
    private float renderOffsetX = -Float.MAX_VALUE;

    public EntityVortex(EntityType<? extends EntityVortex> type, Level world) {
        super(type, world);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();

        // onImpact() is no longer called for blocks with no collision box, like shrubs & crops, as of MC 1.16.2
        if (!level.isClientSide) {
            if (vortexBreakable(level.getBlockState(getOnPos()).getBlock())) {
                handleVortexCollision(getOnPos());
            } else if (vortexBreakable(level.getBlockState(blockPosition()).getBlock())) {
                handleVortexCollision(blockPosition());
            } else {
                Vec3 m = getDeltaMovement().scale(0.5);
                Vec3 p = position().add(m.x(), m.y(), m.z());
                BlockPos pos1 = new BlockPos(p.x(), p.y(), p.z());
                if (!pos1.equals(blockPosition()) && vortexBreakable(level.getBlockState(pos1).getBlock())) {
                    handleVortexCollision(pos1);
                }
            }
        }

        setDeltaMovement(getDeltaMovement().scale(0.95));
        if (getDeltaMovement().lengthSqr() < 0.1D) {
            discard();
        }
    }

    public boolean hasRenderOffsetX() {
        return renderOffsetX > -Float.MAX_VALUE;
    }

    public float getRenderOffsetX() {
        return renderOffsetX;
    }

    public void setRenderOffsetX(float renderOffsetX) {
        this.renderOffsetX = renderOffsetX;
    }

    private boolean tryCutPlants(BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (vortexBreakable(block)) {
            level.destroyBlock(pos, true);
            return true;
        }
        return false;
    }

    @Override
    public float getGravity() {
        return 0;
    }

    @Override
    protected void onHit(HitResult rtr) {
        if (rtr.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) rtr).getEntity();
            entity.setDeltaMovement(entity.getDeltaMovement().add(this.getDeltaMovement().add(0, 0.4, 0)));
            ItemStack shears = new ItemStack(Items.SHEARS);
            // getOwner = getShooter
            if (entity instanceof LivingEntity) {
                Player shooter = getOwner() instanceof Player ? (Player) getOwner() : null;
                if (shooter != null) shears.getItem().interactLivingEntity(shears, shooter, (LivingEntity) entity, InteractionHand.MAIN_HAND);
            }
        } else if (rtr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) rtr).getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            if (vortexBreakable(block)) {
                if (!level.isClientSide) {
                    handleVortexCollision(pos);
                }
            } else {
                discard();
            }
        }
        hitCounter++;
        if (hitCounter > 20) discard();
    }

    private void handleVortexCollision(BlockPos pos) {
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        if (tryCutPlants(pos)) {
            int plantsCut = 1;
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        mPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                        if (tryCutPlants(mPos)) plantsCut++;
                    }
                }
            }
            // slow the vortex down for each plant it broke
            double mult = Math.pow(0.85D, plantsCut);
            setDeltaMovement(getDeltaMovement().scale(mult));
        }
    }

    private boolean vortexBreakable(Block block) {
        return block instanceof IPlantable || block instanceof LeavesBlock || block instanceof WebBlock;
    }

    @Override
    protected void defineSynchedData() {

    }
}
