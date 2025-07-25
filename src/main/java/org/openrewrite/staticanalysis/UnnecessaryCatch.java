/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.staticanalysis;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.NewClass;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@EqualsAndHashCode(callSuper = false)
@Value
public class UnnecessaryCatch extends Recipe {

    @Option(displayName = "Include `java.lang.Exception`",
            description = "Whether to include `java.lang.Exception` in the list of checked exceptions to remove. " +
                    "Unlike other checked exceptions, `java.lang.Exception` is also the superclass of unchecked exceptions. " +
                    "So removing `catch(Exception e)` may result in changed runtime behavior in the presence of unchecked exceptions. " +
                    "Default `false`",
            required = false)
    boolean includeJavaLangException;

    @Option(displayName = "Include `java.lang.Throwable`",
            description = "Whether to include `java.lang.Throwable` in the list of exceptions to remove. " +
                    "Unlike other checked exceptions, `java.lang.Throwable` is also the superclass of unchecked exceptions. " +
                    "So removing `catch(Throwable e)` may result in changed runtime behavior in the presence of unchecked exceptions. " +
                    "Default `false`",
            required = false)
    boolean includeJavaLangThrowable;

    @Override
    public String getDisplayName() {
        return "Remove catch for a checked exception if the try block does not throw that exception";
    }

    @Override
    public String getDescription() {
        return "A refactoring operation may result in a checked exception that is no longer thrown from a `try` block. This recipe will find and remove unnecessary catch blocks.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                return b.withStatements(ListUtils.flatMap(b.getStatements(), statement -> {
                    if (statement instanceof J.Try) {
                        // if a try has no catches, no finally, and no resources get rid of it and merge its statements into the current block
                        J.Try aTry = (J.Try) statement;
                        if (aTry.getCatches().isEmpty() && aTry.getResources() == null && aTry.getFinally() == null) {
                            return ListUtils.map(aTry.getBody().getStatements(), tryStat -> autoFormat(tryStat, ctx, getCursor()));
                        }
                    }
                    return statement;
                }));
            }

            @Override
            public J.Try visitTry(J.Try tryable, ExecutionContext ctx) {
                J.Try t = super.visitTry(tryable, ctx);

                List<JavaType> thrownExceptions = new ArrayList<>();
                AtomicBoolean missingTypeInformation = new AtomicBoolean(false);
                //Collect any checked exceptions thrown from the try block.
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public NewClass visitNewClass(NewClass newClass, Integer integer) {
                        JavaType.Method methodType = newClass.getMethodType();
                        if (methodType == null) {
                            //Do not make any changes if there is missing type information.
                            missingTypeInformation.set(true);
                        } else {
                            thrownExceptions.addAll(methodType.getThrownExceptions());
                        }
                        return super.visitNewClass(newClass, integer);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                        JavaType.Method methodType = method.getMethodType();
                        if (methodType == null) {
                            //Do not make any changes if there is missing type information.
                            missingTypeInformation.set(true);
                        } else {
                            thrownExceptions.addAll(methodType.getThrownExceptions());
                        }
                        return super.visitMethodInvocation(method, integer);
                    }
                }.visit(t.getBody(), 0);

                //If there is any missing type information, it is not safe to make any transformations.
                if (missingTypeInformation.get()) {
                    return t;
                }

                //!e.isAssignableTo("java.lang.RuntimeException")
                //For any checked exceptions being caught, if the exception is not thrown, remove the catch block.
                return t.withCatches(ListUtils.map(t.getCatches(), (i, aCatch) -> {
                    JavaType parameterType = aCatch.getParameter().getType();
                    if (parameterType == null || TypeUtils.isAssignableTo("java.lang.RuntimeException", parameterType)) {
                        return aCatch;
                    }
                    if (!includeJavaLangException && TypeUtils.isOfClassType(parameterType, "java.lang.Exception")) {
                        return aCatch;
                    }
                    if (!includeJavaLangThrowable && TypeUtils.isOfClassType(parameterType, "java.lang.Throwable")) {
                        return aCatch;
                    }
                    for (JavaType e : thrownExceptions) {
                        if (TypeUtils.isAssignableTo(e, parameterType)) {
                            return aCatch;
                        }
                    }
                    maybeRemoveImport(TypeUtils.asFullyQualified(parameterType));
                    return null;
                }));
            }
        };
    }
}
