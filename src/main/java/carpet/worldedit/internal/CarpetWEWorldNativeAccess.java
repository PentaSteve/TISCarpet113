/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpet.worldedit.internal;

import carpet.settings.CarpetSettings;
import carpet.worldedit.CarpetWEAdapter;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class CarpetWEWorldNativeAccess implements WorldNativeAccess<Chunk, IBlockState, BlockPos> {
    private static final int UPDATE = 1;
    private static final int NOTIFY = 2;

    private final WeakReference<World> world;
    private SideEffectSet sideEffectSet;

    public CarpetWEWorldNativeAccess(WeakReference<World> world) {
        this.world = world;
    }

    private World getWorld() {
        return Objects.requireNonNull(world.get(), "The reference to the world was lost");
    }

    @Override
    public void setCurrentSideEffectSet(SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return getWorld().getChunk(x, z);
    }

    @Override
    public IBlockState toNative(com.sk89q.worldedit.world.block.BlockState state) {
        int stateId = BlockStateIdAccess.getBlockStateId(state);
        return BlockStateIdAccess.isValidInternalId(stateId)
            ? Block.getStateById(stateId)
            : CarpetWEAdapter.adapt(state);
    }

    @Override
    public IBlockState getBlockState(Chunk chunk, BlockPos position) {
        return chunk.getBlockState(position);
    }

    @Nullable
    @Override
    public IBlockState setBlockState(Chunk chunk, BlockPos position, IBlockState state) {
        if (chunk instanceof ExtendedChunk) {
            return ((ExtendedChunk) chunk).setBlockState$worldEdit(
                position, state, false, sideEffectSet.shouldApply(SideEffect.UPDATE)
            );
        }
        return chunk.setBlockState(position, state, false);
    }

    @Override
    public IBlockState getValidBlockForPosition(IBlockState block, BlockPos position) {
        return Block.getValidBlockForPosition(block, getWorld(), position);
    }

    @Override
    public BlockPos getPosition(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    @Override
    public void updateLightingForBlock(BlockPos position) {
        getWorld().checkLight(position);
    }

    @Override
    public boolean updateTileEntity(BlockPos position, com.sk89q.jnbt.CompoundTag tag) {
        NBTTagCompound nativeTag = NBTConverter.toNative(tag);
        TileEntity tileEntity = getWorld().getChunk(position).getTileEntity(position);
        if (tileEntity == null) {
            return false;
        }
        tileEntity.setPos(position);
        tileEntity.read(nativeTag);
        return true;
    }

    @Override
    public void notifyBlockUpdate(Chunk chunk, BlockPos position, IBlockState oldState, IBlockState newState) {
        chunk.getWorld().notifyBlockUpdate(position, oldState, newState, UPDATE | NOTIFY);
    }

    @Override
    public boolean isChunkTicking(Chunk chunk) {
        return getWorld().isChunkLoaded(chunk.x, chunk.z, true);
    }

    public void markBlockChanged(Chunk chunk, BlockPos position) {
        (((WorldServer)getWorld()).getPlayerChunkMap()).markBlockForUpdate(position);
    }

    @Override
    public void notifyNeighbors(BlockPos pos, IBlockState oldState, IBlockState newState) {
        if (!CarpetSettings.fillUpdates) {
            return;
        }
        getWorld().notifyNeighborsOfStateChange(pos, oldState.getBlock());
        if (newState.hasComparatorInputOverride()) {
            getWorld().updateComparatorOutputLevel(pos, newState.getBlock());
        }
    }

    @Override
    public void updateNeighbors(BlockPos pos, IBlockState oldState, IBlockState newState, int recursionLimit) {
        if (!CarpetSettings.fillUpdates) {
            return;
        }
        World world = getWorld();
        oldState.updateDiagonalNeighbors(world, pos, NOTIFY);
        newState.updateNeighbors(world, pos, NOTIFY);
        newState.updateDiagonalNeighbors(world, pos, NOTIFY);
    }

    @Override
    public void onBlockStateChange(BlockPos pos, IBlockState oldState, IBlockState newState) {
    }
}
