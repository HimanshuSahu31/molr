package org.molr.mole.core.runnable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.molr.commons.domain.Block;
import org.molr.commons.domain.ImmutableMissionRepresentation;
import org.molr.commons.domain.MissionRepresentation;
import org.molr.mole.core.tree.TreeStructure;
import sun.reflect.generics.tree.Tree;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutionData {

    private final ImmutableMap<Block, Runnable> runnables;
    private final TreeStructure treeStructure;

    private ExecutionData(Builder builder) {
        this.runnables = builder.runnables.build();
        MissionRepresentation representation = builder.representationBuilder.build();
        this.treeStructure = new TreeStructure(representation, builder.parallelBlocksBuilder.build());
    }

    public TreeStructure treeStructure() {
        return this.treeStructure;
    }

    public Map<Block, Runnable> runnables() {
        return this.runnables;
    }


    public static Builder builder(String rootName) {
        return new Builder(rootName);
    }

    public static class Builder {

        private final AtomicLong nextId = new AtomicLong(0);

        private final ImmutableMissionRepresentation.Builder representationBuilder;
        private final ImmutableMap.Builder<Block, Runnable> runnables = ImmutableMap.builder();
        private final ImmutableSet.Builder<Block> parallelBlocksBuilder = ImmutableSet.builder();

        private Builder(String rootName) {
            Block root = block(rootName);
            representationBuilder = ImmutableMissionRepresentation.builder(root);
        }

        public Block sequentialChild(Block parent, String childName) {
            return addChild(parent, childName);
        }

        public Block parallelChild(Block parent, String childName) {
            Block child = addChild(parent, childName);
            parallelBlocksBuilder.add(child);
            return child;
        }

        public void nodeChild(Block parent, String childName, Runnable runnable) {
            Block child = addChild(parent, childName);
            runnables.put(child, runnable);
        }

        public Block root() {
            return representationBuilder.root();
        }

        public ExecutionData build() {
            return new ExecutionData(this);
        }

        private Block addChild(Block parent, String childName) {
            Block child = block(childName);
            representationBuilder.parentToChild(parent, child);
            return child;
        }

        private Block block(String name) {
            return Block.idAndText("" + nextId.getAndIncrement(), name);
        }
    }
}
