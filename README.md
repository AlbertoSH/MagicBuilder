# MagicBuilder

[ ![Download](https://api.bintray.com/packages/albertosh/maven/magic-builder/images/download.svg) ](https://bintray.com/albertosh/maven/magic-builder/_latestVersion)

Compile time code generator for Builder pattern

## Overview

**MagicBuilder** analyzes your models and generates a Builder for them

---

## Features

* Compile time generation
* Inheritance support
* Abstract classes support

--- 

## Installation

###Gradle

Add the following to your `build.gradle` (check last version)

    // Apt plugin
    buildscript {
        repositories {
            maven {
                url "https://plugins.gradle.org/m2/"
            }
        }
        dependencies {
            classpath "net.ltgt.gradle:gradle-apt-plugin:0.6"
        }
    }
    apply plugin: "net.ltgt.apt"

    repositories {
        jcenter()
    }
    
    dependencies{
        // MagicBuilder
        compile 'com.github.albertosh:magic-builder:1.x.x'
        apt 'com.github.albertosh:magic-builder-compiler:1.x.x'
        compileOnly 'com.github.albertosh:magic-builder-compiler:1.x.x'
    }
    
Currently the lib is only at jcenter. In the following days I will upload it to Maven Central

--- 

## How to start

MagicBuilder is based in static code analysis with annotation support. These are the steps to use it:

1. Annotate your models with `@MagicBuilder`
2. And... you're done XD

**MagicBuilder** generates everything you need. Let's take a look to what has done to a very simple class:

```java
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
```

Given that class the builder generated is the following:

```java
public class SimpleItem_MagicBuilder<T extends SimpleItem> implements IMagicBuilder<T> {
  private String someValue;

  private Integer someOtherInt;

  public SimpleItem_MagicBuilder() {
  }

  public String getSomeValue() {
    return someValue;
  }

  public SimpleItem_MagicBuilder<T> someValue(String someValue) {
    this.someValue = someValue;
    return this;
  }

  public Integer getSomeOtherInt() {
    return someOtherInt;
  }

  public SimpleItem_MagicBuilder<T> someOtherInt(Integer someOtherInt) {
    this.someOtherInt = someOtherInt;
    return this;
  }

  @OverridingMethodsMustInvokeSuper
  public SimpleItem_MagicBuilder<T> fromPrototype(SimpleItem prototype) {
    this.someValue = prototype.getSomeValue();
    this.someOtherInt = prototype.getSomeOtherInt();
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T build() {
    return (T) new SimpleItem(this);
  }
}
```

---

## How it works

The first part is *easy*. Just use the APT (Annotation Processor Tool) to analyze your models annotated with `@MagicBuilder` and generate another `.java`  file. 

**But, dude, you're creating a constructor that I haven't defined! That `new SimpleItem(this)` will cause the compilation to fail!**

Well... Yes... and no. You're right when you say that the compilation *should* fail. But it's not failing. Why? Well, try to decompile the `SimpleItem.class`. You'll see something similar to this:

```java
public class SimpleItem {
    private String someValue;
    private Integer someOtherInt;

    public SimpleItem() {
    }

    public String getSomeValue() {
        return this.someValue;
    }

    public Integer getSomeOtherInt() {
        return this.someOtherInt;
    }

    SimpleItem(SimpleItem_MagicBuilder builder) {
        this.someValue = builder.getSomeValue();
        this.someOtherInt = builder.getSomeOtherInt();
    }
}

```

**See that `SimpleItem(SimpleItem_MagicBuilder builder)` constructor?**
![](https://media.giphy.com/media/ujUdrdpX7Ok5W/giphy.gif)

**MagicBuilder** injects the constructor code into your models so the compilation is successful and you don't have to worry about adding that code by yourself (with the consequent problem of having to update it when you change your model)

**Wait, are you saying that you're touching my code?** 

Maybe... More or less... Yes

**Are you doing anything more besides that little hack?** 

Nope, I could but I'm not

**Why should I belive you?** 

You believe that none of the libraries you use harms you. Why would this?

---

##License
 
    The MIT License
    
    Copyright (c) 2016 Alberto Sanz
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    