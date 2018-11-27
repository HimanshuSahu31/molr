package org.molr.mole.core.tree;

import org.molr.commons.domain.Block;
import org.molr.commons.domain.Out;
import org.molr.commons.domain.Placeholder;

import static java.util.Objects.requireNonNull;

public class BlockOutputCollector implements Out {

    private final MissionOutputCollector collector;
    private final Block block;

    public BlockOutputCollector(MissionOutputCollector collector, Block block) {
        this.collector = requireNonNull(collector, "collector must not be null");
        this.block = requireNonNull(block, "block must not be null");
    }

    @Override
    public void emit(String name, Number value) {
        collector.put(block, name, value);
    }

    @Override
    public void emit(String name, String value) {
        collector.put(block, name, value);
    }

    @Override
    public <T> void emit(Placeholder<T> placeholder, T value) {
        collector.put(block, placeholder, value);
    }


}
