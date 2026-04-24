package com.euprocuro.api.application.usecase;

import com.euprocuro.api.application.view.PersonalDashboardView;

public interface DashboardUseCase {
    PersonalDashboardView getDashboard(String userId);
}
