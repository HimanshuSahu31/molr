package io.molr.mole.core.tree;

import io.molr.commons.domain.Block;
import io.molr.commons.domain.StrandCommand;
import io.molr.mole.core.runnable.RunnableLeafsMission;
import io.molr.mole.core.runnable.lang.RunnableLeafsMissionSupport;
import io.molr.mole.core.testing.strand.AbstractSingleMissionStrandExecutorTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static io.molr.commons.domain.RunState.FINISHED;
import static io.molr.commons.domain.RunState.PAUSED;
import static io.molr.commons.domain.RunState.RUNNING;
import static io.molr.commons.domain.StrandCommand.PAUSE;
import static io.molr.commons.domain.StrandCommand.RESUME;
import static io.molr.commons.domain.StrandCommand.STEP_INTO;
import static io.molr.commons.domain.StrandCommand.STEP_OVER;

public class ConcurrentStrandExecutorChildrenExecutionTest extends AbstractSingleMissionStrandExecutorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentStrandExecutorStepOverParallelTest.class);

    private Block parallelBlock;
    private Block lastBlock;
    private Block blockA2;
    private Block blockB2;

    private CountDownLatch latchA1Start;
    private CountDownLatch latchB1Start;
    private CountDownLatch latchB2Start;
    private CountDownLatch latchA1End;
    private CountDownLatch latchB1End;
    private CountDownLatch latchB2End;

    @Override
    protected RunnableLeafsMission mission() {
        return new RunnableLeafsMissionSupport() {
            {
                sequential("step-over", root -> {
                    parallelBlock = root.parallel("parallel", b -> {
                        b.sequential("sequential branch A", bA -> {
                            bA.run("A.1", () -> {
                                unlatch(latchA1Start);
                                await(latchA1End);
                            });
                            blockA2 = bA.run("A.2", () -> {
                            });
                        });
                        b.sequential("sequential branch B", bB -> {
                            bB.run("B.1", () -> {
                                unlatch(latchB1Start);
                                await(latchB1End);
                            });
                            blockB2 = bB.run("B.2", () -> {
                                unlatch(latchB2Start);
                                await(latchB2End);
                            });
                        });
                    });
                    lastBlock = root.run(log("After"));
                });
            }
        }.build();
    }

    @Before
    public void setUp() {
        latchA1Start = new CountDownLatch(1);
        latchB1Start = new CountDownLatch(1);
        latchB2Start = new CountDownLatch(1);
        latchA1End = new CountDownLatch(1);
        latchB1End = new CountDownLatch(1);
        latchB2End = new CountDownLatch(1);
    }

    @Test
    public void testStepOverLastChildrenAfterStepIntoPausesAtParentSibling() throws InterruptedException {
        moveRootStrandTo(parallelBlock);
        instructRootStrandSync(STEP_INTO);
        rootStrandChildren().forEach(se -> waitUntilStrandStateIs(se, PAUSED));
        LOGGER.info("Children paused");
        unlatch(latchA1End, latchB1End, latchB2End);
        rootStrandChildren().forEach(se -> instructSync(se, STEP_OVER));

        waitUntilRootStrandBlockIs(lastBlock);
        waitUntilRootStrandStateIs(PAUSED);

        assertThatRootStrandBlock().isEqualTo(lastBlock);
        assertThatRootStrandState().isNotEqualTo(FINISHED);
    }

    @Test
    public void testChildrenFinishWhileParentIsPauseShouldFinishParent() {
        moveRootStrandTo(parallelBlock);
        instructRootStrandSync(StrandCommand.RESUME);

        await(latchA1Start, latchB1Start);
        instructRootStrandSync(PAUSE);
        unlatch(latchA1End, latchB1End, latchB2End);

        waitUntilRootStrandStateIs(PAUSED);
        rootStrandChildren().forEach(se -> waitUntilStrandStateIs(se, PAUSED));

        rootStrandChildren().forEach(se -> se.instruct(StrandCommand.RESUME));
        rootStrandChildren().forEach(this::waitUntilStrandIsFinished);

        waitUntilRootStrandBlockIs(lastBlock);
        assertThatRootStrandBlock().isEqualTo(lastBlock);
        assertThatRootStrandState().isNotEqualTo(FINISHED);
    }

    @Test
    public void testChildrenFinishWhileParentIsPauseShouldMoveNextOnParent() {
        moveRootStrandTo(parallelBlock);
        instructRootStrandSync(StrandCommand.STEP_OVER);

        await(latchA1Start, latchB1Start);
        instructRootStrandSync(PAUSE);
        unlatch(latchA1End, latchB1End, latchB2End);

        waitUntilRootStrandStateIs(PAUSED);
        rootStrandChildren().forEach(se -> waitUntilStrandStateIs(se, PAUSED));

        rootStrandChildren().forEach(se -> se.instruct(StrandCommand.RESUME));
        rootStrandChildren().forEach(this::waitUntilStrandIsFinished);

        waitUntilRootStrandBlockIs(lastBlock);
    }

    @Test
    public void testIfAllChildrenArePausedParentShouldPause() {
        moveRootStrandTo(parallelBlock);
        instructRootStrandSync(StrandCommand.RESUME);

        await(latchA1Start, latchB1Start);
        rootStrandChildren().forEach(se -> instructAsync(se, PAUSE));
        unlatch(latchA1End, latchB1End, latchB2End);

        rootStrandChildren().forEach(se -> waitUntilStrandStateIs(se, PAUSED));
        waitUntilRootStrandStateIs(PAUSED);

        rootStrandChildren().forEach(se -> assertThatStateOf(se).isEqualTo(PAUSED));
    }

    @Test
    public void testIfAtLeastOneChildrenIsRunningParentIsWaiting() {
        /* WAITING_FOR_CHILDREN == RUNNING RunState */
        moveRootStrandTo(parallelBlock);
        instructRootStrandSync(StrandCommand.RESUME);

        await(latchA1Start, latchB1Start);
        instructRootStrandSync(PAUSE);
        unlatch(latchA1End, latchB1End);

        waitUntilRootStrandStateIs(PAUSED);

        /* at this point the children must be at these blocks*/
        StrandExecutor strandA = rootStrandChildren().stream()
                .filter(se -> se.getActualBlock().equals(blockA2)).findFirst().get();

        StrandExecutor strandB = rootStrandChildren().stream()
                .filter(se -> se.getActualBlock().equals(blockB2)).findFirst().get();

        assertThatStateOf(strandA).isEqualTo(PAUSED);
        assertThatStateOf(strandB).isEqualTo(PAUSED);

        instructAsync(strandB, RESUME);
        await(latchB2Start);

        waitUntilRootStrandStateIs(RUNNING);

        assertThatStateOf(strandA).isEqualTo(PAUSED);
        assertThatStateOf(strandB).isEqualTo(RUNNING);
        assertThatRootStrandState().isEqualTo(RUNNING);
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }
}
