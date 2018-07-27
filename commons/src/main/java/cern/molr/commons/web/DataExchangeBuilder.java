package cern.molr.commons.web;

import cern.molr.commons.api.type.trye.Failure;
import cern.molr.commons.api.type.trye.Success;
import cern.molr.commons.api.type.trye.Try;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Function;

/**
 * A data exchange builder between the client and the server. It builds a publisher of string messages which is
 * generated from one element received from a Flux of strings
 *
 * @author yassine-kr
 */
public class DataExchangeBuilder<Input, Output> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExchangeBuilder.class);

    /**
     * The mapper used for serializing output and deserializing input
     */
    private ObjectMapper mapper;
    private Class<Input> inputType;
    private Flux<String> preInput;
    private ThrowingFunction<Input, Publisher<Output>> generator;
    private Function<Throwable, Output> generatorExceptionHandler;

    public DataExchangeBuilder(Class<Input> inputType) {
        this.inputType = inputType;

        mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Set the received Flux of messages
     *
     * @param preInput received flux of messages
     *
     * @return this builder to chain other methods
     */
    public DataExchangeBuilder<Input, Output> setPreInput(Flux<String> preInput) {
        this.preInput = preInput;
        return this;
    }

    /**
     * Set the generator which generates the output flux from the received input data
     *
     * @return this builder to chain other methods
     */
    public DataExchangeBuilder<Input, Output> setGenerator(ThrowingFunction<Input, Publisher<Output>> generator) {
        this.generator = generator;
        return this;
    }

    /**
     * Set the handler of the exception thrown by the generator
     *
     * @param function the returned output will be serialized and wrapped in a publisher of one element
     *
     * @return this builder to chain other methods
     */
    public DataExchangeBuilder<Input, Output> setGeneratorExceptionHandler(Function<Throwable, Output> function) {
        this.generatorExceptionHandler = function;
        return this;
    }

    /**
     * builds the publisher
     *
     * @return the publisher
     */
    public Flux<String> build() {
        return preInput.take(1)
                .map(getDeserializer())
                .concatMap((tryInput) -> tryInput.match(getDeserializationErrorHandler(),
                        getGenerator().andThen((tryFlux) -> tryFlux.match(getGenerationErrorHandler(),
                                (flux) -> flux.map(getSerializer()))
                )));
    }

    /**
     * Returns the function which try to deserialize the string input
     * @return the deserializer function
     */
    private Function<String, Try<Input>> getDeserializer() {
        return data -> {
            try {
                return new Success<>(mapper.readValue(data, inputType));
            } catch (IOException error) {
                return new Failure<>(error);
            }
        };
    }

    /**
     * Returns the handler which is called when there is a deserialization error
     * @return the function which returns a string publisher
     */
    private Function<Throwable, Publisher<String>> getDeserializationErrorHandler() {
        return error -> {
            LOGGER.error("unable to deserialize the input data", error);
            return Mono.empty();
        };
    }

    /**
     * Returns the generator which generates the output publisher from the input
     * @return the function which returns the output publisher
     */
    private Function<Input, Try<Flux<Output>>> getGenerator() {
        return input -> {
            try {
                return new Success<>(Flux.from(generator.apply(input)));
            } catch (Exception error) {
                return new Failure<>(error);
            }
        };
    }

    /**
     * Returns the handler called when there is a problem in output generation
     * @return the function which handles the error
     */
    private Function<Throwable, Publisher<String>> getGenerationErrorHandler() {
        return error -> {
            try {
                LOGGER.error("exception in getting the result stream", error);
                if (generatorExceptionHandler == null) {
                    return Mono.empty();
                }
                return Mono.just(mapper.writeValueAsString(generatorExceptionHandler.apply(error)));
            } catch (JsonProcessingException error1) {
                LOGGER.error("unable to serialize an output data", error1);
                return Mono.empty();
            }
        };
    }

    /**
     * Returns the serialiser which transforms the output to a string
     * @return the function which serialize the output
     */
    private Function<Output, String> getSerializer() {
        return output -> {
            try {
                return mapper.writeValueAsString(output);
            } catch (JsonProcessingException error) {
                LOGGER.error("unable to serialize an output data", error);
                return null;
            }
        };
    }



    /**
     * A function which can throw an exception
     *
     * @param <T> parameter type
     * @param <R> return type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }


}