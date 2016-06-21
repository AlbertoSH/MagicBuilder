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

















    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleItem that = (SimpleItem) o;

        if (getSomeValue() != null ? !getSomeValue().equals(that.getSomeValue()) : that.getSomeValue() != null)
            return false;
        return getSomeOtherInt() != null ? getSomeOtherInt().equals(that.getSomeOtherInt()) : that.getSomeOtherInt() == null;

    }

    @Override
    public int hashCode() {
        int result = getSomeValue() != null ? getSomeValue().hashCode() : 0;
        result = 31 * result + (getSomeOtherInt() != null ? getSomeOtherInt().hashCode() : 0);
        return result;
    }
}
