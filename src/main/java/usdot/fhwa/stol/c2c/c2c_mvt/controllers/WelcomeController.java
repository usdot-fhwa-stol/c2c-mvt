package usdot.fhwa.stol.c2c.c2c_mvt.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author aaron.cherney
 */
@RestController
public class WelcomeController {

	/**
	 *
	 */
	private static final Logger logger = LoggerFactory.getLogger(WelcomeController.class);

	/**
	 *
	 * @return
	 */
	@GetMapping("/welcome")
    public String welcome() {
        logger.debug("Welcome controller initialized and returned message: Welcome to the C2C RI!");
        return "Welcome to the C2C Message Validator!";
    }
}
