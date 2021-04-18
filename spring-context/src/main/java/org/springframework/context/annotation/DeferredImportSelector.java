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

package org.springframework.context.annotation;

import java.util.Objects;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
 * have been processed. This type of selector can be particularly useful when the selected
 * imports are {@code @Conditional}.
 *
 * <p>Implementations can also extend the {@link org.springframework.core.Ordered}
 * interface or use the {@link org.springframework.core.annotation.Order} annotation to
 * indicate a precedence against other {@link DeferredImportSelector DeferredImportSelectors}.
 *
 * <p>Implementations may also provide an {@link #getImportGroup() import group} which
 * can provide additional sorting and filtering logic across different selectors.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
//（延迟的）ImportSelector.DeferredimportSelector 作为 ImportSelector 的变种，它在处理完所有@Configuration Bean 之后才会执行。
// 它对 import加了@Conditional注解的@Configuration的场景中尤其有用，同时该实现类可通过实现 Ordered 接口或标注＠Order的方式调整其优先执行
// 顺序
public interface DeferredImportSelector extends ImportSelector {

	/**
	 * Return a specific import group or {@code null} if no grouping is required.
	 * @return the import group class or {@code null}
	 */
	//获取 所属的 Group类
	@Nullable
	default Class<? extends Group> getImportGroup() {
		return null;
	}


	/**
	 * Interface used to group results from different import selectors.
	 */
	//用来辅助处理 DeferredImportSelector，在原有的DeferredImportSelector上增加了二次处理的能力，处理步骤：
	//1.根据DeferredImportSelector.getImportGroup返回的group类 将 所有DeferredImportSelector分组（返回的group类是同一个类的分在同一组，如果返回的group类是null，则单独分在一组）
	//2.分组执行：
	//  2.1 遍历分组中的所有DeferredImportSelector，并调用group.process(AnnotationMetadata metadata, DeferredImportSelector selector) 方法（首次处理，（内部可以调用 DeferredImportSelector.selectImports方法））
	//	2.2 调用 group.selectImports() 方法 返回 需要导入的Configuration类（二次处理，（内部可以调用 DeferredImportSelector.selectImports方法））
	//注意：对于	DeferredImportSelector.getImportGroup返回null的情况，就是分别调用分组（group为null表示这个DeferredImportSelector自己一个分组
	// ））中的DeferredImportSelector的selectImports方法，并把返回的Configuration类全部返回回去
	interface Group {

		/**
		 * Process the {@link AnnotationMetadata} of the importing @{@link Configuration}
		 * class using the specified {@link DeferredImportSelector}.
		 */
		/**
		 *
		 * @param metadata 导入DeferredImportSelector的Configuration类的注解信息
		 * @param selector 被导入的DeferredImportSelector实例
		 */
		//处理 DeferredImportSelector实例（内部可以调用 DeferredImportSelector.selectImports方法）
		void process(AnnotationMetadata metadata, DeferredImportSelector selector);

		/**
		 * Return the {@link Entry entries} of which class(es) should be imported for this
		 * group.
		 */
		//返回 本group下所有的DeferredImportSelector实例应该导出的 Configuration类（内部可以调用 DeferredImportSelector.selectImports方法）
		Iterable<Entry> selectImports();

		/**
		 * An entry that holds the {@link AnnotationMetadata} of the importing
		 * {@link Configuration} class and the class name to import.
		 */
		class Entry {

			private final AnnotationMetadata metadata;

			private final String importClassName;

			public Entry(AnnotationMetadata metadata, String importClassName) {
				this.metadata = metadata;
				this.importClassName = importClassName;
			}

			/**
			 * Return the {@link AnnotationMetadata} of the importing
			 * {@link Configuration} class.
			 */
			public AnnotationMetadata getMetadata() {
				return this.metadata;
			}

			/**
			 * Return the fully qualified name of the class to import.
			 */
			public String getImportClassName() {
				return this.importClassName;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				Entry entry = (Entry) o;
				return Objects.equals(this.metadata, entry.metadata) &&
						Objects.equals(this.importClassName, entry.importClassName);
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.metadata, this.importClassName);
			}
		}
	}

}
