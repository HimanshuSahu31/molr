package org.molr.mole.core.runnable.lang;

import org.molr.commons.domain.Block;
import org.molr.mole.core.runnable.RunnableLeafsMission;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class Branch {

    private final RunnableLeafsMission.Builder builder;
    private final Block parent;

    private Branch(RunnableLeafsMission.Builder builder, Block parent) {
        this.builder = builder;
        this.parent = parent;
    }

    static Branch withParent(RunnableLeafsMission.Builder builder, Block parent) {
        return new Branch(builder, parent);
    }

    public Block run(String name, Runnable runnable) {
        return builder.leafChild(parent, name, runnable);
    }

    public Block sequential(String name, Consumer<Branch> branchDefiner) {
        Block node = builder.sequentialChild(parent, name);
        branchDefiner.accept(Branch.withParent(builder, node));
        return node;
    }

    public Block parallel(String name, Consumer<Branch> branchDefiner) {
        Block node = builder.parallelChild(parent, name);
        branchDefiner.accept(Branch.withParent(builder, node));
        return node;
    }

    public Block run(Task task) {
        return run(task.name, task.runnable);
    }

    public Block println(Object object) {
        return run("println(\"" + object + "\");", () -> System.out.println(object));
    }

    public Block sleep(long time, TimeUnit unit) {
        return run("Sleep " + time + " " + unit, () -> {
            try {
                unit.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public static class Task {
        private final String name;
        private final Runnable runnable;

        public Task(String name, Runnable runnable) {
            this.name = requireNonNull(name, "name must not be null.");
            this.runnable = requireNonNull(runnable, "runnable must not be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Task task = (Task) o;
            return Objects.equals(name, task.name) &&
                    Objects.equals(runnable, task.runnable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, runnable);
        }

        @Override
        public String toString() {
            return "Task{" +
                    "name='" + name + '\'' +
                    ", runnable=" + runnable +
                    '}';
        }
    }
}
