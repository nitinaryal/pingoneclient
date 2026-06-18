package com.pingone.oidc.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PingOneApplicationTypeConverterTest {

    private final PingOneApplicationTypeConverter converter = new PingOneApplicationTypeConverter();

    @Test
    void convertsKebabCaseConfigValue() {
        assertThat(converter.convert("oidc-web-app")).isEqualTo(PingOneApplicationType.OIDC_WEB_APP);
    }

    @Test
    void convertsEnumName() {
        assertThat(converter.convert("WORKER")).isEqualTo(PingOneApplicationType.WORKER);
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> converter.convert("unknown-type"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
