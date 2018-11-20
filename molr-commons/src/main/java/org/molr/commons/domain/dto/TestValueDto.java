package org.molr.commons.domain.dto;

import java.util.Objects;

public class TestValueDto {

    public final String text;

    public TestValueDto(String text) {
        this.text = text;
    }

    public TestValueDto() {
        this.text = null;
    }

    @Override
    public String toString() {
        return "TestValueDto{" +
                "text='" + text + '\'' +
                '}';
    }

}
