package com.github.albertosh.magic_builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
        "com.github.albertosh.magic_builder.MagicBuilder"
})
public class MagicBuilderProcessor extends AbstractProcessor {

    private final static String BUILDER_CLASS_SUFIX = "_MagicBuilder";
    //private final static String BUILDER_PACKAGE_SUFIX = ".magicbuilder";
    private final static String BUILDER_PACKAGE_SUFIX = "";
    private Trees trees;
    private TreeMaker make;
    private Names names;
    private JavacElements elements;
    private Symbol symbols;
    private List<TypeElement> pendingClasses;
    private Map<TypeName, ClassName> alreadyGeneratedClasses2Builder;

    public MagicBuilderProcessor() {
        pendingClasses = new ArrayList<>();
        alreadyGeneratedClasses2Builder = new HashMap<>();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment)
                processingEnv).getContext();
        make = TreeMaker.instance(context);
        names = Names.instance(context);
        elements = JavacElements.instance(context);
    }

    private void warning(String message, Element e) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                message,
                e);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(MagicBuilder.class)) {
            pendingClasses.add((TypeElement) elem);
        }

        generateBuilders(pendingClasses);

        return true;
    }

    private void generateBuilders(List<TypeElement> pendingClasses) {
        int i = 0;
        while (!pendingClasses.isEmpty()) {
            TypeElement currentClass = pendingClasses.get(i);

            if (classDoesNotExtendAnything(currentClass) || superBuilderHasBeenAlreadyGenerated(currentClass)) {
                ClassName builderClass = generateBuilderForClass(currentClass);
                injectConstructor(currentClass, builderClass);
                pendingClasses.remove(i);
                alreadyGeneratedClasses2Builder.put(TypeName.get(currentClass.asType()), builderClass);
                int divider = pendingClasses.size();
                if (divider == 0)
                    divider = 1;
                i = i % divider;
            } else {
                // Skip this class. We'll try it again later
                int divider = pendingClasses.size();
                if (divider == 0)
                    divider = 1;
                i = (i + 1) % divider;
            }
        }
    }

    private boolean classExtendsFromSomething(TypeElement currentClass) {
        return !classDoesNotExtendAnything(currentClass);
    }

    private boolean classDoesNotExtendAnything(TypeElement currentClass) {
        TypeMirror superClass = currentClass.getSuperclass();
        return superClass.toString().equals("java.lang.Object");
    }

    private boolean superBuilderHasBeenAlreadyGenerated(TypeElement currentClass) {
        TypeName superType = getSuperType(currentClass);
        return alreadyGeneratedClasses2Builder.keySet().contains(superType);
    }

    private TypeName getSuperType(Element classElement) {
        List<? extends TypeMirror> superClasses = processingEnv.getTypeUtils().directSupertypes(classElement.asType());
        TypeMirror superClass = superClasses.get(0);
        TypeName superType = ClassName.get(superClass);
        return superType;
    }

    private ClassName generateBuilderForClass(TypeElement currentClass) {
        String builderName = currentClass.getSimpleName() + BUILDER_CLASS_SUFIX;
        String packageName = currentClass.getEnclosingElement().toString() + BUILDER_PACKAGE_SUFIX;
        ClassName builderClass = ClassName.bestGuess(packageName + "." + builderName);

        TypeName currentType = TypeName.get(currentClass.asType());

        TypeSpec.Builder builder = TypeSpec.classBuilder(builderName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());

        if (classIsAbstract(currentClass))
            builder.addModifiers(Modifier.ABSTRACT);

        TypeVariableName typeVariableName = TypeVariableName.get("T", TypeName.get(currentClass.asType()));
        builder.addTypeVariable(typeVariableName);
        TypeName currentBuilderType = ParameterizedTypeName.get(builderClass, typeVariableName);

        addSuperClass(builder, currentClass, typeVariableName);

        MethodSpec.Builder fromPrototypeBuilder = MethodSpec.methodBuilder("fromPrototype")
                .addAnnotation(OverridingMethodsMustInvokeSuper.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(currentBuilderType);


        fromPrototypeBuilder.addParameter(typeVariableName, "prototype");


        if (classExtendsFromSomething(currentClass)) {
            fromPrototypeBuilder
                    .addAnnotation(Override.class)
                    .addStatement("super.fromPrototype(prototype)");
        }

        addFields(currentClass, builder, fromPrototypeBuilder, currentBuilderType);

        fromPrototypeBuilder.addStatement("return this");
        builder.addMethod(fromPrototypeBuilder.build());

        if (classIsNotAbstract(currentClass))
            addBuildMethod(currentClass, builder, typeVariableName);

        TypeSpec builded = builder.build();
        JavaFile javaFile = JavaFile.builder(packageName, builded)
                .build();
        try {
            Filer filer = processingEnv.getFiler();
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builderClass;
    }

    private void addSuperClass(TypeSpec.Builder builder, TypeElement currentClass, TypeName typeVariableName) {
        if (classExtendsFromSomething(currentClass)) {
            TypeName superClassType = getSuperType(currentClass);
            ClassName superBuilderClassName = alreadyGeneratedClasses2Builder.get(superClassType);

            TypeName superType = ParameterizedTypeName.get(superBuilderClassName, typeVariableName);
            builder.superclass(superType);
        }

            ClassName builderImplClassName = ClassName.get(IMagicBuilder.class);
            ParameterizedTypeName parameterizedBuilder = ParameterizedTypeName.get(builderImplClassName, typeVariableName);
            builder.addSuperinterface(parameterizedBuilder);

    }


    private boolean classIsAbstract(TypeElement currentClass) {
        return currentClass.getModifiers().contains(Modifier.ABSTRACT);
    }

    private boolean classIsNotAbstract(TypeElement currentClass) {
        return !classIsAbstract(currentClass);
    }


    private void addFields(Element currentClass, TypeSpec.Builder builder, MethodSpec.Builder fromPrototypeBuilder, TypeName currentBuilderType) {
        List<? extends Element> enclosed = currentClass.getEnclosedElements();
        for (Element field : enclosed) {
            if (field.getKind().isField()) {
                writeSingleAttributeSet(field, builder, fromPrototypeBuilder, currentBuilderType);
            }
        }
    }

    private void writeSingleAttributeSet(Element field, TypeSpec.Builder builder, MethodSpec.Builder fromPrototypeBuilder, TypeName currentBuilderType) {
        String name = field.getSimpleName().toString();
        TypeName type = TypeName.get(field.asType());
        builder.addField(FieldSpec.builder(
                type, name, Modifier.PRIVATE
        ).build());


        builder.addMethod(MethodSpec
                .methodBuilder("get" + name.substring(0, 1).toUpperCase() + name.substring(1))
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return $L", name)
                .build());

        builder.addMethod(MethodSpec
                .methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(currentBuilderType)
                .addParameter(type, name)
                .addStatement("this.$L = $L", name, name)
                .addStatement("return this")
                .build());

        Optional<String> method = methodThatReturnsValue(field);
        if (method.isPresent())
            fromPrototypeBuilder.addStatement("this.$L = prototype.$L", name, method.get());
    }

    protected Optional<String> methodThatReturnsValue(Element field) {
        if (field.getModifiers().contains(Modifier.PUBLIC)) {
            return Optional.of(field.getSimpleName().toString());
        } else {
            StringBuilder transformedName = new StringBuilder(field.getSimpleName().toString());
            char firstChar = transformedName.charAt(0);
            firstChar = (char) (firstChar - 'a' + 'A');
            transformedName.setCharAt(0, firstChar);
            Element enclosingClass = field.getEnclosingElement();
            for (Element method : enclosingClass.getEnclosedElements()) {
                if (method.getKind().equals(ElementKind.METHOD)) {
                    String methodName = method.getSimpleName().toString();
                    if (methodName.equals("is" + transformedName) || methodName.equals("get" + transformedName))
                        return Optional.of(methodName + "()");
                }
            }
            warning("Couldn't find a way to get " + field.getSimpleName().toString() + "!\nThis can be easily fixed with a get method.\nBe careful when you use fromPrototype method", field);
            return Optional.empty();
        }
    }


    private void addBuildMethod(Element currentClass, TypeSpec.Builder builder, TypeVariableName returningType) {
        TypeName elemType = TypeName.get(currentClass.asType());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .returns(returningType)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return (T) new $T(this)", elemType);
        builder.addMethod(methodBuilder.build());
    }


    private void injectConstructor(TypeElement currentClass, ClassName builderClass) {
        JCTree tree = (JCTree) trees.getTree(currentClass);
        TreeTranslator visitor = new ConsturctorVisitor(currentClass, builderClass);
        tree.accept(visitor);
    }

    private class ConsturctorVisitor extends TreeTranslator {

        private final TypeElement currentClass;
        private final ClassName builderClass;

        private ConsturctorVisitor(TypeElement currentClass, ClassName builderClass) {
            this.currentClass = currentClass;
            this.builderClass = builderClass;
        }

        @Override
        public void visitClassDef(JCTree.JCClassDecl classNode) {
            super.visitClassDef(classNode);

            Iterator<JCTree> iterator = classNode.getMembers().iterator();
            List<JCTree> members = new ArrayList<>();
            iterator.forEachRemaining(members::add);
            members.add(getConstructorNode(classNode));
            JCTree[] asArray = members.toArray(new JCTree[members.size()]);
            classNode.defs = com.sun.tools.javac.util.List.from(asArray);
            result = classNode;
        }

        private JCTree getConstructorNode(JCTree.JCClassDecl classNode) {
            JCTree.JCModifiers modifiers = make.Modifiers(0); // Default visibility

            ListBuffer<JCTree.JCStatement> nullChecks = new ListBuffer<JCTree.JCStatement>();
            ListBuffer<JCTree.JCStatement> assigns = new ListBuffer<JCTree.JCStatement>();
            ListBuffer<JCTree.JCVariableDecl> params = new ListBuffer<JCTree.JCVariableDecl>();

            fillParams(params);

            if (classExtendsFromSomething(currentClass))
                addSuperInvocation(assigns);

            fillAssigns(classNode, assigns);

            return make.MethodDef(modifiers, names.init,
                    null, com.sun.tools.javac.util.List.<JCTree.JCTypeParameter>nil(), params.toList(), com.sun.tools.javac.util.List.<JCTree.JCExpression>nil(),
                    make.Block(0L, nullChecks.appendList(assigns).toList()), null);
        }

        private void addSuperInvocation(ListBuffer<JCTree.JCStatement> assigns) {
            JCTree.JCExpression builderParam = make.Ident(names.fromString("builder"));
            JCTree.JCIdent sup = make.Ident(names._super);
            JCTree.JCMethodInvocation invocation = make.Apply(com.sun.tools.javac.util.List.nil(), sup, com.sun.tools.javac.util.List.of(builderParam));
            JCTree.JCExpressionStatement exec = make.Exec(invocation);
            assigns.append(exec);
        }

        private void fillParams(ListBuffer<JCTree.JCVariableDecl> params) {
            JCTree.JCExpression paramClass = null;
            String[] split = builderClass.toString().split("\\.");
            for (String segment : split) {
                if (paramClass == null)
                    paramClass = make.Ident(names.fromString(segment));
                else
                    paramClass = make.Select(paramClass, names.fromString(segment));
            }

            long flags = Flags.PARAMETER;
            JCTree.JCModifiers modifiers = make.Modifiers(flags);
            Name name = names.fromString("builder");
            JCTree.JCVariableDecl param = make.VarDef(modifiers, name, paramClass, null);

            params.add(param);
        }

        private void fillAssigns(JCTree.JCClassDecl classNode, ListBuffer<JCTree.JCStatement> assigns) {
            Iterator<JCTree> iterator = classNode.getMembers().iterator();
            List<JCTree> members = new ArrayList<>();
            iterator.forEachRemaining(jcTree -> {
                members.add(jcTree);
                if (jcTree.getKind().equals(Tree.Kind.VARIABLE)) {
                    JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) jcTree;
                    JCTree.JCFieldAccess thisX = make.Select(make.Ident(names._this), var.getName());

                    JCTree.JCExpression builder = make.Ident(names.fromString("builder"));
                    String nameAsString = var.getName().toString();
                    String methodName = "get" + nameAsString.substring(0, 1).toUpperCase() + nameAsString.substring(1);
                    JCTree.JCExpression method = make.Select(builder, names.fromString(methodName));
                    JCTree.JCMethodInvocation invocation = make.Apply(com.sun.tools.javac.util.List.nil(), method, com.sun.tools.javac.util.List.nil());
                    JCTree.JCExpressionStatement exec = make.Exec(invocation);
                    JCTree.JCExpression assign = make.Assign(thisX, exec.getExpression());
                    assigns.append(make.Exec(assign));
                }
            });
        }
    }
}
