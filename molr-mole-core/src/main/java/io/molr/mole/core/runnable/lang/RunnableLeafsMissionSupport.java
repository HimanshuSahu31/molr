package io.molr.mole.core.runnable.lang;

import com.google.common.collect.ImmutableSet;
import io.molr.commons.domain.MissionParameter;
import io.molr.commons.domain.MissionParameterDescription;
import io.molr.commons.domain.Placeholder;
import io.molr.mole.core.runnable.RunnableLeafsMission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * An abstract class which is intended to describe a tree of runnables, which can be used as simple test case for
 * parallel tree execution.
 */
public abstract class RunnableLeafsMissionSupport {

    private final static Logger LOGGER = LoggerFactory.getLogger(RunnableLeafsMissionSupport.class);

    private RunnableLeafsMission.Builder builder;
    private ImmutableSet.Builder<MissionParameter<?>> parameterBuilder = ImmutableSet.builder();

    protected void sequential(String newName, Consumer<Branch> branchConsumer) {
        root(newName, branchConsumer, RunnableLeafsMission::sequentialRoot);
    }

    protected void parallel(String newName, Consumer<Branch> branchConsumer) {
        root(newName, branchConsumer, RunnableLeafsMission::parallelRoot);
    }

    private void root(String newName, Consumer<Branch> branchConsumer, Function<String, RunnableLeafsMission.Builder> builderFactory) {
        if (this.builder != null) {
            throw new IllegalStateException("Root can only be defined once!");
        }

        requireNonNull(newName, "name must not be null.");
        requireNonNull(branchConsumer, "branchConsumer must not be null.");

        this.builder = builderFactory.apply(newName);
        Branch rootBranch = Branch.withParent(builder, builder.root());
        branchConsumer.accept(rootBranch);
    }

    protected <T> Placeholder<T> mandatory(Placeholder<T> placeholder) {
        this.parameterBuilder.add(MissionParameter.required(placeholder));
        return placeholder;
    }

    protected <T> Placeholder<T> mandatory(Placeholder<T> placeholder, T defaultValue) {
        this.parameterBuilder.add(MissionParameter.required(placeholder).withDefault(defaultValue));
        return placeholder;
    }

    protected <T> Placeholder<T> optional(Placeholder<T> placeholder) {
        this.parameterBuilder.add(MissionParameter.optional(placeholder));
        return placeholder;
    }

    protected <T> Placeholder<T> optional(Placeholder<T> placeholder, T defaultValue) {
        this.parameterBuilder.add(MissionParameter.optional(placeholder).withDefault(defaultValue));
        return placeholder;
    }

    public RunnableLeafsMission build() {
        MissionParameterDescription parameterDescription = new MissionParameterDescription(parameterBuilder.build());
        return builder.build(parameterDescription);
    }


}
