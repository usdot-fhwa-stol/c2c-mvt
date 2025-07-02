package usdot.fhwa.stol.c2c.c2c_mvt.standards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.decoders.Decoder;
import usdot.fhwa.stol.c2c.c2c_mvt.parsers.Parser;
import usdot.fhwa.stol.c2c.c2c_mvt.validators.Validator;

import static org.assertj.core.api.Assertions.*;

class C2CMVTStandardsTest {

    private C2CMVTStandards standards;

    @BeforeEach
    void setUp() throws Exception {
        // Use a minimal valid config file placed in src/test/resources/c2c-mvt-test.json
        standards = new C2CMVTStandards(new ClassPathResource("c2c-mvt.json"));
    }

    @Test
    void testGetStandardsAsJsonArray() throws Exception {
        String json = standards.getStandardsAsJsonArray();
        assertThat(json).contains("ngTMDD");
    }

    @Test
    void testGetVersionsAsJsonArray() throws Exception {
        String json = standards.getVersionsAsJsonArray("ngTMDD");
        assertThat(json).contains("1.0");
    }

    @Test
    void testGetEncodingsAsJsonArray() throws Exception {
        String json = standards.getEncodingsAsJsonArray("ngTMDD", "1.0");
        assertThat(json).contains("UTF-8");
    }


    @Test
    void testGetDecoderInstance() throws Exception {
        Decoder<?> decoder = standards.getDecoderInstance("ngTMDD", "1.0");
        assertThat(decoder).isNotNull();
        assertThat(decoder.getClass().getSimpleName()).isEqualTo("JsonDecoder");
    }

    @Test
    void testGetParserInstance() throws Exception {
        Parser<?> parser = standards.getParserInstance("ngTMDD", "1.0");
        assertThat(parser).isNotNull();
        assertThat(parser.getClass().getSimpleName()).isEqualTo("NGTMDDJsonParser");
    }

    @Test
    void testGetValidatorInstance() throws Exception {
        Validator<?> validator = standards.getValidatorInstance("ngTMDD", "1.0");
        assertThat(validator).isNotNull();
        assertThat(validator.getClass().getSimpleName()).isEqualTo("NGTMDDJsonValidator");
    }

    @Test
    void testThrowsExceptionForUnknownStandard() {
        assertThatThrownBy(() -> standards.getVersionsAsJsonArray("unknown"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testThrowsExceptionForUnknownVersion() {
        assertThatThrownBy(() -> standards.getEncodingsAsJsonArray("ngTMDD", "unknown"))
                .isInstanceOf(NullPointerException.class);
    }
}