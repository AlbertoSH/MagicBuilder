package com.github.albertosh.magic_builder;

public interface IMagicBuilder<T> {

    public <B extends T> B build();

}
