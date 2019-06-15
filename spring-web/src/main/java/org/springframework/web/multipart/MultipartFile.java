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

package org.springframework.web.multipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * A representation of an uploaded file received in a multipart request.
 *
 * <p>The file contents are either stored in memory or temporarily on disk.
 * In either case, the user is responsible for copying file contents to a
 * session-level or persistent store as and if desired. The temporary storage
 * will be cleared at the end of request processing.
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see org.springframework.web.multipart.MultipartHttpServletRequest
 * @see org.springframework.web.multipart.MultipartResolver
 */
//封装了请求数据中的文件，此时这个文件存储在内存中或临时的磁盘文件中，
// 需要将其转存到一个合适的位置，因为请求结束后临时存储将被清空
public interface MultipartFile extends InputStreamSource {

	/**
	 * Return the name of the parameter in the multipart form.
	 * @return the name of the parameter (never {@code null} or empty)
	 */
	// 获取参数的名称
	String getName();

	/**
	 * Return the original filename in the client's filesystem.
	 * <p>This may contain path information depending on the browser used,
	 * but it typically will not with any other than Opera.
	 * @return the original filename, or the empty String if no file has been chosen
	 * in the multipart form, or {@code null} if not defined or not available
	 * @see org.apache.commons.fileupload.FileItem#getName()
	 * @see org.springframework.web.multipart.commons.CommonsMultipartFile#setPreserveFilename
	 */
	// 获取文件的原名称
	@Nullable
	String getOriginalFilename();

	/**
	 * Return the content type of the file.
	 * @return the content type, or {@code null} if not defined
	 * (or no file has been chosen in the multipart form)
	 */
	// 文件内容的类型
	@Nullable
	String getContentType();

	/**
	 * Return whether the uploaded file is empty, that is, either no file has
	 * been chosen in the multipart form or the chosen file has no content.
	 */
	// 文件是否为空
	boolean isEmpty();

	/**
	 * Return the size of the file in bytes.
	 * @return the size of the file, or 0 if empty
	 */
	// 文件大小
	long getSize();

	/**
	 * Return the contents of the file as an array of bytes.
	 * @return the contents of the file as bytes, or an empty byte array if empty
	 * @throws IOException in case of access errors (if the temporary store fails)
	 */
	// 将文件内容以字节数组的形式返回
	byte[] getBytes() throws IOException;

	/**
	 * Return an InputStream to read the contents of the file from.
	 * <p>The user is responsible for closing the returned stream.
	 * @return the contents of the file as stream, or an empty stream if empty
	 * @throws IOException in case of access errors (if the temporary store fails)
	 */
	// 将文件内容以输入流的形式返回
	@Override
	InputStream getInputStream() throws IOException;

	/**
	 * Return a Resource representation of this MultipartFile. This can be used
	 * as input to the {@code RestTemplate} or the {@code WebClient} to expose
	 * content length and the filename along with the InputStream.
	 * @return this MultipartFile adapted to the Resource contract
	 * @since 5.1
	 */
	default Resource getResource() {
		return new MultipartFileResource(this);
	}

	/**
	 * Transfer the received file to the given destination file.
	 * <p>This may either move the file in the filesystem, copy the file in the
	 * filesystem, or save memory-held contents to the destination file. If the
	 * destination file already exists, it will be deleted first.
	 * <p>If the target file has been moved in the filesystem, this operation
	 * cannot be invoked again afterwards. Therefore, call this method just once
	 * in order to work with any storage mechanism.
	 * <p><b>NOTE:</b> Depending on the underlying provider, temporary storage
	 * may be container-dependent, including the base directory for relative
	 * destinations specified here (e.g. with Servlet 3.0 multipart handling).
	 * For absolute destinations, the target file may get renamed/moved from its
	 * temporary location or newly copied, even if a temporary copy already exists.
	 * @param dest the destination file (typically absolute)
	 * @throws IOException in case of reading or writing errors
	 * @throws IllegalStateException if the file has already been moved
	 * in the filesystem and is not available anymore for another transfer
	 * @see org.apache.commons.fileupload.FileItem#write(File)
	 * @see javax.servlet.http.Part#write(String)
	 */
	// 将文件内容传输到指定文件中
	void transferTo(File dest) throws IOException, IllegalStateException;

	/**
	 * Transfer the received file to the given destination file.
	 * <p>The default implementation simply copies the file input stream.
	 * @since 5.1
	 * @see #getInputStream()
	 * @see #transferTo(File)
 	 */
	default void transferTo(Path dest) throws IOException, IllegalStateException {
		FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
	}

}
