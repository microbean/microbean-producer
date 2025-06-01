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
import java.util.SequencedSet;

import java.util.function.Function;

import org.microbean.bean.Assignment;
import org.microbean.bean.AttributedElement;
import org.microbean.bean.AttributedType;
import org.microbean.bean.Creation;
import org.microbean.bean.Destruction;
import org.microbean.bean.Id;

class DelegatingProducer<I> implements Producer<I> {

  private final Producer<I> delegate;

  DelegatingProducer(final Producer<I> delegate) {
    super();
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public SequencedSet<? extends Assignment<?>> assign(final Function<? super AttributedType, ?> f) {
    return this.delegate.assign(f);
  }

  @Override
  public SequencedSet<AttributedElement> dependencies() {
    return this.delegate.dependencies();
  }

  @Override
  public void dispose(final I i, final Destruction d) {
    this.delegate.dispose(i, d);
  }

  @Override
  public SequencedSet<AttributedElement> initializationDependencies() {
    return this.delegate.initializationDependencies();
  }
  
  @Override
  public I produce(final Creation<I> c) {
    return this.delegate.produce(c);
  }

  @Override
  public I produce(final Id id, final SequencedSet<? extends Assignment<?>> assignments) {
    return this.delegate.produce(id, assignments);
  }

  @Override
  public SequencedSet<AttributedElement> productionDependencies() {
    return this.delegate.productionDependencies();
  }
  
}
