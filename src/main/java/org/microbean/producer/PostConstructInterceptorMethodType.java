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
 * An {@link AbstractLifecycleCallbackInterceptorMethodType} representing a post-construct interception.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class PostConstructInterceptorMethodType extends AbstractLifecycleCallbackInterceptorMethodType {

  /**
   * The sole instance of this class.
   */
  public static final PostConstructInterceptorMethodType INSTANCE = new PostConstructInterceptorMethodType();
  
  private PostConstructInterceptorMethodType() {
    super(TargetKind.EVENT, "PostConstruct");
  }
  
}
