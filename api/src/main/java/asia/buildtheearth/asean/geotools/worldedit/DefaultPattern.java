package asia.buildtheearth.asean.geotools.worldedit;

import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Wrapper for {@link BlockType} pattern.
 * Usually we don't need this if we use FastAsync-WorldEdit.
 */
public class DefaultPattern extends AbstractPattern {
    private final Supplier<BlockType> holder;

    public DefaultPattern(Supplier<BlockType> holder) {
        this.holder = holder;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BlockType type = Objects.requireNonNull(holder.get());
        return type.getDefaultState().applyBlock(position);
    }
}
