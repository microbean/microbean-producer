/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2026 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.producer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.function.Predicate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

import org.microbean.construct.Domain;

import org.microbean.construct.element.SyntheticAnnotationMirror;
import org.microbean.construct.element.SyntheticAnnotationTypeElement;

import static java.util.Collections.unmodifiableList;

import static java.util.Objects.requireNonNull;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;

import static org.microbean.construct.element.AnnotationMirrors.sameAnnotation;
import static org.microbean.construct.element.AnnotationMirrors.streamBreadthFirst;

/**
 * A utility class providing methods that work with <dfn>interceptor bindings</dfn>.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public class InterceptorBindings {

  private final Predicate<? super ExecutableElement> annotationElementInclusionPredicate;

  private final Domain domain;

  private final AnnotationMirror metaInterceptorBinding;

  private final List<AnnotationMirror> metaInterceptorBindings;

  private final AnnotationMirror anyInterceptorBinding;

  private final List<AnnotationMirror> anyInterceptorBindings;
  
  /**
   * Creates a new {@link InterceptorBindings}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   *
   * @see #InterceptorBindings(Domain, AnnotationMirror, AnnotationMirror, Predicate)
   */
  public InterceptorBindings(final Domain domain) {
    this(domain, null, null, null);
  }

  /**
   * Creates a new {@link InterceptorBindings}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @param metaInterceptorBinding an {@link AnnotationMirror} to serve as the {@linkplain #metaInterceptorBinding()
   * meta-interceptor binding}; may (commonly) be {@code null} in which case a synthetic meta-interceptor binding will
   * be used instead
   *
   * @param anyInterceptorBinding an {@link AnnotationMirror} indicating that any interceptor binding should match; may
   * be {@code null}
   *
   * @param annotationElementInclusionPredicate a {@link Predicate} that returns {@code true} if a given {@link
   * ExecutableElement}, representing an annotation element, is to be included in any comparison operation; may be
   * {@code null} in which case it is as if {@code ()-> true} were supplied instead
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   */
  public InterceptorBindings(final Domain domain,
                             final AnnotationMirror metaInterceptorBinding,
                             final AnnotationMirror anyInterceptorBinding,
                             final Predicate<? super ExecutableElement> annotationElementInclusionPredicate) {
    super();
    if (metaInterceptorBinding == null || anyInterceptorBinding == null) {
      final List<? extends AnnotationMirror> as = domain.typeElement("java.lang.annotation.Documented").getAnnotationMirrors();
      assert as.size() == 3; // @Documented, @Retention, @Target, in that order, all annotated in turn with each other
      final AnnotationMirror documentedAnnotation = as.get(0);
      final AnnotationMirror retentionAnnotation = as.get(1);
      final AnnotationMirror targetAnnotation = as.get(2);
      this.metaInterceptorBinding =
        metaInterceptorBinding == null ?
        new SyntheticAnnotationMirror(new SyntheticAnnotationTypeElement(List.of(documentedAnnotation,
                                                                                 retentionAnnotation, // happens fortuitously to be RUNTIME
                                                                                 targetAnnotation), // happens fortuitously to be ANNOTATION_TYPE
                                                                         "InterceptorBinding")) :
        metaInterceptorBinding;
      this.anyInterceptorBinding =
        anyInterceptorBinding == null ?
        // TODO: meh, documented, retention, target, etc.
        new SyntheticAnnotationMirror(new SyntheticAnnotationTypeElement(List.of(this.metaInterceptorBinding), "Any")) :
        anyInterceptorBinding;
    } else {
      this.metaInterceptorBinding = metaInterceptorBinding;
      this.anyInterceptorBinding = anyInterceptorBinding;
    }
    this.domain = domain; // may not be needed
    this.annotationElementInclusionPredicate = annotationElementInclusionPredicate == null ? InterceptorBindings::returnTrue : annotationElementInclusionPredicate;
    this.metaInterceptorBindings = List.of(this.metaInterceptorBinding);
    this.anyInterceptorBindings = List.of(this.anyInterceptorBinding);
  }

  /**
   * Returns a non-{@code null}, determinate {@link AnnotationMirror} representing the <dfn>any interceptor
   * binding</dfn>.
   *
   * @return a non-{@code null}, determinate {@link AnnotationMirror} representing the <dfn>any interceptor
   * binding</dfn>
   */
  public final AnnotationMirror anyInterceptorBinding() {
    return this.anyInterceptorBinding;
  }

  /**
   * Returns {@code true} if and only if the supplied {@link AnnotationMirror} is {@linkplain
   * org.microbean.assign.Qualifiers#sameAnnotation(AnnotationMirror, AnnotationMirror) the same} as the {@linkplain
   * #anyInterceptorBinding() <dfn>any interceptor binding</dfn>}.
   *
   * @param a a non-{@code null} {@link AnnotationMirror}
   *
   * @return {@code true} if and only if the supplied {@link AnnotationMirror} is {@linkplain
   * org.microbean.assign.Qualifiers#sameAnnotation(AnnotationMirror, AnnotationMirror) the same} as the {@linkplain
   * #anyInterceptorBinding() <dfn>any interceptor binding</dfn>}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   *
   * @see #anyInterceptorBinding()
   *
   * @see org.microbean.construct.element.AnnotationMirrors#sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate)
   */
  public final boolean anyInterceptorBinding(final AnnotationMirror a) {
    return sameAnnotation(this.anyInterceptorBinding, a, this.annotationElementInclusionPredicate);
  }

  /**
   * Returns a non-{@code null}, determinate, immutable {@link List} housing only the {@linkplain
   * #anyInterceptorBinding() <dfn>any interceptor binding</dfn>}.
   *
   * @return a non-{@code null}, determinate, immutable {@link List} housing only the {@linkplain
   * #anyInterceptorBinding() <dfn>any interceptor binding</dfn>}
   */
  public final List<AnnotationMirror> anyInterceptorBindings() {
    return this.anyInterceptorBindings;
  }
  
  /**
   * Returns a non-{@code null}, determinate {@link AnnotationMirror} representing the (meta-) interceptor binding.
   *
   * @return a non-{@code null}, determinate {@link AnnotationMirror} representing the (meta-) interceptor binding
   */
  public AnnotationMirror metaInterceptorBinding() {
    return this.metaInterceptorBinding;
  }

  /**
   * Returns {@code true} if and only if the supplied {@link AnnotationMirror} has an {@linkplain
   * AnnotationMirror#getAnnotationType() annotation type} declared by a {@link TypeElement} that is {@linkplain
   * javax.lang.model.AnnotatedConstruct#getAnnotationMirrors() annotated with} at least one annotation {@linkplain
   * #metaInterceptorBinding(AnnotationMirror) deemed to be the meta-interceptor binding}.
   *
   * @param a a non-{@code null} {@link AnnotationMirror}
   *
   * @return {@code true} if and only if the supplied {@link AnnotationMirror} has an {@linkplain
   * AnnotationMirror#getAnnotationType() annotation type} declared by a {@link TypeElement} that is {@linkplain
   * javax.lang.model.AnnotatedConstruct#getAnnotationMirrors() annotated with} at least one annotation {@linkplain
   * #metaInterceptorBinding(AnnotationMirror) deemed to be the meta-interceptor binding}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   */
  public final boolean metaInterceptorBinding(final AnnotationMirror a) {
    return sameAnnotation(this.metaInterceptorBinding(), a, this.annotationElementInclusionPredicate);
  }

  /**
   * Returns a non-{@code null}, determinate, immutable {@link List} whose sole element is the {@linkplain
   * #metaInterceptorBinding() meta-interceptor binding} annotation.
   *
   * @return a non-{@code null}, determinate, immutable {@link List} whose sole element is the {@linkplain
   * #metaInterceptorBinding() meta-interceptor binding} annotation
   */
  public final List<AnnotationMirror> metaInterceptorBindings() {
    return this.metaInterceptorBindings;
  }

  /**
   * Returns {@code true} if and only if the supplied {@link AnnotationMirror} has an {@linkplain
   * AnnotationMirror#getAnnotationType() annotation type} declared by a {@link TypeElement} that is {@linkplain
   * javax.lang.model.AnnotatedConstruct#getAnnotationMirrors() annotated with} at least one annotation {@linkplain
   * #metaInterceptorBinding(AnnotationMirror) deemed to be the meta-interceptor binding}.
   *
   * @param a a non-{@code null} {@link AnnotationMirror}
   *
   * @return {@code true} if and only if the supplied {@link AnnotationMirror} has an {@linkplain
   * AnnotationMirror#getAnnotationType() annotation type} declared by a {@link TypeElement} that is {@linkplain
   * javax.lang.model.AnnotatedConstruct#getAnnotationMirrors() annotated with} at least one annotation {@linkplain
   * #metaInterceptorBinding(AnnotationMirror) deemed to be the meta-interceptor binding}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   */
  public final boolean interceptorBinding(final AnnotationMirror a) {
    if (!this.metaInterceptorBinding(a)) {
      final Element annotationInterface = a.getAnnotationType().asElement();
      if (annotationInterface.getKind() == ANNOTATION_TYPE) {
        for (final AnnotationMirror ma : annotationInterface.getAnnotationMirrors()) {
          if (this.metaInterceptorBinding(ma)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns a non-{@code null}, determinate, immutable {@link List} of {@link AnnotationMirror} instances drawn from
   * the supplied {@link Collection} that are {@linkplain #interceptorBinding(AnnotationMirror) deemed to be
   * interceptor bindings}.
   *
   * <p>In this implementation, cycles are avoided and comparisons are accomplished with the {@link
   * org.microbean.construct.element.AnnotationMirrors#sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)}
   * method.</p>
   *
   * <p>This implementation eliminates duplicates as calculated via the {@link
   * org.microbean.construct.element.AnnotationMirrors#sameAnnotation(AnnotationMirror, AnnotationMirror, Predicate)}
   * method.</p>
   *
   * <p>This implementation considers interceptor bindings to be <dfn>transitive</dfn>. Consequently, the returned
   * {@link List} may be greater in {@linkplain List#size() size} than the supplied {@link Collection}.</p>
   *
   * @param as a non-{@code null}, determinate {@link Collection} of {@link AnnotationMirror}s
   *
   * @return a non-{@code null}, determinate, immutable {@link List} of {@link AnnotationMirror} instances drawn from
   * the supplied {@link Collection} that were {@linkplain #interceptorBinding(AnnotationMirror) deemed to be
   * interceptor bindings}
   *
   * @exception NullPointerException if {@code as} is {@code null}
   *
   * @see #interceptorBinding(AnnotationMirror)
   *
   * @see org.microbean.construct.element.AnnotationMirrors#sameAnnotation(AnnotationMirror, AnnotationMirror,
   * Predicate)
   */
  public List<AnnotationMirror> interceptorBindings(final Collection<? extends AnnotationMirror> as) {
    if (as.isEmpty()) {
      return List.of();
    }
    final List<AnnotationMirror> seen = new ArrayList<>(as.size()); // size is arbitrary
    return
      streamBreadthFirst(as)
      .filter(a0 -> {
          for (final AnnotationMirror a1 : seen) {
            if (sameAnnotation(a0, a1, this.annotationElementInclusionPredicate)) {
              return false; // we included it already
            }
          }
          return this.interceptorBinding(a0) && seen.add(a0);
        })
      .toList();
  }

  private static final <X> boolean returnTrue(final X ignored) {
    return true;
  }

}
