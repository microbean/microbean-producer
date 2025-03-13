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

import java.util.SequencedSet;

import org.microbean.bean.Aggregate;
import org.microbean.bean.Assignment;
import org.microbean.bean.AttributedElement;
import org.microbean.bean.Request;

/**
 * An interface whose implementations {@linkplain #initialize(Object, Request) initialize} contextual instances.
 *
 * <p>{@link Initializer}s are subordinate to {@link Producer}s, typically operating on the contextual instances they
 * produce.</p>
 *
 * @param <I> the type of contextual instance
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #initialize(Object, Request)
 */
// Subordinate to Factory<I> (really to PostInitializer<I>). Normally applied to Producer<I> output.
// Calls initializer methods and injects fields
// Note that this deliberately extends Aggregate, providing access to dependencies.
public interface Initializer<I> extends Aggregate {

  /**
   * A convenience method that assigns a contextual reference to each of this {@link Initializer}'s {@link
   * org.microbean.bean.AttributedElement} instances and returns the resulting {@link SequencedSet} of {@link
   * Assignment}s.
   *
   * <p>Typically there is no need to override this method.</p>
   *
   * @param r a {@link Request} that retrieves a contextual reference suitable for an {@link
   * org.microbean.bean.AttributedType}; must not be {@code null}
   *
   * @return an immutable {@link SequencedSet} of {@link Assignment} instances; never {@code null}
   *
   * @exception NullPointerException if {@code r} is {@code null}
   *
   * @see #assign(java.util.function.Function)
   */
  public default SequencedSet<? extends Assignment<?>> assign(final Request<?> r) {
    return this.assign(r::reference);
  }

  /**
   * Returns an immutable {@link SequencedSet} of {@link AttributedElement} instances <strong>required for instance
   * initialization only</strong>.
   *
   * <p>The returned {@link SequencedSet} may be, and often is, empty.</p>
   *
   * <p>Overriding this method is normal and expected.</p>
   *
   * <p>Any overrides of this method must return determinate values.</p>
   *
   * @return an immutable {@link SequencedSet} of {@link AttributedElement} instances; never {@code null}
   *
   * @see Aggregate#dependencies()
   *
   * @see #assign(java.util.function.Function)
   *
   * @see #initialize(Object, SequencedSet)
   *
   * @see AttributedElement
   */
  @Override // Aggregate
  public default SequencedSet<AttributedElement> dependencies() {
    return Aggregate.super.dependencies();
  }
  
  /**
   * Initializes the supplied contextual instance, possibly using the supplied {@link Request} to obtain supporting
   * contextual references.
   *
   * <p>Typically {@link Initializer} implementations will perform <dfn>field injection</dfn> and then will call
   * <dfn>initializer methods</dfn> on the supplied contextual instance.</p>
   *
   * <p>Normally there is no need to override the default implementation of this method, which calls the {@link
   * #initialize(Object, SequencedSet)} method with {@link Assignment}s derived from this {@link Initializer}'s
   * {@linkplain #dependencies() dependencies}.</p>
   *
   * @param i the contextual instance to initialize; may be {@code null}
   *
   * @param r a {@link Request} that can be used to acquire supporting contextual references; may be {@code null}
   *
   * @return the initialized instance, or a copy of it, or a stand-in for it
   *
   * @see #initialize(Object, SequencedSet)
   */
  public default I initialize(final I i, final Request<I> r) {
    return this.initialize(i, this.assign(r));
  }

  /**
   * Initializes the supplied contextual instance, possibly (often) making use of the supplied, dependent,
   * contextual references.
   *
   * <p>Implementations of this method must not call {@link #initialize(Object, Request)} or an infinite loop may
   * result.</p>
   *
   * <p>Typically {@link Initializer} implementations will perform <dfn>field injection</dfn> and then will call
   * <dfn>initializer methods</dfn> on the supplied contextual instance.</p>
   *
   * @param i the contextual instance to initialize; may be {@code null}
   *
   * @param assignments a {@link SequencedSet} of {@link Assignment}s to be distributed appropriately among fields and
   * initializer methods; must not be {@code null}
   *
   * @return the initialized instance, or a copy of it, or a stand-in for it
   *
   * @exception NullPointerException if {@code assignments} is {@code null}
   *
   * @see Producer#produce(Request)
   */
  public I initialize(final I i, final SequencedSet<? extends Assignment<?>> assignments);

}
