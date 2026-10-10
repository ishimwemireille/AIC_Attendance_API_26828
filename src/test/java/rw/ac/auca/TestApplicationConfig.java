package rw.ac.auca;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * The Class TestApplicationConfig.
 *
 * The application class lives in rw.ac.auca.aicattendancesystem, a sibling of
 * the packages the tests live in, so a slice test searching upwards for a
 * @SpringBootConfiguration never reaches it. This class sits at the common root
 * so the search succeeds.
 *
 * It deliberately scans nothing. Each slice brings only the components it
 * needs: the web test scans the controllers, while @DataJpaTest finds the
 * entities and repositories through this class's package. Scanning here would
 * leak controllers into the persistence slice, where their services do not
 * exist.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestApplicationConfig {
}
