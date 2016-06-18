package com.github.albertosh.magic_builder.sample;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class Main {

    public static void main(String[] args) {
        SimpleItem simpleItem = new SimpleItem_MagicBuilder()
                .someValue("simple")
                .someOtherInt(0)
                .build();

        assertThat(simpleItem.getSomeValue(), is("simple"));
        assertThat(simpleItem.getSomeOtherInt(), is(0));

        // Casting to builder doesn't give headaches
        ExtendingItem_MagicBuilder extendingItemBuilder = (ExtendingItem_MagicBuilder) new ExtendingItem_MagicBuilder()
                .extended("another value")
                .someValue("extending")
                .someOtherInt(1);
        ExtendingItem extendingItem = extendingItemBuilder.build();

        /*
        Casting built objects WILL cause problems because the constructor of the father item will be called

        ExtendingItem extendingItem = (ExtendingItem) new ExtendingItem_MagicBuilder()
                .extended("another value")
                .someValue("extending")
                .someOtherInt(1)
                .build();

        That code will invoke SimpleItem's constructor so 'extended' value will be lost
        BE CAREFUL WITH THAT!!!
        */

        assertThat(extendingItem.getExtended(), is("another value"));
        assertThat(extendingItem.getSomeValue(), is("extending"));
        assertThat(extendingItem.getSomeOtherInt(), is(1));


        ExtendingItem copy = new ExtendingItem_MagicBuilder()
                .fromPrototype(extendingItem)
                .build();

        assertThat(copy, is(equalTo(extendingItem)));

        ExtendingItem modifiedCopy = new ExtendingItem_MagicBuilder()
                .fromPrototype(extendingItem)
                .extended("ext")
                .build();

        assertThat(modifiedCopy.getExtended(), is("ext"));
        assertThat(modifiedCopy.getSomeValue(), is("extending"));
        assertThat(modifiedCopy.getSomeOtherInt(), is(1));


        ExtendingAbstractItem_MagicBuilder extendingAbstractItemBuilder = (ExtendingAbstractItem_MagicBuilder) new ExtendingAbstractItem_MagicBuilder()
                .c("cc")
                .d("dd")
                .a("aa")
                .b("bb");
        ExtendingAbstractItem extendingAbstractItem = extendingAbstractItemBuilder.build();

        assertThat(extendingAbstractItem.getA(), is("aa"));
        assertThat(extendingAbstractItem.getB(), is("bb"));
        assertThat(extendingAbstractItem.getC(), is("cc"));
        assertThat(extendingAbstractItem.getD(), is("dd"));
        
    }

}
