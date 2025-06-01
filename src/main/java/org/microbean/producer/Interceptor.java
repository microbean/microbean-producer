/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025 microBean™.
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

import org.microbean.attributes.Attributes;

import org.microbean.interceptor.InterceptorMethod;

/**
 * A collection of {@link InterceptorMethod}s indexed by {@link InterceptorMethodType}.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public interface Interceptor extends AutoCloseable {

  /**
   * Closes this {@link Interceptor}, rendering it inappropriate for further use.
   *
   * <p>The default implementation of this method does nothing.</p>
   */
  @Override // AutoCloseable
  public default void close() {

  }

  /**
   * Returns a non-{@code null}, immutable, determinate {@link Collection} of {@link InterceptorMethod}s indexed under
   * the supplied {@link InterceptorMethodType}.
   *
   * @param type an {@link InterceptorMethodType}; must not be {@code null}
   *
   * @return a non-{@code null}, immutable, determinate {@link Collection} of {@link InterceptorMethod}s indexed under
   * the supplied {@link InterceptorMethodType}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   */
  // "Give me the @AroundInvoke interceptor methods that are bound to the interceptor binding set that I am attributed
  // with"
  //
  // Interceptor bindings must be at the class level and will therefore apply to all interceptor methods.
  public Collection<? extends InterceptorMethod> interceptorMethods(final InterceptorMethodType type);

}
