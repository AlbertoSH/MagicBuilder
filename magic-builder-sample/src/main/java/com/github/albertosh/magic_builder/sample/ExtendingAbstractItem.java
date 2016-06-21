package com.github.albertosh.magic_builder.sample;

import com.github.albertosh.magic_builder.MagicBuilder;

@MagicBuilder
public class ExtendingAbstractItem extends AbstractItem {

    private String c;
    private String d;

    public String getC() {
        return c;
    }

    public String getD() {
        return d;
    }
}
