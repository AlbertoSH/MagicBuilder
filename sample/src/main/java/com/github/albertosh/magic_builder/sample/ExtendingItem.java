package com.github.albertosh.magic_builder.sample;

public class ExtendingItem extends SimpleItem {

    private String extended;

    public String getExtended() {
        return extended;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExtendingItem that = (ExtendingItem) o;

        return getExtended() != null ? getExtended().equals(that.getExtended()) : that.getExtended() == null;

    }

    @Override
    public int hashCode() {
        return getExtended() != null ? getExtended().hashCode() : 0;
    }
}
