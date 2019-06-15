/*
 * Copyright 2002-2018 the original author or authors.
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

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 * <p>
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 * <p>
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 * <p>
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 * <p>
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
//JDK动态代理需要实现 InvocationHandler类
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/**
	 * use serialVersionUID from Spring 1.2 for interoperability.
	 */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: We could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring "invoke" into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance. (We have a good test suite to ensure that the different
	 * proxies behave the same :-)
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 */

	/**
	 * We use a static Log to avoid serialization issues.
	 */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/** Config used to configure this proxy. */
	/**
	 * 代理类的相关配置，这个类继承自ProxyConfig，都是代理类的相关配置
	 */
	private final AdvisedSupport advised;


	/*
	todo 通常情况下，Spring AOP代理对象不会对equals和hashCode方法增强
	，注意这是在通常情况下，那什么是“通常情况”，什么又是“不通常情况”呢？
　　如果目标对象直接重写Object对象的equals或hashCode方法，此时Spring AOP则
不会对它增强，equalsDefined=false或hashCodeDefined=false；如果目标对象实现的
接口定义了equals或hashCode方法，此时Spring AOP则会对它增强，
equalsDefined=true或hashCodeDefined=true。所以“通常情况”就是我们并不会在接口
定义equals或hashCode方法，“不通常情况”就是在有的特殊情况下在接口
定义equals或hashCode方法。再换句话说，如果我们需要Spring AOP增强equals或hashCode方法
则必须要在其目标对象的实现接口定义equals或hashCode方法。
	 */
	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 */
	/**
	 * 用于判断目标对象实现的接口是否定义了equals方法
	 */
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 */
	/**
	 * 用于判断目标对象实现的接口是否定义了hashCode方法
	 */
	private boolean hashCodeDefined;


	/**
	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
	 *
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 *                            exception in this case, rather than let a mysterious failure happen later.
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		////只有一个带AdvisedSupport类型的构造方法，这个类型上面提到过是生成代理类的相关配置，
		// 必须不能为空，否则将抛出参数异常的错误
		Assert.notNull(config, "AdvisedSupport must not be null");
		////AdvisedSupport配置类中需要定义通知器和目标源。
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		//赋值给成员变量
		this.advised = config;
	}


	@Override
	public Object getProxy() {
		// 没有指定ClassLoader则通过Thread.currentThread.getContextClassLoader()获取
		// 当前线程上下文的类加载器。
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}
		//获取代理对象需要实现的完整接口
		Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		//这里就是上面提到过的判断的目标对象实现的接口是否定义了equals或hashCode方法，
		findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
		/*
		/通过JDK生成代理对象。第一个ClassLoader代表创建类的类加载器，第二个
		表示需要被代理的目标对象的接口，第三个参数InvocationHandler叫做调用处理器，
		在这里它就是对象本身，调用的代理对象方法实际就是调用InvocationHandler接口中
		的invoke方法。
		 */
		return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
	}

	/**
	 * Finds any {@link #equals} or {@link #hashCode} method that may be defined
	 * on the supplied set of interfaces.
	 *
	 * @param proxiedInterfaces the interfaces to introspect
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 */
	/**
	 * @param proxy  指的是我们生成的代理对象；
	 * @param method 指的是我们所代理的那个真实对象的某个方法的Method对象；
	 * @param args   指的是调用那个真实对象方法的参数。
	 * @return
	 * @throws Throwable
	 */
	//代理对象的回调方法
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		MethodInvocation invocation;
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Object target = null;

		//对一些情况的特殊判断，主要是不对目标对象应用切面
		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// 这里就是之前提到过的，“通常情况”Spring AOP不会对equals方法进行拦截增强，
				// 所以这里判断如果目标对象没有定义equals方法的话，就会直接调用JdkDynamicAopProxy的equals方法而不会增强
				// The target does not implement the equals(Object) method itself.
				return equals(args[0]);
			} else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// 这里就是之前提到过的，“通常情况”Spring AOP不会对hashCode方法进行拦截增强，
				// 所以这里判断如果目标对象没有定义hashCode方法的话，就会直接调用JdkDynamicAopProxy的hashCode方法而不会增强
				// The target does not implement the hashCode() method itself.
				return hashCode();
			}
			//如果是getDecoratedClass方法，返回代理背后最终真正的目标对象（如果是多级代理）
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				//todo 2019/4/10 15258 // 这里是上面的疑点，也是Spring4.3新出现的特性
				return AopProxyUtils.ultimateTargetClass(this.advised);
			} else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// 这个地方就有点意思了，Spring AOP不会增强直接实现Advised接口的目标对象，再重复一次，
				// 也就是说如果目标对象实现的Advised接口，则不会对其应用切面进行方法的增强。
				//这个方法是一个对Java通过反射调用方法的封装>>去除了访问控制权限并执行
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			//方法的返回值
			Object retVal;

			////是否暴露代理对象，默认false可配置为true，如果暴露就意味着允许在线程内共享代理对象，注意这是在线程内，
			// 也就是说同一线程的任意地方都能通过AopContext获取该代理对象，这应该算是比较高级一点的用法了。
			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			//通过目标源获取目标对象。获取拦截器链，并调用增强方法及目标对象的方法
			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			//优化>>尽可能减少我们持有 target的时间，万一这个对象是从对象池中获取的
			target = targetSource.getTarget();
			Class<?> targetClass = (target != null ? target.getClass() : null);

			//获取拦截器链，相当于是获取潜在的增强方法，只是潜在，后续还有匹配的判断；
			// Get the interception chain for this method.
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			//拦截器链如果为空的话就直接调用目标对象的方法。
			// Check whether we have any advice. If we don't, we can fallback on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				//适配vargrgs参数
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				//直接调用目标对象的方法
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			} else {
				//通过ReflectiveMethodInvocation.proceed调用拦截器中的方法和目标对象方法
				// We need to create a method invocation...
				invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				//ReflectiveMethodInvocation对象完成对AOP功能实现的封装
				retVal = invocation.proceed(); //获取返回值
			}

			//获取返回值类型
			// Massage return value if necessary.
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// 一些列的判断条件，如果返回值不为空，且为目标对象的话，就直接将目标对象赋值给retVal
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				retVal = proxy;
			}
			//如果返回值为null，但是方法返回值是原始类型，不能转换
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		} finally {
			//如果targetSource是动态的
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				//释放targetSource
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				// Restore old proxy.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 */
	//代理类的equals方法
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			//如果other是JdkDynamicAopProxy
			otherProxy = (JdkDynamicAopProxy) other;
		} else if (Proxy.isProxyClass(other.getClass())) {
			//如果other是 jdk动态代理类
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				////如果other是 jdk动态代理类，但不是spring生成的JdkDynamicAopProxy代理类，返回false
				return false;
			}
			//否则，提取出 JdkDynamicAopProxy
			otherProxy = (JdkDynamicAopProxy) ih;
		} else {
			// Not a valid comparison...
			return false;
		}


		//返回 this和other的this.advised(包括实现的)
		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	//使用JdkDynamicAopProxy.class的hashCode 和目标对象的hashCode，所以 同一个对象和 代理对象 是不一样的
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
