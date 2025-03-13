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

/**
 * Provides classes and interfaces implementing producers.
 *
 * <p>You can implement a {@link org.microbean.bean.Factory Factory}'s {@link
 * org.microbean.bean.Factory#create(org.microbean.bean.Request) create(Request)} method like so:</p>
 *
 * {@snippet :
 *   @Override
 *   public I create(final Request<I> r) { // @link substring="create" target="org.microbean.bean.Factory#create(org.microbean.bean.Request)" @link substring="Request" target="org.microbean.bean.Request"
 *     return
 *       interceptionsApplicator.apply( // @link regex='(?<=\.)apply' target="InterceptionsApplicator#apply(Object, org.microbean.bean.Request)"
 *       postInitializer.postInitialize( // @link regex='(?<=\.)postInitialize' target="PostInitializer#postInitialize(Object, org.microbean.bean.Request)"
 *       initializer.initialize( // @link regex='(?<=\.)initialize' target="Initializer#initialize(Object, org.microbean.bean.Request)"
 *       producer.produce(r), r), r), r); // note that producer might be an InterceptingProducer // @link regex='(?<=\.)produce' target="Producer#produce(org.microbean.bean.Request)" @link substring="InterceptingProducer" target="InterceptingProducer"
 *   }
 *   }
 *
 * <p>You can implement a {@link org.microbean.bean.Factory Factory}'s {@link org.microbean.bean.Factory#destroy(Object,
 * org.microbean.bean.Request) destroy(Object, Request)} method like so:</p>
 *
 * {@snippet :
 *   @Override
 *   public void destroy(final I i, final Request<I> creationRequest) { // @link substring="create" target="org.microbean.bean.Factory#create(org.microbean.bean.Request)" @link regex='\bRequest' target="org.microbean.bean.Request"
 *     producer.dispose(preDestructor.preDestroy(i, creationRequest), creationRequest); // @link regex='(?<=\.)dispose' target="Producer#dispose(Object, org.microbean.bean.Request)" @link regex='(?<=\.)preDestroy' target="PreDestructor#preDestroy(Object, org.microbean.bean.Request)"
 *   }
 *   }
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
package org.microbean.producer;
