package io.molr.mole.core.tree;

import io.molr.commons.domain.Block;
import io.molr.commons.domain.Result;
import io.molr.commons.domain.RunState;
import io.molr.commons.domain.StrandCommand;
import io.molr.mole.core.runnable.RunnableLeafsMission;
import io.molr.mole.core.runnable.lang.RunnableLeafsMissionSupport;
import io.molr.mole.core.testing.strand.AbstractSingleMissionStrandExecutorTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class ConcurrentStrandExecutorStepOverParallelTest extends AbstractSingleMissionStrandExecutorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentStrandExecutorStepOverParallelTest.class);

    private Block parallel;
    private Block parallelA1;
    private Block parallelA2;
    private Block parallelB1;
    private Block parallelB2;

    private CountDownLatch latchA1Start;
    private CountDownLatch latchB1Start;
    private CountDownLatch latchA1End;
    private CountDownLatch latchB1End;
    private CountDownLatch latchA2Start;
    private CountDownLatch latchB2Start;
    private CountDownLatch latchA2End;
    private CountDownLatch latchB2End;

    @Override
    protected RunnableLeafsMission mission() {
        return new RunnableLeafsMissionSupport() {
            {
                sequential("step-over", root -> {
                    parallel = root.parallel("parallel", b -> {
                        b.sequential("sequential branch A", bA -> {
                            parallelA1 = bA.run("A.1", () -> {
                                unlatch(latchA1Start);
                                await(latchA1End);
                            });
                            parallelA2 = bA.run("A.2", () -> {
                                unlatch(latchA2Start);
                                await(latchA2End);
                            });
                        });
                        b.sequential("sequential branch B", bB -> {
                            parallelB1 = bB.run("B.1", () -> {
                                unlatch(latchB1Start);
                                await(latchB1End);
                            });
                            parallelB2 = bB.run("B.2", () -> {
                                unlatch(latchB2Start);
                                await(latchB2End);
                            });
                        });
                    });
                });
            }
        }.build();
    }

    @Before
    public void setUp() {
        latchA1Start = new CountDownLatch(1);
        latchB1Start = new CountDownLatch(1);
        latchA1End = new CountDownLatch(1);
        latchB1End = new CountDownLatch(1);
        latchA2Start = new CountDownLatch(1);
        latchB2Start = new CountDownLatch(1);
        latchA2End = new CountDownLatch(1);
        latchB2End = new CountDownLatch(1);
    }

    @Test
    public void testStepOverTwiceWithParallelStrands() {
        moveRootStrandTo(parallel);
        assertThatRootStrandBlock().isEqualTo(parallel);

        instructRootStrandSync(StrandCommand.STEP_OVER);
        await(latchA1Start, latchB1Start);

        instructRootStrandSync(StrandCommand.STEP_OVER);
        unlatch(latchA1End, latchB1End, latchA2End, latchB2End);

        waitUntilRootStrandStateIs(RunState.FINISHED);

        assertThatResultOf(parallel).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelA1).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelA2).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelB1).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelB2).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void testResumeAfterStepOverWithParallelStrands() {
        moveRootStrandTo(parallel);
        assertThatRootStrandBlock().isEqualTo(parallel);

        instructRootStrandSync(StrandCommand.STEP_OVER);
        await(latchA1Start, latchB1Start);

        instructRootStrandSync(StrandCommand.RESUME);
        unlatch(latchA1End, latchB1End, latchA2End, latchB2End);

        waitUntilRootStrandStateIs(RunState.FINISHED);

        assertThatResultOf(parallel).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelA1).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelA2).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelB1).isEqualTo(Result.SUCCESS);
        assertThatResultOf(parallelB2).isEqualTo(Result.SUCCESS);
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }
}
