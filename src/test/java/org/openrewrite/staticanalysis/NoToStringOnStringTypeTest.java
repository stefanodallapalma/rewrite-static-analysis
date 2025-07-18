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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoToStringOnStringTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoToStringOnStringType());
    }

    @DocumentExample
    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void toStringOnString() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String method() {
                      return "hello".toString();
                  }
              }
              """,
            """
              class Test {
                  static String method() {
                      return "hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeOnObject() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String method(Object obj) {
                      return obj.toString();
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void toStringOnStringVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String method(String str) {
                      return str.toString();
                  }
              }
              """,
            """
              class Test {
                  static String method(String str) {
                      return str;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void toStringOnMethodInvocation() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static void method1() {
                      String str = method2().toString();
                  }

                  static String method2() {
                      return "";
                  }
              }
              """,
            """
              class Test {
                  static void method1() {
                      String str = method2();
                  }

                  static String method2() {
                      return "";
                  }
              }
              """
          )
        );
    }
}
