/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop;

/**
 * AOP代理接口的标记（特别是：introduction这个接口），含义是明确地打算返回原始目标对象
 * （当从方法调用返回时通常会被代理对象替换）。请注意，这是{@link java.io.Serializable}样式的标记接口，
 * 在语义上应用于声明的接口而不是具体对象的完整类。 换句话说，此标记仅适用于特定接口（通常不作为AOP代理的主接口，
 * 比如introduction 接口），
 * 因此不会影响具体AOP代理可能实现的其他接口。
 * 当目标对象实现本接口时，如果目标对象的方法返回值为目标对象，则替换为接口
 */

/**
 * Marker for AOP proxy interfaces (in particular: introduction interfaces)
 * that explicitly intend to return the raw target object (which would normally
 * get replaced with the proxy object when returned from a method invocation).
 *
 * <p>Note that this is a marker interface in the style of {@link java.io.Serializable},
 * semantically applying to a declared interface rather than to the full class
 * of a concrete object. In other words, this marker applies to a particular
 * interface only (typically an introduction interface that does not serve
 * as the primary interface of an AOP proxy), and hence does not affect
 * other interfaces that a concrete AOP proxy may implement.
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 * @see org.springframework.aop.scope.ScopedObject
 */
public interface RawTargetAccess {

}
