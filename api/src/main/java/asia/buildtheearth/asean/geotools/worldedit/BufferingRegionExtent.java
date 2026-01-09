package asia.buildtheearth.asean.geotools.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractFlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.iterator.FlatRegionIterator;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class BufferingRegionExtent extends NullExtent {
    private int minX = 0, minY = 0, minZ = 0;
    private int maxX = -1, maxY = -1, maxZ = -1;

    private final BlockMap<BaseBlock> buffer;

    private BlockVector3 min = null, max = null;

    public BufferingRegionExtent(@NotNull BlockMap<BaseBlock> buffer) {
        this.buffer = buffer;
    }

    public BufferingRegionExtent() {
        this(BlockMap.createForBaseBlock());
    }

    public @NotNull BlockVector3 getMin() {
        return this.min;
    }

    public @NotNull BlockVector3 getMax() {
        return this.max;
    }

    public BlockMap<BaseBlock> getBuffer() {
        return this.buffer;
    }

    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, @NotNull T block) throws WorldEditException {
        this.include(x, y, z);
        this.buffer.put(BlockVector3.at(x, y, z), block.toBaseBlock());
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, @NotNull B block) throws WorldEditException {
        this.include(location);
        this.buffer.put(location, block.toBaseBlock());
        return true;
    }

    public void include(@NotNull BlockVector3 location) {
        int x = location.x();
        int y = location.y();
        int z = location.z();
        this.include(x, y, z);
    }

    public void include(int x, int y, int z) {
        if (this.min == null || this.max == null) {
            this.min = BlockVector3.at(x, y, z);
            this.max = BlockVector3.at(x, y, z);
            this.minX = x;
            this.maxX = x;
            this.minY = y;
            this.maxY = y;
            this.minZ = z;
            this.maxZ = z;
        } else {
            if (x < this.minX) this.minX = x;
            if (y < this.minY) this.minY = y;
            if (z < this.minZ) this.minZ = z;

            if (x > this.maxX) this.maxX = x;
            if (y > this.maxY) this.maxY = y;
            if (z > this.maxZ) this.maxZ = z;

            // Avoid construction overhead
            if(this.min.x() != this.minX
            || this.min.z() != this.minZ
            || this.min.y() != this.minY)
                this.min = BlockVector3.at(this.minX, this.minY, this.minZ);

            if(this.max.x() != this.maxX
            || this.max.z() != this.maxZ
            || this.max.y() != this.maxY)
                this.max = BlockVector3.at(this.maxX, this.maxY, this.maxZ);
        }
    }

    @Contract("-> new")
    public Region asRegion() {
        return new AbstractFlatRegion(null) {
            @Override
            public BlockVector3 getMinimumPoint() {
                return min != null? getMin() : BlockVector3.ZERO;
            }

            @Override
            public BlockVector3 getMaximumPoint() {
                return max != null? getMax() : BlockVector3.ZERO;
            }

            @Override
            public void expand(BlockVector3... changes) {
                throw new UnsupportedOperationException("Cannot change the size of this region");
            }

            @Override
            public void contract(BlockVector3... changes) {
                throw new UnsupportedOperationException("Cannot change the size of this region");
            }

            @Override
            public boolean contains(BlockVector3 position) {
                return buffer.containsKey(position);
            }

            @Override
            public @NotNull Iterator<BlockVector3> iterator() {
                return buffer.keySet().iterator();
            }

            @Override
            public Iterable<BlockVector2> asFlatRegion() {
                return () -> new FlatRegionIterator(this);
            }
        };
    }
}
