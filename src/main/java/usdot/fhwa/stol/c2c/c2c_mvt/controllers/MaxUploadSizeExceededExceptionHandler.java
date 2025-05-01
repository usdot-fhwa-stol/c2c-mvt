/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author Federal Highway Administration
 */
@ControllerAdvice
public class MaxUploadSizeExceededExceptionHandler 
{

	/**
	 * File size limit for uploads, defined in src/main/resources/application.properties
	 */
	@Value("${spring.servlet.multipart.max-file-size}")
	private String MAXSIZE;

	/**
	 * HashSet used to determine if multiple parts of a file upload request are
	 * for the same file. Stores the last modified time stamp of the request
	 */
	public final static HashSet<Long> m_oRequestTimes = new HashSet();

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
		synchronized (m_oRequestTimes) // only log the exception once, since files are uploaded in chunks each file will trigger the Exception multiple times
		{
			if (m_oRequestTimes.add(request.getDateHeader("Last-Modified")))
				StandardValidationController.logException(exc, "Upload failed: Files cannot be larger than " + MAXSIZE);
		}
		
        ModelAndView modelAndView = new ModelAndView("file");
        modelAndView.getModel().put("message", "File too large!");
        return modelAndView;
    }	
}
