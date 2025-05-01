package usdot.fhwa.stol.c2c.c2c_mvt;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Default Servlet Initializer for Spring Boot application
 * @author Aaron Cherney
 */
public class ServletInitializer extends SpringBootServletInitializer {

	/**
	 * Default configure method for Spring Boot Application
	 * @param application 
	 * @return
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(C2CMVTApplication.class);
	}

}
