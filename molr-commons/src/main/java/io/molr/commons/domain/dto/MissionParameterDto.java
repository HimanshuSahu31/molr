package io.molr.commons.domain.dto;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import io.molr.commons.domain.MissionParameter;
import io.molr.commons.domain.Placeholder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.molr.commons.util.Exceptions.illegalArgumentException;
import static io.molr.commons.util.Exceptions.illegalStateException;

public class MissionParameterDto<T> {

    private static final BiMap<Class<?>, String> TYPE_NAMES = ImmutableBiMap.of(
            String.class, "string",
            Double.class, "double",
            Integer.class, "integer",
            Boolean.class, "boolean"
    );
    private static final Map<Class<?>, Function<String, Placeholder<?>>> TYPE_CREATORS = ImmutableMap.of(
            String.class, Placeholder::aString,
            Double.class, Placeholder::aDouble,
            Integer.class, Placeholder::anInteger,
            Boolean.class, Placeholder::aBoolean
    );

    public final String name;
    public final String type;
    public final boolean required;
    public final T defaultValue;

    public MissionParameterDto() {
        this.name = null;
        this.type = null;
        this.required = false;
        this.defaultValue = null;
    }

    public MissionParameterDto(String name, String type, boolean required, T defaultValue) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public static final <T> MissionParameterDto from(MissionParameter<T> parameter) {
        Placeholder<T> placeholder = parameter.placeholder();
        return new MissionParameterDto<>(placeholder.name(), typeStringFrom(placeholder.type()), parameter.isRequired(), parameter.defaultValue());
    }

    public MissionParameter<T> toMissionParameter() {
        if (this.required) {
            return MissionParameter.required(placeholder()).withDefault(defaultValue);
        } else {
            return MissionParameter.optional(placeholder()).withDefault(defaultValue);
        }
    }

    private Placeholder<T> placeholder() {
        Class<?> typeClass = TYPE_NAMES.inverse().get(type);
        if (typeClass == null) {
            throw illegalStateException("Type '{}' cannot be converted into a valid java type.", type);
        }
        Function<String, Placeholder<?>> typeSupplier = TYPE_CREATORS.get(typeClass);
        if (typeSupplier == null) {
            throw illegalStateException("Type '{}' cannot be converted into a valid java type.", type);
        }
        return (Placeholder<T>) typeSupplier.apply(name);
    }

    private static final String typeStringFrom(Class<?> type) {
        String typeName = TYPE_NAMES.get(type);
        if (typeName != null) {
            return typeName;
        }
        throw illegalArgumentException("Type '{}' cannot be mapped to a valid json value.", type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissionParameterDto that = (MissionParameterDto) o;
        return required == that.required &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, required, defaultValue);
    }

    @Override
    public String toString() {
        return "MissionParameterDto{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", required=" + required +
                ", defaultValue=" + defaultValue +
                '}';
    }
}
