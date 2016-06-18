package com.github.albertosh.magic_builder.sample;

import com.github.albertosh.magic_builder.MagicBuilder;

@MagicBuilder
public abstract class AbstractItem {

    private String a;
    private String b;

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }
}
