package com.github.albertosh.magic_builder.sample;

import com.github.albertosh.magic_builder.MagicBuilder;

@MagicBuilder
public class SimpleItem {

    private String someValue;
    private Integer someOtherInt;

    public String getSomeValue() {
        return someValue;
    }

    public Integer getSomeOtherInt() {
        return someOtherInt;
    }

}
