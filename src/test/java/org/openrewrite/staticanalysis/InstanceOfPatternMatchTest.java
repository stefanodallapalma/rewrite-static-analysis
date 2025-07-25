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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings({"RedundantCast", "DataFlowIssue", "ConstantValue", "ImplicitArrayToString", "PatternVariableCanBeUsed", "UnnecessaryLocalVariable", "SizeReplaceableByIsEmpty", "rawtypes", "ResultOfMethodCallIgnored", "ArraysAsListWithZeroOrOneArgument", "DuplicateCondition"})
class InstanceOfPatternMatchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InstanceOfPatternMatch())
          .allSources(sourceSpec -> version(sourceSpec, 17));
    }


    @Nested
    class If {
        @Test
        void ifConditionWithoutPattern() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          Object s = 1;
                          if (o instanceof String && ((String) (o)).length() > 0) {
                              if (((String) o).length() > 1) {
                                  System.out.println(o);
                              }
                          }
                      }
                  }
                  """,
                """
                  public class A {
                      void test(Object o) {
                          Object s = 1;
                          if (o instanceof String string && string.length() > 0) {
                              if (string.length() > 1) {
                                  System.out.println(o);
                              }
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void multipleCasts() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o, Object o2) {
                          Object string = 1;
                          if (o instanceof String && o2 instanceof Integer) {
                              System.out.println((String) o);
                              System.out.println((Integer) o2);
                          }
                      }
                  }
                  """,
                """
                  public class A {
                      void test(Object o, Object o2) {
                          Object string = 1;
                          if (o instanceof String string1 && o2 instanceof Integer integer) {
                              System.out.println(string1);
                              System.out.println(integer);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void longNames() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.ArrayList;
                  public class A {
                      void test(Object o) {
                          Object list = 1;
                          if (o instanceof ArrayList<?>) {
                              System.out.println((ArrayList<?>) o);
                          }
                      }
                  }
                  """,
                """
                  import java.util.ArrayList;
                  public class A {
                      void test(Object o) {
                          Object list = 1;
                          if (o instanceof ArrayList<?> arrayList) {
                              System.out.println(arrayList);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_1() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.stream.Collectors;
                  import java.util.stream.Stream;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static Stream<Map<String, Object>> applyRoutesType(Object routes) {
                          if (routes instanceof List) {
                              List<Object> routesList = (List<Object>) routes;
                              if (routesList.isEmpty()) {
                                  return Stream.empty();
                              }
                              if (routesList.stream()
                                            .anyMatch(route -> !(route instanceof Map))) {
                                  return Stream.empty();
                              }
                              return routesList.stream()
                                               .map(route -> (Map<String, Object>) route);
                          }
                          return Stream.empty();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_2() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.stream.Collectors;
                  public class A {
                      public static List<Map<String, Object>> applyRoutesType(Object routes) {
                          if (routes instanceof List) {
                              List routesList = (List) routes;
                              if (routesList.isEmpty()) {
                                  return Collections.emptyList();
                              }
                          }
                          return Collections.emptyList();
                      }
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.stream.Collectors;
                  public class A {
                      public static List<Map<String, Object>> applyRoutesType(Object routes) {
                          if (routes instanceof List routesList) {
                              if (routesList.isEmpty()) {
                                  return Collections.emptyList();
                              }
                          }
                          return Collections.emptyList();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_3() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Collections;
                  import java.util.List;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static void applyRoutesType(Object routes) {
                          if (routes instanceof List) {
                              List<Object> routesList = (List<Object>) routes;
                              String.join(",", (List) routes);
                          }
                      }
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static void applyRoutesType(Object routes) {
                          if (routes instanceof List list) {
                              List<Object> routesList = (List<Object>) routes;
                              String.join(",", list);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_4() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Collections;
                  import java.util.List;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static void applyRoutesType(Object routes) {
                          if (routes instanceof List) {
                              String.join(",", (List) routes);
                          }
                      }
                  }
                  """,
                    """
                  import java.util.Collections;
                  import java.util.List;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static void applyRoutesType(Object routes) {
                          if (routes instanceof List list) {
                              String.join(",", list);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_5() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.Collection;
                  import java.util.List;
                  public class A {
                      @SuppressWarnings("unchecked")
                      private Collection<Object> addValueToList(List<String> previousValues, Object value) {
                          if (previousValues == null) {
                              return (value instanceof Collection) ? (Collection<Object>) value : Arrays.asList(value);
                          }
                          return List.of();
                      }
                 }
                 """
              )
            );
        }

        @Test
        void typeParameters_6() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.stream.Collectors;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static List<Map<String, Object>> applyRoutesType(Object routes) {
                          if (routes instanceof List) {
                              List<Object> routesList = (List<Object>) routes;
                              if (routesList.isEmpty()) {
                                  return Collections.emptyList();
                              }
                              if (routesList.stream()
                                            .anyMatch(route -> !(route instanceof Map))) {
                                  return Collections.emptyList();
                              }
                              return routesList.stream()
                                               .map(route -> (Map<String, Object>) route)
                                               .collect(Collectors.toList());
                          }
                          return Collections.emptyList();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_7() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.stream.Collectors;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static List<Map<String, Object>> applyRoutesType(Object routes) {
                          if (routes instanceof List) {
                              return ((List<?>) routes).stream()
                                             .map(route -> (Map<String, Object>) route)
                                             .collect(Collectors.toList());
                          }
                        return Collections.emptyList();
                      }
                  }
                  """,
                    """
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.stream.Collectors;
                  public class A {
                      @SuppressWarnings("unchecked")
                      public static List<Map<String, Object>> applyRoutesType(Object routes) {
                          if (routes instanceof List<?> list) {
                              return list.stream()
                                             .map(route -> (Map<String, Object>) route)
                                             .collect(Collectors.toList());
                          }
                        return Collections.emptyList();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeParameters_8() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Arrays;
                  import java.util.Collection;
                  import java.util.List;
                  public class A {
                      @SuppressWarnings("unchecked")
                      private Collection<Object> addValueToList(List<String> previousValues, Object value) {
                          Collection<Object> cl = List.of();
                          if (previousValues == null) {
                              if (value instanceof Collection) {
                                  cl = (Collection<Object>) value;
                              } else {
                                  cl = Arrays.asList(value.toString());
                              }
                          }
                          return cl;
                      }
                 }
                 """
              )
            );
        }

        @Test
        void primitiveArray() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof int[]) {
                              System.out.println((int[]) o);
                          }
                      }
                  }
                  """,
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof int[] ints) {
                              System.out.println(ints);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void matchingVariableInBody() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String) {
                              String str = (String) o;
                              String str2 = (String) o;
                              System.out.println(str + str2);
                          }
                      }
                  }
                  """,
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String str) {
                              String str2 = str;
                              System.out.println(str + str2);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void conflictingVariableInBody() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String) {
                              String string = "x";
                              System.out.println((String) o);
                  //            String string1 = "y";
                          }
                      }
                  }
                  """,
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String string1) {
                              String string = "x";
                              System.out.println(string1);
                  //            String string1 = "y";
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void conflictingVariableOfNestedType() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Map;

                  public class A {
                      void test(Object o) {
                          Map.Entry entry = null;
                          if (o instanceof Map.Entry) {
                            entry = (Map.Entry) o;
                          }
                          System.out.println(entry);
                      }
                  }
                  """,
                """
                  import java.util.Map;

                  public class A {
                      void test(Object o) {
                          Map.Entry entry = null;
                          if (o instanceof Map.Entry entry1) {
                            entry = entry1;
                          }
                          System.out.println(entry);
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/2787")
        @Test
        void nestedPotentiallyConflictingIfs() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String) {
                              if (o instanceof String) {
                                  System.out.println((String) o);
                              }
                              System.out.println((String) o);
                          }
                      }
                  }
                  """,
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String string1) {
                              if (o instanceof String string) {
                                  System.out.println(string);
                              }
                              System.out.println(string1);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/481")
        @Test
        void conflictingWithLocalVariable() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Main {
                       public class Bar {}
                       public class FooBar {}
                       public static void main(String[] args) {
                           Object o = new Object();
                           if (o instanceof Bar) {
                               System.out.println(((Bar)o));
                               Bar bar = null;
                               if (o instanceof FooBar) {
                                   System.out.println(((FooBar)o));
                                   Bar bar1 = null;
                               }
                           }
                       }
                  }
                  """,
                """
                  public class Main {
                       public class Bar {}
                       public class FooBar {}
                       public static void main(String[] args) {
                           Object o = new Object();
                           if (o instanceof Bar bar2) {
                               System.out.println(bar2);
                               Bar bar = null;
                               if (o instanceof FooBar fooBar) {
                                   System.out.println(fooBar);
                                   Bar bar1 = null;
                               }
                           }
                       }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/334")
        @Test
        void conflictingWithOtherInstanceOf() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Main {
                       public class Bar {}
                       public class FooBar {}
                       public static void main(String[] args) {
                           Object o = new Object();
                           if (o instanceof Bar) {
                               System.out.println(((Bar)o));
                               if (o instanceof FooBar) {
                                   System.out.println(((FooBar)o));
                                   Bar bar1 = null;
                               }
                           }
                       }
                  }
                  """,
                """
                  public class Main {
                       public class Bar {}
                       public class FooBar {}
                       public static void main(String[] args) {
                           Object o = new Object();
                           if (o instanceof Bar bar2) {
                               System.out.println(bar2);
                               if (o instanceof FooBar bar) {
                                   System.out.println(bar);
                                   Bar bar1 = null;
                               }
                           }
                       }
                  }
                  """
              )
            );
        }

        @Test
        void expressionWithSideEffects() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          Object s = 1;
                          if (convert(o) instanceof String && ((String) convert(o)).length() > 0) {
                              if (((String) convert(o)).length() > 1) {
                                  System.out.println(o);
                              }
                          }
                      }
                      Object convert(Object o) {
                          return o;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noTypeCast() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String) {
                              System.out.println(o);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeCastInElse() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String) {
                              System.out.println(o);
                          } else {
                              System.out.println((String) o);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void ifConditionWithPattern() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String s && s.length() > 0) {
                              System.out.println(s);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void orOperationInIfCondition() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (o instanceof String || ((String) o).length() > 0) {
                              if (((String) o).length() > 1) {
                                  System.out.println(o);
                              }
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void negatedInstanceOfMatchedInElse() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      void test(Object o) {
                          if (!(o instanceof String)) {
                              System.out.println(((String) o).length());
                          } else {
                              System.out.println(((String) o).length());
                          }
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/174")
        @Test
        void ifTwoDifferentInstanceOf() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        int combinedLength(Object o, Object o2) {
                            if (o instanceof String && o2 instanceof String) {
                                return ((String) o).length() + ((String) o2).length();
                            }
                            return -1;
                        }
                    }
                    """,
                  """
                    class A {
                        int combinedLength(Object o, Object o2) {
                            if (o instanceof String string && o2 instanceof String string1) {
                                return string.length() + string1.length();
                            }
                            return -1;
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/174")
        @Test
        void ifTwoDifferentInstanceOfWithParentheses() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        int combinedLength(Object o, Object o2) {
                            if (o instanceof String && (o2 instanceof String)) {
                                return ((String) o).length() + ((String) o2).length();
                            }
                            return -1;
                        }
                    }
                    """,
                  """
                    class A {
                        int combinedLength(Object o, Object o2) {
                            if (o instanceof String string && (o2 instanceof String string1)) {
                                return string.length() + string1.length();
                            }
                            return -1;
                        }
                    }
                    """
                ), 17
              )
            );
        }
    }

    @Nested
    @SuppressWarnings({"CastCanBeRemovedNarrowingVariableType", "ClassInitializerMayBeStatic"})
    class Ternary {

        @Test
        void typeCastInTrue() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      String test(Object o) {
                          return o instanceof String ? ((String) o).substring(1) : o.toString();
                      }
                  }
                  """,
                """
                  public class A {
                      String test(Object o) {
                          return o instanceof String s ? s.substring(1) : o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void multipleVariablesOnlyOneUsed() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      String test(Object o1, Object o2) {
                          return o1 instanceof String && o2 instanceof Number
                              ? ((String) o1).substring(1) : o1.toString();
                      }
                  }
                  """,
                """
                  public class A {
                      String test(Object o1, Object o2) {
                          return o1 instanceof String s && o2 instanceof Number
                              ? s.substring(1) : o1.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void initBlocks() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      static {
                          Object o = null;
                          String s = o instanceof String ? ((String) o).substring(1) : String.valueOf(o);
                      }
                      {
                          Object o = null;
                          String s = o instanceof String ? ((String) o).substring(1) : String.valueOf(o);
                      }
                  }
                  """,
                """
                  public class A {
                      static {
                          Object o = null;
                          String s = o instanceof String s1 ? s1.substring(1) : String.valueOf(o);
                      }
                      {
                          Object o = null;
                          String s = o instanceof String s1 ? s1.substring(1) : String.valueOf(o);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void typeCastInFalse() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      String test(Object o) {
                          return o instanceof String ? o.toString() : ((String) o).substring(1);
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/pull/265")
        @Test
        void multipleCastsInDifferentOperands() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.Comparator;
                  public class A {
                     Comparator<Object> comparator() {
                       return (a, b) ->
                           a instanceof String && b instanceof String ? ((String) a).compareTo((String) b) : 0;
                     }
                  }
                  """,
                """
                  import java.util.Comparator;
                  public class A {
                     Comparator<Object> comparator() {
                       return (a, b) ->
                           a instanceof String s && b instanceof String s1 ? s.compareTo(s1) : 0;
                     }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class Binary {

        @Test
        void onlyReplacementsBeforeOrOperator() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof String && ((String) o).length() > 1 || ((String) o).length() > 2;
                      }
                  }
                  """,
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof String s && s.length() > 1 || ((String) o).length() > 2;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void methodCallBreaksFlowScope() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      boolean m(Object o) {
                          return test(o instanceof String) && ((String) o).length() > 1;
                      }
                      boolean test(boolean b) {
                          return b;
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/572")
        @Test
        void castTypeIsSuperType() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Main {
                      void test(Object o) {
                          if (o instanceof Car) {
                              ((Vehicle) o).start();
                          }
                      }
                      private static abstract class Vehicle { abstract void start(); }
                      private static class Car extends Vehicle { void start() {} }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/572")
        @Test
        void castTypeIsSubType() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Main {
                      void test(Object o) {
                          if (o instanceof Car) {
                              ((Vehicle) o).start();
                          }
                      }
                      private static abstract class Vehicle { abstract void start(); }
                      private static class Car extends Vehicle { void start() {} }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class Arrays {

        @Test
        void string() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof String[] && ((String[]) o).length > 1 || ((String[]) o).length > 2;
                      }
                  }
                  """,
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof String[] ss && ss.length > 1 || ((String[]) o).length > 2;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void primitive() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof int[] && ((int[]) o).length > 1 || ((int[]) o).length > 2;
                      }
                  }
                  """,
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof int[] is && is.length > 1 || ((int[]) o).length > 2;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void multiDimensional() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof int[][] && ((int[][]) o).length > 1 || ((int[][]) o).length > 2;
                      }
                  }
                  """,
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof int[][] is && is.length > 1 || ((int[][]) o).length > 2;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void dimensionalMismatch() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      boolean test(Object o) {
                          return o instanceof int[][] && ((int[]) o).length > 1;
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    class Generics {
        @Test
        void wildcardInstanceOf() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class A {
                      Object test(Object o) {
                          if (o instanceof List<?>) {
                              return ((List<?>) o).get(0);
                          }
                          return o.toString();
                      }
                  }
                  """,
                """
                  import java.util.List;
                  public class A {
                      Object test(Object o) {
                          if (o instanceof List<?> list) {
                              return list.get(0);
                          }
                          return o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rawInstanceOfAndWildcardParameterizedCast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class A {
                      Object test(Object o) {
                          return o instanceof List ? ((List<?>) o).get(0) : o.toString();
                      }
                  }
                  """,
                """
                  import java.util.List;
                  public class A {
                      Object test(Object o) {
                          return o instanceof List<?> l ? l.get(0) : o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rawInstanceOfAndObjectParameterizedCast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class A {
                      Object test(Object o) {
                          return o instanceof List ? ((List<Object>) o).get(0) : o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rawInstanceOfAndParameterizedCast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class A {
                      String test(Object o) {
                          return o instanceof List ? ((List<String>) o).get(0) : o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void unboundGenericTypeVariable() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class A<T> {
                      void test(Object t) {
                          if (t instanceof List) {
                              List<T> l = (List<T>) t;
                              System.out.println(l.size());
                          }
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/482")
        @Test
        void castTooSpecific() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                         class B<T> {}
                         void method() {
                             Object o = new Object();
                             if (o instanceof B) {
                                 B<String> bString = (B) o;
                                 System.out.println(bString);
                             }
                         }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void bareAssignmentButParameterizedCheck() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        class B<T> {}
                        void method() {
                            Object o = new Object();
                            if (o instanceof B<?>) {
                                B b = (B)o;
                            }
                        }
                    }
                    """,
                  """
                    class A {
                        class B<T> {}
                        void method() {
                            Object o = new Object();
                            if (o instanceof B b) {
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/308")
        @Test
        void passBareTypeToParameterizedMethod() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        class B<T> {}
                        void method() {
                            Object o = new Object();
                            if (o instanceof B) {
                                param((B)o);
                            }
                        }
                        void param(B<String> b) {}
                    }
                    """,
                  """
                    class A {
                        class B<T> {}
                        void method() {
                            Object o = new Object();
                            if (o instanceof B b) {
                                param(b);
                            }
                        }
                        void param(B<String> b) {}
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void multipleVariablesOneNotAcceptableToCast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;
                  public class A {
                      void test(Object o1, Object o2) {
                          if (o1 instanceof String && o2 instanceof List<?>) {
                              String s = (String) o1;
                              List<String> l = (List) o2;
                              l.add(s);
                          }
                      }
                  }
                  """,
                """
                  import java.util.List;
                  public class A {
                      void test(Object o1, Object o2) {
                          if (o1 instanceof String s && o2 instanceof List<?>) {
                              List<String> l = (List) o2;
                              l.add(s);
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    @SuppressWarnings({"unchecked", "rawtypes"})
    class Various {
        @Test
        void unaryWithoutSideEffects() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      String test(Object o) {
                          return ((Object) ("1" + ~1)) instanceof String ? ((String) ((Object) ("1" + ~1))).substring(1) : o.toString();
                      }
                  }
                  """,
                """
                  public class A {
                      String test(Object o) {
                          return ((Object) ("1" + ~1)) instanceof String s ? s.substring(1) : o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void nestedClasses() {
            rewriteRun(
              //language=java
              java(
                """
                  public class A {
                      public static class Else {}
                      String test(Object o) {
                          if (o instanceof Else) {
                              return ((Else) o).toString();
                          }
                          return o.toString();
                      }
                  }
                  """,
                """
                  public class A {
                      public static class Else {}
                      String test(Object o) {
                          if (o instanceof Else else1) {
                              return else1.toString();
                          }
                          return o.toString();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void iterableParameter() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.HashMap;
                  import java.util.List;
                  import java.util.Map;

                  public class ApplicationSecurityGroupsParameterHelper {
                      static final String APPLICATION_SECURITY_GROUPS = "application-security-groups";
                      public Map<String, Object> transformGatewayParameters(Map<String, Object> parameters) {
                          Map<String, Object> environment = new HashMap<>();
                          Object applicationSecurityGroups = parameters.get(APPLICATION_SECURITY_GROUPS);
                          if (applicationSecurityGroups instanceof List) {
                              environment.put(APPLICATION_SECURITY_GROUPS, String.join(",", (List) applicationSecurityGroups));
                          }
                          return environment;
                      }
                  }
                  """,
                """
                  import java.util.HashMap;
                  import java.util.List;
                  import java.util.Map;

                  public class ApplicationSecurityGroupsParameterHelper {
                      static final String APPLICATION_SECURITY_GROUPS = "application-security-groups";
                      public Map<String, Object> transformGatewayParameters(Map<String, Object> parameters) {
                          Map<String, Object> environment = new HashMap<>();
                          Object applicationSecurityGroups = parameters.get(APPLICATION_SECURITY_GROUPS);
                          if (applicationSecurityGroups instanceof List list) {
                              environment.put(APPLICATION_SECURITY_GROUPS, String.join(",", list));
                          }
                          return environment;
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/483")
        @Test
        void classVariableShadowing() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        class B {}
                        Object b;
                        void method() {
                            Object o = new Object();
                            if (o instanceof B) {
                                B b = (B) o;
                                System.out.println(b);
                            }
                        }
                    }
                    """,
                  """
                    class A {
                        class B {}
                        Object b;
                        void method() {
                            Object o = new Object();
                            if (o instanceof B b) {
                                System.out.println(b);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/484")
        @Test
        void enumPatternMatch() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        enum B {}
                        void method() {
                            Object o = new Object();
                            if (o instanceof B) {
                                B e = (B) o;
                                System.out.println(e);
                            }
                        }
                    }
                    """,
                  """
                    class A {
                        enum B {}
                        void method() {
                            Object o = new Object();
                            if (o instanceof B e) {
                                System.out.println(e);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/528")
        @Test
        void matchingPrimitiveVariableInBody() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    class A {
                        void test(Object o, Integer integer) {
                            if (o instanceof Integer) {
                                for (int j = 0; j < (int) o; j++) {
                                }
                            }
                        }
                    }
                    """,
                  """
                    class A {
                        void test(Object o, Integer integer) {
                            if (o instanceof Integer integer1) {
                                for (int j = 0; j < integer1; j++) {
                                }
                            }
                        }
                    }
                    """
                ), 17)
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/532")
        @Test
        void varVariableDeclaration() {
            rewriteRun(
              version(
                //language=java
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String) {
                                final var str = (String) o;
                                System.out.println(str.toUpperCase());
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String str) {
                                System.out.println(str.toUpperCase());
                            }
                        }
                    }
                    """
                ), 17)
            );
        }
    }

    @Nested
    class Throws {
        @Test
        void throwsException() {
            rewriteRun(
              //language=java
              java(
                """
                  class A {
                      void test(Throwable t) {
                          if (t instanceof RuntimeException) {
                              throw (RuntimeException) t;
                          }
                      }
                  }
                  """,
                """
                  class A {
                      void test(Throwable t) {
                          if (t instanceof RuntimeException exception) {
                              throw exception;
                          }
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/307")
        @Test
        void throwsExceptionWithExtraParentheses() {
            rewriteRun(
              //language=java
              java(
                """
                  class A {
                      void test(Throwable t) {
                          if (t instanceof Exception) {
                              // Extra parentheses trips up the replacement
                              throw ((Exception) t);
                          }
                      }
                  }
                  """,
                """
                  class A {
                      void test(Throwable t) {
                          if (t instanceof Exception exception) {
                              // Extra parentheses trips up the replacement
                              throw exception;
                          }
                      }
                  }
                  """
              )
            );
        }

    }
    @Test
    void nestedVariables() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                public class A {
                    Throwable wrap(Throwable cause) {
                        if (cause instanceof Error) {
                            if (cause instanceof OutOfMemoryError) {
                                throw ((OutOfMemoryError) cause);
                            }
                            return (Error) cause;
                        }
                        return cause;
                    }
                }
                """,
              """
                public class A {
                    Throwable wrap(Throwable cause) {
                        if (cause instanceof Error error1) {
                            if (cause instanceof OutOfMemoryError error) {
                                throw error;
                            }
                            return error1;
                        }
                        return cause;
                    }
                }
                """
            ), 17)
        );
    }
}
