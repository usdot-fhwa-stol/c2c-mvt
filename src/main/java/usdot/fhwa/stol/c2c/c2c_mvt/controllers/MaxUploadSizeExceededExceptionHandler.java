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
	@Value("${spring.servlet.multipart.max-file-size}")
	private String MAXSIZE;
	public final static HashSet<Long> m_oRequestTimes = new HashSet();
	@ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxSizeException(
      MaxUploadSizeExceededException exc, 
      HttpServletRequest request,
      HttpServletResponse response) 
	{
		synchronized (m_oRequestTimes)
		{
			if (m_oRequestTimes.add(request.getDateHeader("Last-Modified")))
				StandardValidationController.logException(exc, "Upload failed: Files cannot be larger than " + MAXSIZE);
		}
		
        ModelAndView modelAndView = new ModelAndView("file");
        modelAndView.getModel().put("message", "File too large!");
        return modelAndView;
    }	
}
