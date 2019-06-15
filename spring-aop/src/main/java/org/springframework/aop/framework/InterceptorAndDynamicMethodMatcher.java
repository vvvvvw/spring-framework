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

package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.MethodMatcher;

/**
 * Internal framework class, combining a MethodInterceptor instance
 * with a MethodMatcher for use as an element in the advisor chain.
 *
 * @author Rod Johnson
 */

/*
<aop:config>
    <aop:aspect ref="aspectTest">
        <aop:pointcut id="test" expression="execution(* com.demo.TestPoint.test())"/>
        <aop:before method="doBefore" pointcut-ref="test"/>
        <aop:after-returning method="doAfter" pointcut-ref="test"/>
    </aop:aspect>
</aop:config>
 */
//　如果，我们不是通过定义advisor通知器的方式，而是直接定义一个切面，那么，在我们定义切面这个类是是不需要实现任何接口的，
// 其中的任意方法都可以作为前置或者后置通知，这取决于你的xml配置
class InterceptorAndDynamicMethodMatcher {

	//方法拦截器
	final MethodInterceptor interceptor;

	//匹配器
	final MethodMatcher methodMatcher;

	public InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
		this.interceptor = interceptor;
		this.methodMatcher = methodMatcher;
	}

}
