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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"ClassInitializerMayBeStatic", "StatementWithEmptyBody", "ConstantConditions", "SequencedCollectionMethodCanBeUsed"})
class EqualsAvoidsNullTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EqualsAvoidsNull());
    }

    @DocumentExample
    @Test
    void invertConditional() {
        rewriteRun(
          //language=java
          java(
            """
              public class A {
                  {
                      String s = null;
                      if(s.equals("test")) {}
                      if(s.equalsIgnoreCase("test")) {}
                      System.out.println(s.contentEquals("test"));
                  }
              }
              """,
            """
              public class A {
                  {
                      String s = null;
                      if("test".equals(s)) {}
                      if("test".equalsIgnoreCase(s)) {}
                      System.out.println("test".contentEquals(s));
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveCharAlone() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class A {
                  boolean compareToPrimitiveTypes(List<Object> objects) {
                      return objects.get(0).equals(1) || objects.get(0).equals('a');
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnnecessaryNullCheck() {
        rewriteRun(
          //language=java
          java(
            """
              public class A {
                  {
                      String s = null;
                      if(s != null && s.equals("test")) {}
                      if(null != s && s.equals("test")) {}
                  }
              }
              """,
            """
              public class A {
                  {
                      String s = null;
                      if("test".equals(s)) {}
                      if("test".equals(s)) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void nullLiteral() {
        rewriteRun(
          //language=java
          java(
                """
              public class A {
                    void foo(String s) {
                        if(s.equals(null)) {
                        }
                    }
                }
              """,
            """
              public class A {
                    void foo(String s) {
                        if(s == null) {
                        }
                    }
                }
              """
          )
        );
    }

    @Test
    void ObjectEquals() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void foo(Object s) {
                      if (s.equals("null")) {
                      }
                  }
              }
              """,
            """
              class A {
                  void foo(Object s) {
                      if ("null".equals(s)) {
                      }
                  }
              }
              """
          )
        );
    }

    @Nested
    @SuppressWarnings("ResultOfMethodCallIgnored")
    class ReplaceConstantMethodArg {

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/pull/398")
        @Test
        void one() {
            rewriteRun(
              // language=java
              java(
                """
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private boolean isFoo(String foo) {
                          return foo.contentEquals(Constants.FOO);
                      }
                  }
                  """,
                """
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private boolean isFoo(String foo) {
                          return Constants.FOO.contentEquals(foo);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void chainedMethodCalls() {
            // language=java
            rewriteRun(
              java(
                """
                  package c;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  """
              ),
              java(
                """
                  class Foo {
                      String getFooType() {
                          return "FOO";
                      }
                      Foo getFOO() {
                          return this;
                      }
                  }
                  """
              ),
              java(
                """
                  import static c.Constants.FOO;
                  class A {
                      boolean filterFoo(final Foo foo) {
                          return foo.getFOO().getFooType().contentEquals(FOO);
                      }
                  }
                  """,
                """
                  import static c.Constants.FOO;
                  class A {
                      boolean filterFoo(final Foo foo) {
                          return FOO.contentEquals(foo.getFOO().getFooType());
                      }
                  }
                  """
              )
            );
        }

        @Test
        void staticImport() {
            rewriteRun(
              // language=java
              java(
                """
                  package c;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  """
              ),
              // language=java
              java(
                """
                  import static c.Constants.FOO;
                  class A {
                      private boolean isFoo(String foo) {
                          return foo.contentEquals(FOO);
                      }
                  }
                  """,
                """
                  import static c.Constants.FOO;
                  class A {
                      private boolean isFoo(String foo) {
                          return FOO.contentEquals(foo);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void multiple() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private boolean isFoo(String foo, String bar) {
                          return foo.equals(Constants.FOO)
                              || bar.contentEquals(Constants.FOO);
                      }
                  }
                  """,
                """
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private boolean isFoo(String foo, String bar) {
                          return Constants.FOO.equals(foo)
                              || Constants.FOO.contentEquals(bar);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void generics() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private <T> void r(T e) {
                          e.toString().equals(Constants.FOO);
                      }
                  }
                  """,
                """
                  import java.util.List;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private <T> void r(T e) {
                          Constants.FOO.equals(e.toString());
                      }
                  }
                  """
              )
            );
        }

        @Test
        void lambda() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private void isFoo(List<Object> list) {
                          list.stream().filter(c -> c.toString().contentEquals(Constants.FOO));
                      }
                  }
                  """,
                """
                  import java.util.List;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class A {
                      private void isFoo(List<Object> list) {
                          list.stream().filter(c -> Constants.FOO.contentEquals(c.toString()));
                      }
                  }
                  """
              )
            );
        }

        @Disabled("Not yet supported")
        @Test
        void lambdaGenerics() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class C {
                      boolean c(String k) {
                          return true;
                      }

                      Object get(String k) {
                          return null;
                      }

                      void r(String k, String v) {
                      }
                  }
                  class A {
                      private <T extends C> void rr(List<String> f, T e) {
                          f.stream()
                              .filter(fn -> e.c(fn))
                              .forEach(fn -> e.get(fn).equals(Constants.FOO));
                      }
                  }
                  """,
                """
                  import java.util.List;
                  public class Constants {
                      public static final String FOO = "FOO";
                  }
                  class C {
                      boolean c(String k) {
                          return true;
                      }

                      Object get(String k) {
                          return null;
                      }

                      void r(String k, String v) {
                      }
                  }
                  class A {
                      private <T extends C> void rr(List<String> f, T e) {
                          f.stream()
                              .filter(fn -> e.c(fn))
                              .forEach(fn -> Constants.FOO.equals(e.get(fn)));
                      }
                  }
                  """
              )
            );
        }

        @Test
        void nonStaticNonFinalNoChange() {
            rewriteRun(
              // language=java
              java(
                """
                  public class Constants {
                      public final String FOO = "FOO";
                      public static String BAR = "BAR";
                  }
                  class A {
                      private boolean isFoo(String foo) {
                          return foo.contentEquals(new Constants().FOO);
                      }
                      private boolean isBar(String bar) {
                          return bar.contentEquals(Constants.BAR);
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/434")
        @Test
        void missingWhitespace() {
            rewriteRun(
              // language=java
              java(
                """
                  class A {
                      private static final String FOO = "FOO";

                      boolean withParentExpression(String foo) {
                          return foo != null && foo.equals(FOO);
                      }
                  }
                  """,
                """
                  class A {
                      private static final String FOO = "FOO";

                      boolean withParentExpression(String foo) {
                          return FOO.equals(foo);
                      }
                  }
                  """
              )
            );
        }
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/442")
    @Test
    void retainCompareToAsToNotChangeOrder() {
        rewriteRun(
          //language=java
          java(
            """
              public class A {
                  {
                      String s = null;
                      System.out.println(s.compareTo("test"));
                      System.out.println(s.compareToIgnoreCase("test"));
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/472")
    @Nested
    class EqualsAvoidsNullNonIdempotent {
        @Test
        void literalAndConstant() {
            rewriteRun(
              spec -> spec.recipe(new EqualsAvoidsNull()),
              // language=java
              java(
                """
                  public class Foo {
                      private static final String FOO = "";
                      public void foo() {
                          FOO.equals("");
                          "".equals(FOO);
                      }
                  }
                  """,
                """
                  public class Foo {
                      private static final String FOO = "";
                      public void foo() {
                          "".equals(FOO);
                          "".equals(FOO);
                      }
                  }
                  """
              ));
        }

        @Test
        void rawOnRaw() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Foo {
                      public void bar() {
                          "FOO".equals("BAR");
                          "FOO".equalsIgnoreCase("BAR");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void referenceOnReference() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Foo {
                      private static final String FOO = null;
                      private static final String BAR = null;
                      public void bar() {
                          BAR.equals(FOO);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rawOverReference() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Foo {
                      private static final String FOO = null;
                      public void bar(String _null) {
                          String _null2 = null;
                          FOO.equals("RAW");
                          _null.equals("RAW");
                          _null2.equals("RAW");
                      }
                  }
                  """
                ,
                    """
                  public class Foo {
                      private static final String FOO = null;
                      public void bar(String _null) {
                          String _null2 = null;
                          "RAW".equals(FOO);
                          "RAW".equals(_null);
                          "RAW".equals(_null2);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rawOverLocalReference() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Foo {
                      private static final String FOO = null;
                      public void bar(String _null) {
                          String _null1 = null;
                          String _null2 = null;
                          _null.equals(FOO);
                          _null2.equals(FOO);
                          _null.equals(_null);
                          _null2.equals(_null2);
                          _null1.equals(_null2);
                      }
                  }
                  """
                ,
                    """
                  public class Foo {
                      private static final String FOO = null;
                      public void bar(String _null) {
                          String _null1 = null;
                          String _null2 = null;
                          FOO.equals(_null);
                          FOO.equals(_null2);
                          _null.equals(_null);
                          _null2.equals(_null2);
                          _null1.equals(_null2);
                      }
                  }
                  """
              )
            );
        }
    }
}
