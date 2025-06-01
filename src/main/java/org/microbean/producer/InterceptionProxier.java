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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.function.Supplier;

import javax.lang.model.element.ExecutableElement;

import org.microbean.bean.Id;

import org.microbean.construct.Domain;

import org.microbean.interceptor.InterceptorMethod;

import org.microbean.proxy.ProxySpecification;

import static java.util.HashMap.newHashMap;

/**
 * A creator of proxies for business method interceptions.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public interface InterceptionProxier {

  /**
   * Creates an <dfn>interception proxy</dfn> for a given contextual instance.
   *
   * @param <I> the contextual instance type
   *
   * @param id an {@link Id}; must not be {@code null}
   *
   * @param instanceSupplier a {@link Supplier} of contextual instances of the appropriate type; must not be {@code null}
   *
   * @param aroundInvokeInterceptions a {@link Map} of {@link InterceptorMethod}s indexed by the {@link
   * ExecutableElement} to which they apply; must not be {@code null}
   *
   * @return a non-{@code null} interception proxy
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public <I> I interceptionProxy(final Id id,
                                 final Supplier<? extends I> instanceSupplier,
                                 final Map<ExecutableElement, List<InterceptorMethod>> aroundInvokeInterceptions);

  /**
   * A {@link ProxySpecification} that exposes {@link InterceptorMethod}-related information.
   *
   * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
   *
   * @see #interceptorMethods(ExecutableElement)
   */
  public static class Specification extends ProxySpecification {

    private final Map<ExecutableElement, List<InterceptorMethod>> aroundInvokeInterceptions;

    /**
     * Creates a new {@link Specification}.
     *
     * @param domain a {@link Domain}; must not be {@code null}
     *
     * @param id an {@link Id}; must not be {@code null}
     *
     * @param aroundInvokeInterceptions a {@link Map} of {@link InterceptorMethod}s indexed by the {@link
     * ExecutableElement} to which they apply; must not be {@code null}
     *
     * @exception NullPointerException if any argument is {@code null}
     */
    public Specification(final Domain domain,
                         final Id id,
                         final Map<ExecutableElement, List<InterceptorMethod>> aroundInvokeInterceptions) {
      super(domain, id);
      final Map<ExecutableElement, List<InterceptorMethod>> m = newHashMap(aroundInvokeInterceptions.size());
      for (final Entry<ExecutableElement, List<InterceptorMethod>> e : aroundInvokeInterceptions.entrySet()) {
        m.put(e.getKey(), List.copyOf(e.getValue()));
      }
      this.aroundInvokeInterceptions = Map.copyOf(m);
    }

    /**
     * Returns a non-{@code null}, immutable, determinate {@link List} of {@link InterceptorMethod}s pertaining to the
     * supplied {@link ExecutableElement}.
     *
     * @param ee an {@link ExecutableElement}; must not be {@code null}
     *
     * @return a non-{@code null}, immutable, determinate {@link List} of {@link InterceptorMethod}s pertaining to the
     * supplied {@link ExecutableElement}
     *
     * @exception NullPointerException if {@code ee} is {@code null}
     */
    public final List<InterceptorMethod> interceptorMethods(final ExecutableElement ee) {
      final List<InterceptorMethod> ims = this.aroundInvokeInterceptions.get(ee);
      return ims == null || ims.isEmpty() ? List.of() : ims;
    }
    
  }
  
}
