package usdot.fhwa.stol.c2cri.c2cri_mvt.controllers;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class WelcomeTestsTest {

    @Mock
    private WelcomeController welcomeService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testWelcomeApi() {
        String expectedResponse = "Welcome to the C2C RI!";
        when(welcomeService.welcome()).thenReturn(expectedResponse);
    }
}
