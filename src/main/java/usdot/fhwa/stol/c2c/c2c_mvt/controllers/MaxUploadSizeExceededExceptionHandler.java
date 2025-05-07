/*
 * Copyright (C) 2025 LEIDOS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Exception Handler for MaxUploadSizeExceededExceptions. Needed to be able to
 * catch the Exception display it to the user
 * @author Aaron Cherney
 */
@ControllerAdvice
public class MaxUploadSizeExceededExceptionHandler 
{

	/**
	 * File size limit for uploads, defined in src/main/resources/application.properties
	 */
	@Value("${spring.servlet.multipart.max-file-size}")
	private String maxSize;

	/**
	 * HashSet used to determine if multiple parts of a file upload request are
	 * for the same file. Stores the last modified time stamp of the request
	 */
	public final static HashSet<Long> requestTimes = new HashSet();

	/**
	 * This method handles {@link MaxUploadSizeExceededExpection}s by logging
	 * the exception through {@link StandardValidationController} so the user
	 * will be able to see the reason why the upload failed.
	 * @param exc the Exception
	 * @param request Request that contains the file upload
	 * @param response Response
	 * @return
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxSizeException(
      MaxUploadSizeExceededException exc, 
      HttpServletRequest request,
      HttpServletResponse response) 
	{
		synchronized (requestTimes) // only log the exception once, since files are uploaded in chunks each file will trigger the Exception multiple times
		{
			if (requestTimes.add(request.getDateHeader("Last-Modified")))
				StandardValidationController.logException(exc, "Upload failed: Files cannot be larger than " + maxSize, null);
		}
		
        ModelAndView modelAndView = new ModelAndView("file");
        modelAndView.getModel().put("message", "File too large!");
        return modelAndView;
    }	
}
