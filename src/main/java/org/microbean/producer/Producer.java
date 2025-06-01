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

import java.util.LinkedHashSet;
import java.util.SequencedSet;

import java.util.function.Function;

import org.microbean.bean.Aggregate;
import org.microbean.bean.Assignment;
import org.microbean.bean.AttributedElement;
import org.microbean.bean.AttributedType;
import org.microbean.bean.Creation;
import org.microbean.bean.Destruction;
import org.microbean.bean.Id;
import org.microbean.bean.ReferencesSelector;

import static java.util.Collections.unmodifiableSequencedSet;

/**
 * An interface whose implementations {@linkplain #produce(Creation) produce} and commonly initialize contextual
 * instances.
 *
 * <p>{@link Producer}s are used to implement {@link org.microbean.bean.Factory} instances' {@link
 * org.microbean.bean.Factory#create(Creation) create(Creation)} and {@link org.microbean.bean.Factory#destroy(Object,
 * Destruction) destroy(Object, Destruction)} methods.</p>
 *
 * <p>A {@link Producer} normally initializes the contextual instances it produces as part of its {@link
 * #produce(Creation)} method implementation, but is not required to.</p>
 *
 * @param <I> the contextual instance type
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #produce(Creation)
 *
 * @see #dependencies()
 *
 * @see #dispose(Object, Destruction)
 *
 * @see org.microbean.bean.Factory#create(Creation)
 */
// Subordinate to Factory<I> (but looking more and more like it every day)
// Akin to CDI's Producer.
// Handles instance production, interception and disposal, *including intercepted production*.
//
// Also handles initialization. We may want to revisit this. See for example
// https://github.com/search?q=repo%3Aweld%2Fcore+%22.produce%28%29%22+language%3AJava&type=code
//
// See also: InterceptingProducer
public interface Producer<I> extends Aggregate {

  /**
   * A convenience method that returns an immutable, determinate {@link SequencedSet} of {@link AttributedElement}s
   * consisting of this {@link Producer}'s {@linkplain #productionDependencies() production dependencies} followed by
   * its {@linkplain #initializationDependencies() initialization dependencies}.
   *
   * <p>There is normally no need to override the default implementation of this method.</p>
   *
   * @return a non-{@code null}, immutable, determinate {@link SequencedSet} of {@link AttributedElement}s consisting of
   * this {@link Producer}'s {@linkplain #productionDependencies() production dependencies} followed by its {@linkplain
   * #initializationDependencies() initialization dependencies}
   *
   * @see #productionDependencies()
   *
   * @see #initializationDependencies()
   */
  @Override // Aggregate
  public default SequencedSet<AttributedElement> dependencies() {
    final SequencedSet<AttributedElement> productionDependencies = this.productionDependencies();
    final SequencedSet<AttributedElement> initializationDependencies = this.initializationDependencies();
    if (productionDependencies.isEmpty()) {
      return initializationDependencies;
    } else if (initializationDependencies.isEmpty()) {
      return productionDependencies;
    }
    final LinkedHashSet<AttributedElement> d = new LinkedHashSet<>();
    d.addAll(productionDependencies);
    d.addAll(initializationDependencies);
    return unmodifiableSequencedSet(d);
  }

  /**
   * Disposes of the supplied contextual instance.
   *
   * <p>The default implementation of this method checks to see if {@code i} is an instance of {@link AutoCloseable},
   * and, if so, calls {@link AutoCloseable#close() close()} on it, throwing any resulting exception as a {@link
   * DisposalException}.</p>
   *
   * @param i a contextual instance {@linkplain #produce(Creation) produced} by this {@link Producer}; may be {@code
   * null}
   *
   * @param r the {@link Creation} that was {@linkplain #produce(Creation) present at production time}; must not be {@code
   * null}
   *
   * @exception NullPointerException if {@code r} is {@code null}
   *
   * @exception DisposalException if {@code i} is an {@link AutoCloseable} instance, and if its {@link
   * AutoCloseable#close() close()} method throws a checked exception
   */
  public default void dispose(final I i, final Destruction r) {
    if (i instanceof AutoCloseable ac) {
      try {
        ac.close();
      } catch (final RuntimeException | Error e) {
        throw e;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new DisposalException(e.getMessage(), e);
      } catch (final Exception e) {
        throw new DisposalException(e.getMessage(), e);
      }
    }
  }

  /**
   * Returns an immutable, determinate {@link SequencedSet} of {@link AttributedElement}s representing dependencies
   * required for initialization.
   *
   * <p>Such dependencies may represent initialization method parameters and/or fields.</p>
   *
   * <p>Contrast initialization dependencies with <dfn>production dependencies</dfn>.</p>
   *
   * @return a non-{@code null}, immutable, determinate {@link SequencedSet} of {@link AttributedElement}s representing
   * dependencies required for initialization
   *
   * @see #productionDependencies()
   */
  public SequencedSet<AttributedElement> initializationDependencies();

  /**
   * Produces a new contextual instance and returns it by calling the {@link #produce(Id, SequencedSet)} method with the
   * return value of an invocation of the {@link #assign(Function)} method with a reference to the supplied {@link
   * Creation}'s {@link ReferencesSelector#reference(AttributedType) reference(AttributedType)} method.
   *
   * @param c a {@link Creation}; must not be {@code null}
   *
   * @return a new contextual instance, or {@code null}
   *
   * @exception NullPointerException if {@code c} is {@code null}
   *
   * @see #produce(Id, SequencedSet)
   *
   * @see #dependencies()
   */
  public default I produce(final Creation<I> c) {
    return this.produce(c.id(), this.assign(c::reference));
  }

  /**
   * Produces a new contextual instance and returns it, possibly (often) making use of the supplied assignments.
   *
   * <p>Implementations of this method must not call {@link #produce(Creation)} or an infinite loop may result.</p>
   *
   * @param id an {@link Id} for which production is occurring; must not be {@code null}
   *
   * @param assignments a {@link SequencedSet} of {@link Assignment}s this {@link Producer} needs to complete production
   * and possibly initialization; must not be {@code null}
   *
   * @return a new contextual instance, or {@code null}
   *
   * @exception NullPointerException if {@code assignments} is {@code null}
   */
  public I produce(final Id id, final SequencedSet<? extends Assignment<?>> assignments);

  /**
   * Returns an immutable, determinate {@link SequencedSet} of {@link AttributedElement}s representing dependencies
   * required for production.
   *
   * <p>Such dependencies normally represent constructor parameters.</p>
   *
   * <p>Contrast production dependencies with <dfn>initialization dependencies</dfn>.</p>
   *
   * @return a non-{@code null}, immutable, determinate {@link SequencedSet} of {@link AttributedElement}s representing
   * dependencies required for production
   *
   * @see #initializationDependencies()
   */
  public SequencedSet<AttributedElement> productionDependencies();

}
