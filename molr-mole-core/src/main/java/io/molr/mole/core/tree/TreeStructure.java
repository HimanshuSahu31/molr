package io.molr.mole.core.tree;

import io.molr.commons.domain.Block;
import io.molr.commons.domain.ImmutableMissionRepresentation;
import io.molr.commons.domain.MissionRepresentation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO #1 consider merging with MissionRepresentation
 */
public class TreeStructure {

    private final MissionRepresentation representation;
    private final Set<Block> parallelBlocks;

    public TreeStructure(MissionRepresentation representation, Set<Block> parallelBlocks) {
        this.representation = representation;
        this.parallelBlocks = parallelBlocks;
    }

    /**
     * Returns a new {@link TreeStructure} with the specified {@link Block} as root. It will not return a view of this
     * structure but a completely new one.
     * <p>
     * NOTE: The current implementation is not optimized for performance...
     */
    public TreeStructure substructure(Block block) {
        if (!representation.allBlocks().contains(block)) {
            throw new IllegalArgumentException("Block " + block + " is not part of this structure");
        }

        ImmutableMissionRepresentation.Builder builder = ImmutableMissionRepresentation.builder(block);
        addChildren(block, null, builder);

        MissionRepresentation subrepresentation = builder.build();
        Set<Block> subparallelBlocks = parallelBlocks.stream()
                .filter(subrepresentation.allBlocks()::contains).collect(Collectors.toSet());
        return new TreeStructure(subrepresentation, subparallelBlocks);
    }

    /**
     * Optionally returns the next block in the tree structure of the specified parameter. Since this method does not
     * know the history of the movements, it will not return as next block a children of the parameter. On the contrary,
     * if the parameter is the last child of a sequence, it will automatically resolve the sibling of the parent as the
     * next block. An empty {@link Optional} indicates that there is no next block and the tree navigation can be
     * considered finished
     * <p>
     * TODO ?? think if it makes sense to have a VisitorInstance that knows how to navigate from a block onwards..
     */
    public Optional<Block> nextBlock(Block actualBlock) {
        Optional<Block> maybeParent = parentOf(actualBlock);
        if (!maybeParent.isPresent()) {
            /* Root block has no next */
            return Optional.empty();
        }
        Block parent = maybeParent.get();

        List<Block> siblings = childrenOf(parent);
        if (isLastSibling(actualBlock, siblings)) {
            return nextBlock(parent);
        }

        return nextSiblingOf(actualBlock, siblings);
    }

    public List<Block> childrenOf(Block block) {
        return representation.childrenOf(block);
    }

    public boolean isParallel(Block block) {
        return parallelBlocks.contains(block);
    }

    public boolean isLeaf(Block block) {
        return representation.isLeaf(block);
    }

    public Block rootBlock() {
        return representation.rootBlock();
    }

    public Set<Block> allBlocks() {
        return representation.allBlocks();
    }

    public boolean contains(Block block) {
        return allBlocks().contains(block);
    }

    public MissionRepresentation missionRepresentation() {
        return representation;
    }

    /**
     * Determines whether or not the {@code target} is a descendant of the {@code source} block in this structure.
     * <p>
     * NOTE: The current implementation is not optimized for performance...
     */
    public boolean isDescendantOf(Block target, Block source) {
        return substructure(source).allBlocks().contains(target);
    }

    private void addChildren(Block child, Block parent, ImmutableMissionRepresentation.Builder builder) {
        if (parent != null) {
            builder.parentToChild(parent, child);
        }

        if (!isLeaf(child)) {
            for (Block grandChild : childrenOf(child)) {
                addChildren(grandChild, child, builder);
            }
        }
    }

    private boolean isLastSibling(Block actualBlock, List<Block> siblings) {
        return siblings.indexOf(actualBlock) >= siblings.size() - 1;
    }

    private Optional<Block> nextSiblingOf(Block block, List<Block> siblings) {
        if (isLastSibling(block, siblings)) {
            return Optional.empty();
        }
        return Optional.of(siblings.get(siblings.indexOf(block) + 1));
    }

    public Optional<Block> parentOf(Block block) {
        return representation.parentOf(block);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeStructure that = (TreeStructure) o;
        return Objects.equals(representation, that.representation) &&
                Objects.equals(parallelBlocks, that.parallelBlocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(representation, parallelBlocks);
    }
}
