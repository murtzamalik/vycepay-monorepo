package com.vycepay.common.apprelease;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppReleaseDecisionTest {

    private static AppReleasePolicy policy(int min, int latest, boolean maintenance) {
        return new AppReleasePolicy(
                "ANDROID", min, latest, "1.0.0",
                "Update available", "Please update",
                "https://play.google.com/store/apps/details?id=com.wallet.vycepay",
                null, maintenance, null, true);
    }

    @Test
    void missingVersionCode_noUpdateFlags() {
        AppReleaseDecision d = AppReleaseDecision.from(policy(5, 10, false), null);
        assertThat(d.updateRequired()).isFalse();
        assertThat(d.updateRecommended()).isFalse();
        assertThat(d.maintenance()).isFalse();
    }

    @Test
    void belowMin_forceAndRecommend() {
        AppReleaseDecision d = AppReleaseDecision.from(policy(5, 10, false), 4);
        assertThat(d.updateRequired()).isTrue();
        assertThat(d.updateRecommended()).isTrue();
    }

    @Test
    void betweenMinAndLatest_softOnly() {
        AppReleaseDecision d = AppReleaseDecision.from(policy(5, 10, false), 7);
        assertThat(d.updateRequired()).isFalse();
        assertThat(d.updateRecommended()).isTrue();
    }

    @Test
    void atOrAboveLatest_ok() {
        AppReleaseDecision d = AppReleaseDecision.from(policy(5, 10, false), 10);
        assertThat(d.updateRequired()).isFalse();
        assertThat(d.updateRecommended()).isFalse();
    }

    @Test
    void maintenance_overridesVersion() {
        AppReleaseDecision d = AppReleaseDecision.from(policy(5, 10, true), 1);
        assertThat(d.maintenance()).isTrue();
        assertThat(d.updateRequired()).isFalse();
        assertThat(d.updateRecommended()).isFalse();
    }
}
