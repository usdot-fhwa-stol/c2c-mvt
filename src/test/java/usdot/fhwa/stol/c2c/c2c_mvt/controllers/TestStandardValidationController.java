/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.controllers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author Federal Highway Administration
 */
public class TestStandardValidationController 
{

	/**
	 *
	 */
	@InjectMocks
    private StandardValidationController m_oSVC;
	
	/**
	 *
	 */
	@BeforeEach
    public void setUp() 
	{
        MockitoAnnotations.openMocks(this);
		m_oSVC.init();
    }
}
