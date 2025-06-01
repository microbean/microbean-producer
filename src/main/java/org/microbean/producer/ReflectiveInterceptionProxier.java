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

import static java.lang.invoke.MethodHandles.Lookup;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Supplier;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.construct.Domain;

import org.microbean.interceptor.InterceptionFunction;
import org.microbean.interceptor.InterceptorMethod;

import org.microbean.producer.InterceptionProxier.Specification;

import org.microbean.proxy.AbstractReflectiveProxier;
import org.microbean.proxy.Proxy;

import static java.lang.System.identityHashCode;

import static java.lang.invoke.MethodHandles.lookup;

import static java.lang.reflect.InvocationHandler.invokeDefault;

import static java.lang.reflect.Proxy.newProxyInstance;

import static org.microbean.interceptor.Interceptions.ofInvocation;

/**
 * An {@link AbstractReflectiveProxier} implementation that uses the proxy features of the Java Development Kit.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class ReflectiveInterceptionProxier extends AbstractReflectiveProxier<Specification> {

  private static final Lookup lookup = lookup();

  /**
   * Creates a new {@link ReflectiveInterceptionProxier}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   */
  public ReflectiveInterceptionProxier(final Domain domain) {
    super(domain);
  }

  @Override // AbstractReflectiveProxier<Specification>
  @SuppressWarnings("unchecked")
  protected final <R> Proxy<R> proxy(final Specification ps,
                                     final Class<?>[] interfaces,
                                     final Supplier<? extends R> instanceSupplier) {
    final Map<Method, ExecutableElement> ees = new ConcurrentHashMap<>();
    final Map<Method, InterceptionFunction> fs = new ConcurrentHashMap<>();
    final R instance = Objects.requireNonNull(instanceSupplier.get(), "instanceSupplier.get()");
    return
      (Proxy<R>)newProxyInstance(this.classLoader(), interfaces, new InvocationHandler() {
          @Override // InvocationHandler
          public final Object invoke(final Object p, final Method m, final Object[] a) throws Throwable {
            return switch (m) {
            case null -> throw new NullPointerException("m");
            case Method x when equalsMethod(m) -> p == a[0];
            case Method x when hashCodeMethod(m) -> identityHashCode(p);
            default -> {
              final List<InterceptorMethod> interceptorMethods =
                ps.interceptorMethods(ees.computeIfAbsent(m, ReflectiveInterceptionProxier.this::executableElement));
              if (interceptorMethods.isEmpty()) {
                yield m.isDefault() ? invokeDefault(instance, m, a) : m.invoke(instance, a);
              }
              yield fs.computeIfAbsent(m, m0 -> {
                  try {
                    return ofInvocation(interceptorMethods, lookup, m0, () -> instance, null);
                  } catch (final IllegalAccessException e) {
                    throw new IllegalArgumentException("m0: " + m0, e);
                  }
                })
                .apply(a);
            }
            };
          }
        });
  }

}
