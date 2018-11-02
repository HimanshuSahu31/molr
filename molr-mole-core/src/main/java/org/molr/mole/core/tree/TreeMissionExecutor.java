package org.molr.mole.core.tree;

import org.molr.commons.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.molr.commons.domain.StrandCommand.STEP_INTO;


/**
 * Keeps track of the state of the execution of one mission instance. It assumes a tree of execution blocks, can execute them and keeps track of cursor positions within strands.
 */
public class TreeMissionExecutor implements MissionExecutor {

    private final Flux<MissionState> states;
    private final StrandFactoryImpl strandFactory;
    private final StrandExecutorFactory strandExecutorFactory;

    public TreeMissionExecutor(TreeStructure treeStructure, LeafExecutor leafExecutor, ResultTracker resultTracker) {
        strandFactory = new StrandFactoryImpl();
        strandExecutorFactory = new StrandExecutorFactory(strandFactory, leafExecutor);

        ReplayProcessor<Object> statesSink = ReplayProcessor.cacheLast();

        strandExecutorFactory.newStrandsStream().subscribe(newExecutor -> {
            newExecutor.getBlockStream().subscribe(any -> statesSink.onNext(new Object()));
            newExecutor.getStateStream().subscribe(any -> statesSink.onNext(new Object()));
        });

        states = statesSink.map(signal -> gatherStates()).publishOn(Schedulers.elastic()).doOnNext(a -> System.out.println(a));

        Strand rootStrand = strandFactory.rootStrand();
        StrandExecutor rootExecutor = strandExecutorFactory.createStrandExecutor(rootStrand, treeStructure);

        /*
        merge of
            actual block
            actual state

            PER STRAND!
         */

        if (!treeStructure.isLeaf(treeStructure.rootBlock())) {
            rootExecutor.instruct(STEP_INTO);
        }
    }

    @Deprecated
    public Strand getRootStrand() {
        return strandFactory.rootStrand();
    }

    @Deprecated
    public StrandFactoryImpl getStrandFactory() {
        return strandFactory;
    }

    @Override
    public Flux<MissionState> states() {
        return states;
    }


    private MissionState gatherStates() {
        MissionState.Builder builder = MissionState.builder();
        for (StrandExecutor executor : strandExecutorFactory.allStrandExecutors()) {
            RunState runState = executor.getActualState();
            Block cursor = executor.getActualBlock();
            Optional<Strand> parent = strandFactory.parentOf(executor.getStrand());
            if(parent.isPresent()) {
                builder.add(executor.getStrand(), runState, cursor, parent.get(), executor.getAllowedCommands());
            } else {
                builder.add(executor.getStrand(), runState, cursor, executor.getAllowedCommands());
            }
        }
        return builder.build();
    }

    @Override
    public void instruct(Strand strand, StrandCommand command) {
        StrandExecutor executor = strandExecutorFactory.getStrandExecutorFor(strand);
        executor.instruct(command);
    }


}
