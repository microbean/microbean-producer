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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SequencedSet;
import java.util.StringJoiner;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.microbean.bean.Assignment;

import org.microbean.attributes.ArrayValue;
import org.microbean.attributes.Attributes;
import org.microbean.attributes.ListValue;
import org.microbean.attributes.Value;

import org.microbean.bean.AttributedElement;
import org.microbean.bean.AttributedType;
import org.microbean.bean.Creation;
import org.microbean.bean.Destruction;
import org.microbean.bean.Id;
import org.microbean.bean.ReferencesSelector;

import org.microbean.construct.Domain;

import org.microbean.interceptor.InterceptionFunction;
import org.microbean.interceptor.InterceptorMethod;

import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableSequencedSet;

import static java.util.HashMap.newHashMap;

import static java.util.HashSet.newHashSet;

import static org.microbean.assign.Qualifiers.anyQualifier;

import static org.microbean.interceptor.Interceptions.ofConstruction;
import static org.microbean.interceptor.Interceptions.ofInvocation;
import static org.microbean.interceptor.Interceptions.ofLifecycleEvent;

/**
 * A {@link Producer} that applies various kinds of interceptions to a delegate {@link Producer}.
 *
 * @param <I> the contextual instance type
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public class InterceptingProducer<I> extends DelegatingProducer<I> {


  /*
   * Static fields.
   */


  private static final List<InterceptorMethodType> TYPES =
    List.of(AroundConstructInterceptorMethodType.INSTANCE,
            PostConstructInterceptorMethodType.INSTANCE,
            AroundInvokeInterceptorMethodType.INSTANCE,
            PreDestroyInterceptorMethodType.INSTANCE);

  private static final Map<Id, Map<ExecutableElement, Set<Attributes>>> ibs = new ConcurrentHashMap<>();

  private static final Map<Destruction, BiConsumer<? super Object, Destruction>> bcs = synchronizedMap(newHashMap(100));


  /*
   * Instance fields.
   */


  // i.e. the TypeMirror corresponding to org.microbean.producer.Interceptor.class
  private final TypeMirror interceptorType;

  private final InterceptionProxier proxier;


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link InterceptingProducer}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @param delegate a {@link Producer} to which ultimate production will be delegated; must not be {@code null}
   *
   * @param proxier an {@link InterceptionProxier}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public InterceptingProducer(final Domain domain, final Producer<I> delegate, final InterceptionProxier proxier) {
    super(delegate);
    this.interceptorType = domain.declaredType(Interceptor.class.getCanonicalName());
    this.proxier = Objects.requireNonNull(proxier, "proxier");
  }


  /*
   * Instance methods.
   */


  @Override // DelegatingProducer<I>
  public final SequencedSet<AttributedElement> dependencies() {
    return super.dependencies();
  }
  
  @Override // DelegatingProducer<I>
  public final void dispose(final I i, final Destruction d) {
    // Note that the removal here means you can only call dispose() once for a given instance if it has a pre-destroy. I
    // think that's OK?
    final BiConsumer<? super Object, Destruction> bc = bcs.remove(d);
    if (bc != null) {
      bc.accept(i, d);
    }
    super.dispose(i, d);
  }

  @Override // DelegatingProducer<I>
  public final I produce(final Creation<I> c) {
    if (c == null) {
      return super.produce(c);
    }
    final Id id = c.id();

    final Map<ExecutableElement, Set<Attributes>> ibsByMethod =
      ibs.computeIfAbsent(id, InterceptingProducer::interceptorBindingsByMethod);
    if (ibsByMethod.isEmpty()) {
      // No interceptions. Bail out.
      return super.produce(c);
    }

    final Map<ExecutableElement, List<InterceptorMethod>> interceptorMethodsByMethod = newHashMap(13);
    final Map<InterceptorMethodType, Set<ExecutableElement>> methodsByInterceptorType = newHashMap(5);
    for (final Entry<ExecutableElement, Set<Attributes>> e : ibsByMethod.entrySet()) {
      for (final Interceptor interceptor : interceptors(e.getValue(), c)) {
        for (final InterceptorMethodType type : TYPES) {
          final Collection<? extends InterceptorMethod> interceptorMethods = interceptor.interceptorMethods(type);
          if (interceptorMethods != null && !interceptorMethods.isEmpty()) {
            final ExecutableElement method = e.getKey();
            methodsByInterceptorType.computeIfAbsent(type, t -> newHashSet(7)).add(method);
            interceptorMethodsByMethod.computeIfAbsent(method, m -> new ArrayList<>(7)).addAll(interceptorMethods);
          }
        }
      }
    }
    if (methodsByInterceptorType.isEmpty()) {
      // No interceptions. Bail out.
      return super.produce(c);
    }

    Supplier<I> s = () -> super.produce(c);

    // Any around-constructs?
    final Set<ExecutableElement> interceptedConstructors = methodsByInterceptorType.remove(AroundConstructInterceptorMethodType.INSTANCE);
    if (interceptedConstructors != null && !interceptedConstructors.isEmpty()) {
      assert interceptedConstructors.size() == 1 : "interceptedConstructors: " + interceptedConstructors;
      final List<InterceptorMethod> constructorInterceptorMethods =
        interceptorMethodsByMethod.remove(interceptedConstructors.iterator().next());
      s = this.aroundConstructsSupplier(id, constructorInterceptorMethods, c);
    }
    if (methodsByInterceptorType.isEmpty()) {
      // No additional interceptions. Bail out.
      return s.get();
    }

    // Any around-invokes?
    final Set<ExecutableElement> interceptedBusinessMethods = methodsByInterceptorType.remove(AroundInvokeInterceptorMethodType.INSTANCE);
    if (interceptedBusinessMethods != null && !interceptedBusinessMethods.isEmpty()) {
      s = this.aroundInvokesSupplier(s, interceptedBusinessMethods, interceptorMethodsByMethod::get, id);
    }
    if (methodsByInterceptorType.isEmpty()) {
      // No additional interceptions. Bail out.
      return s.get();
    }

    // Any post-constructs?
    final Set<ExecutableElement> postConstructMethods = methodsByInterceptorType.remove(PostConstructInterceptorMethodType.INSTANCE);
    if (postConstructMethods != null && !postConstructMethods.isEmpty()) {
      s = postConstructsSupplier(s, postConstructMethods, interceptorMethodsByMethod::remove);
    }
    if (methodsByInterceptorType.isEmpty()) {
      // No additional interceptions. Bail out.
      return s.get();
    }

    // Any pre-destroys?
    final Set<ExecutableElement> preDestroyMethods = methodsByInterceptorType.remove(PreDestroyInterceptorMethodType.INSTANCE);
    if (preDestroyMethods != null && !preDestroyMethods.isEmpty()) {
      final BiConsumer<? super Object, Destruction> bc =
        preDestroysBiConsumer(preDestroyMethods, interceptorMethodsByMethod::remove);
      if (bc != null) {
        // TODO: yuck
        bcs.put((Destruction)c, bc);
      }
    }

    return s.get();
  }

  @SuppressWarnings("unchecked")
  private final Supplier<I> aroundConstructsSupplier(final Id id,
                                                     final Collection<? extends InterceptorMethod> constructorInterceptorMethods,
                                                     final ReferencesSelector rs) {
    final SequencedSet<? extends AttributedElement> pds = this.productionDependencies();
    final SequencedSet<? extends AttributedElement> ideps = this.initializationDependencies();
    final InterceptionFunction creationFunction =
      ofConstruction(constructorInterceptorMethods,
                     (ignored, argumentsArray) -> {
                       final SequencedSet<Assignment<?>> assignments = new LinkedHashSet<>();
                       int i = 0;
                       for (final AttributedElement pd : pds) {
                         assignments.add(new Assignment<>(pd, argumentsArray[i++]));
                       }
                       for (final AttributedElement idep : ideps) {
                         assignments.add(new Assignment<>(idep, rs.reference(idep.attributedType())));
                       }
                       return this.produce(id, unmodifiableSequencedSet(assignments));
                     });
    return () -> {
      final Object[] arguments = new Object[pds.size()];
      int i = 0;
      for (final AttributedElement pd : pds) {
        arguments[i++] = rs.reference(pd.attributedType());
      }
      return (I)creationFunction.apply(arguments);
    };
  }

  private final Supplier<I> aroundInvokesSupplier(final Supplier<I> s,
                                                  final Collection<? extends ExecutableElement> businessMethods,
                                                  final Function<? super ExecutableElement, List<InterceptorMethod>> f,
                                                  final Id id) {
    if (businessMethods.isEmpty()) {
      return s;
    }
    final Map<ExecutableElement, List<InterceptorMethod>> aroundInvokesByMethod = newHashMap(13);
    for (final ExecutableElement businessMethod : businessMethods) {
      aroundInvokesByMethod.put(businessMethod, f.apply(businessMethod));
    }
    return aroundInvokesByMethod.isEmpty() ? s : () -> this.proxier.interceptionProxy(id, s, aroundInvokesByMethod);
  }

  private final Iterable<Interceptor> interceptors(final Collection<? extends Attributes> bindingsSet,
                                                   final ReferencesSelector rs) {
    if (bindingsSet.isEmpty()) {
      return List.of();
    }
    final List<Attributes> attributes = new ArrayList<>(bindingsSet.size() + 1); // + 1: reserve space for @Any
    attributes.addAll(bindingsSet);
    attributes.add(anyQualifier());
    return rs.references(new AttributedType(this.interceptorType, attributes));
  }


  /*
   * Static methods.
   */


  private static final List<ExecutableElement> constructorsAndMethods(final TypeMirror t) {
    return t instanceof DeclaredType dt ? constructorsAndMethods(dt.asElement()) : List.of();
  }

  private static final List<ExecutableElement> constructorsAndMethods(final Element e) {
    final List<? extends Element> enclosedElements = e.getEnclosedElements();
    if (enclosedElements.isEmpty()) {
      return List.of();
    }
    final List<ExecutableElement> ees = new ArrayList<>(enclosedElements.size());
    for (final Element ee : enclosedElements) {
      switch (ee.getKind()) {
      case CONSTRUCTOR, METHOD -> ees.add((ExecutableElement)ee);
      }
    }
    return ees.isEmpty() ? List.of() : Collections.unmodifiableList(ees);
  }

  private static final Attributes interceptionSpecification(final Collection<? extends Attributes> c) {
    for (final Attributes a : c) {
      if (a.name().equals("InterceptionSpecification")) {
        return a;
      }
    }
    return null;
  }

  private static final Map<ExecutableElement, Set<Attributes>> interceptorBindingsByMethod(final Id id) {
    return interceptorBindingsByMethod(interceptionSpecification(id.attributes()), id.types().get(0));
  }

  private static final Map<ExecutableElement, Set<Attributes>> interceptorBindingsByMethod(final Attributes interceptionSpecification,
                                                                                           final TypeMirror t) {
    return interceptorBindingsByMethod(interceptionSpecification, constructorsAndMethods(t));
  }

  @SuppressWarnings("unchecked")
  private static final Map<ExecutableElement, Set<Attributes>> interceptorBindingsByMethod(final Attributes interceptionSpecification,
                                                                                           final Collection<? extends ExecutableElement> ees) {
    if (interceptionSpecification == null || ees == null || ees.isEmpty() || interceptionSpecification.values().isEmpty()) {
      return Map.of();
    }
    final Map<ExecutableElement, Set<Attributes>> ibsByMethod = newHashMap(13);
    for (final ExecutableElement ee : ees) {
      final StringBuilder sb = new StringBuilder();
      sb.append(ee.getSimpleName().toString());
      final StringJoiner sj = new StringJoiner(", ", "(", ")");
      for (final Element p : ee.getParameters()) {
        sj.add(name(p.asType()));
      }
      sb.append(sj.toString());
      // (sb is now Javadoc-ish: frob(java.lang.String[], java.lang.Object) etc.)
      switch (interceptionSpecification.values().get(sb.toString())) {
      case Attributes a -> ibsByMethod.put(ee, Set.of(a));
      case ArrayValue<?> av -> ibsByMethod.put(ee, Set.copyOf(Arrays.asList(((ArrayValue<Attributes>)av).value())));
      case ListValue<?> lv -> ibsByMethod.put(ee, Set.copyOf(((ListValue<Attributes>)lv).value()));
      default -> {}
      };
    }
    return ibsByMethod.isEmpty() ? Map.of() : Collections.unmodifiableMap(ibsByMethod);
  }

  // Not suitable for general use.
  private static final String name(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> name(((ArrayType)t).getComponentType()) + "[]";
    case BOOLEAN -> "boolean";
    case BYTE -> "byte";
    case CHAR -> "char";
    case DECLARED -> ((QualifiedNameable)((DeclaredType)t).asElement()).getQualifiedName().toString();
    case DOUBLE -> "double";
    case FLOAT -> "float";
    case INT -> "int";
    case INTERSECTION -> name(((IntersectionType)t).getBounds().get(0));
    case LONG -> "long";
    case SHORT -> "short";
    case TYPEVAR -> name(((TypeVariable)t).getUpperBound());
    case VOID -> "void";
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  private static final <I> Supplier<I> postConstructsSupplier(final Supplier<I> s,
                                                              final Collection<? extends ExecutableElement> postConstructMethods,
                                                              final Function<? super ExecutableElement, List<InterceptorMethod>> f) {
    if (postConstructMethods.isEmpty()) {
      return s;
    }
    final List<InterceptorMethod> interceptorMethods = new ArrayList<>();
    for (final ExecutableElement pcm : postConstructMethods) {
      final List<InterceptorMethod> ims = f.apply(pcm);
      if (ims != null) {
        interceptorMethods.addAll(ims);
      }
    }
    if (interceptorMethods.isEmpty()) {
      return s;
    }
    final TargetSupplier<I> targetSupplier = new TargetSupplier<>();
    final Runnable invoker = ofLifecycleEvent(interceptorMethods, targetSupplier, null);
    return () -> {
      final I i = s.get();
      targetSupplier.set(i); // TODO: thread safe?
      invoker.run();
      return i;
    };
  }

  private static final <I> BiConsumer<I, Destruction> preDestroysBiConsumer(final Collection<? extends ExecutableElement> preDestroyMethods,
                                                                            final Function<? super ExecutableElement, List<InterceptorMethod>> f) {
    if (preDestroyMethods.isEmpty()) {
      return null;
    }
    final List<InterceptorMethod> interceptorMethods = new ArrayList<>();
    for (final ExecutableElement pdm : preDestroyMethods) {
      final List<InterceptorMethod> ims = f.apply(pdm);
      if (ims != null) {
        interceptorMethods.addAll(ims);
      }
    }
    if (interceptorMethods.isEmpty()) {
      return null;
    }
    final TargetSupplier<I> targetSupplier = new TargetSupplier<>();
    final Runnable invoker = ofLifecycleEvent(interceptorMethods, targetSupplier, null);
    return (i, d) -> {
      targetSupplier.set(i); // TODO: thread safe?
      invoker.run();
    };
  }


  /*
   * Inner and nested classes.
   */


  private static final class TargetSupplier<I> implements Supplier<I> {

    private volatile I i;

    private TargetSupplier() {
      super();
    }

    @Override // Supplier<I>
    public final I get() {
      return this.i; // volatile read
    }

    private final void set(final I i) {
      this.i = i; // volatile write
    }

  }

}
