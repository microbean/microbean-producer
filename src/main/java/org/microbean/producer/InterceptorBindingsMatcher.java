/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024–2026 microBean™.
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

import java.util.Collection;
import java.util.List;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;

import org.microbean.assign.Annotated;
import org.microbean.assign.Matcher;

import org.microbean.bean.Id;

import static java.util.Objects.requireNonNull;

import static org.microbean.construct.element.AnnotationMirrors.containsAll;
import static org.microbean.construct.element.AnnotationMirrors.sameAnnotation;

/**
 * A {@link Matcher} encapsulating <a
 * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#interceptors">CDI-compatible interceptor binding
 * matching rules</a>.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #test(Collection, Collection)
 */
// TODO: Now that interceptors have been effectively refactored out into microbean-producer, this might be able to move
// there, or some other microbean-producer-dependent project.
public class InterceptorBindingsMatcher implements Matcher<Annotated<? extends AnnotatedConstruct>, Id> {

  private final InterceptorBindings ib;

  /**
   * Creates a new {@link InterceptorBindingsMatcher}.
   *
   * @param ib a non-{@code null} {@link InterceptorBindings}
   *
   * @exception NullPointerException if {@code ib} is {@code null}
   */
  public InterceptorBindingsMatcher(final InterceptorBindings ib) {
    super();
    this.ib = requireNonNull(ib, "ib");
  }

  private final boolean test(final Collection<? extends AnnotationMirror> receiverAttributes,
                             final Collection<? extends AnnotationMirror> payloadAttributes) {
    final Collection<? extends AnnotationMirror> payloadBindings = this.ib.interceptorBindings(payloadAttributes);
    if (payloadBindings.isEmpty()) {
      return this.ib.interceptorBindings(receiverAttributes).isEmpty();
    } else if (payloadBindings.size() == 1 && this.ib.anyInterceptorBinding(payloadBindings.iterator().next())) {
      return true;
    }
    final Collection<? extends AnnotationMirror> receiverBindings = this.ib.interceptorBindings(receiverAttributes);
    return
      receiverBindings.size() == payloadBindings.size() &&
      containsAll(receiverBindings, payloadBindings) &&
      containsAll(payloadBindings, receiverBindings);
  }

  @Override // Matcher<Annotated<? extends AnnotatedConstruct>, Id>
  public final boolean test(final Annotated<? extends AnnotatedConstruct> aac, final Id id) {
    return this.test(aac.annotations(), id.annotations());
  }

}
