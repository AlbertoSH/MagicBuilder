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


        ExtendingItem copyExtended = new ExtendingItem_MagicBuilder()
                .fromPrototype(extendingItem)
                .build();

        assertThat(copyExtended, is(equalTo(extendingItem)));

        ExtendingItem_MagicBuilder copyFromSimpleBuilder = (ExtendingItem_MagicBuilder) new ExtendingItem_MagicBuilder()
                .fromPrototype(simpleItem);


        ExtendingItem copyFromSimple =
                copyFromSimpleBuilder
                        .extended("ext")
                        .build();

        assertThat(copyFromSimple.getExtended(), is("ext"));
        assertThat(copyFromSimple.getSomeValue(), is("simple"));
        assertThat(copyFromSimple.getSomeOtherInt(), is(0));
    }

}
