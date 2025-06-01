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

import java.util.Objects;

/**
 * An abstract descriptor of a type of {@link org.microbean.interceptor.InterceptorMethod InterceptorMethod}.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public abstract sealed class InterceptorMethodType permits AbstractLifecycleCallbackInterceptorMethodType, AbstractMethodInterceptorMethodType {

  private final String name;

  private final TargetKind kind;

  /**
   * Creates a new {@link InterceptorMethodType}.
   *
   * @param kind a {@link TargetKind}; must not be {@code null}
   *
   * @param name a globally unique name; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  InterceptorMethodType(final TargetKind kind, final String name) {
    super();
    this.kind = Objects.requireNonNull(kind, "kind");
    this.name = Objects.requireNonNull(name, "name");
  }

  /**
   * Returns the {@link TargetKind} affiliated with this {@link InterceptorMethodType}.
   *
   * @return a non-{@code null} {@link TargetKind}
   */
  public final TargetKind kind() {
    return this.kind;
  }

  /**
   * Returns the name of this {@link InterceptorMethodType}.
   *
   * @return a non-{@code null}, globally unique name
   */
  public final String name() {
    return this.name;
  }

  @Override
  public int hashCode() {
    return 31 * (31 + this.kind().hashCode()) + this.name().hashCode();
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      final InterceptorMethodType her = (InterceptorMethodType)other;
      return
        Objects.equals(this.name(), her.name()) &&
        Objects.equals(this.kind(), her.kind());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return this.name();
  }

  /**
   * A descriptor of a kind of interceptor method.
   *
   * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
   */
  public enum TargetKind {

    /**
     * A constructor-oriented {@link TargetKind}.
     */
    CONSTRUCTOR,

    /**
     * A lifecycle event-oriented {@link TargetKind}.
     */
    EVENT,

    /**
     * A business method-oriented {@link TargetKind}.
     */
    METHOD;
  }

}
