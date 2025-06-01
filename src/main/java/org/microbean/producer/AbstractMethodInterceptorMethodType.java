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

/**
 * A business method-oriented {@link InterceptorMethodType}.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public abstract non-sealed class AbstractMethodInterceptorMethodType extends InterceptorMethodType {

  /**
   * Creates a new {@link AbstractMethodInterceptorMethodType}.
   *
   * @param name a globally unique name; must not be {@code null}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   */
  protected AbstractMethodInterceptorMethodType(final String name) {
    super(TargetKind.METHOD, name);
  }
  
}
