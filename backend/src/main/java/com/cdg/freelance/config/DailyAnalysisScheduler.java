package com.cdg.freelance.config;

import com.cdg.freelance.service.AnalyseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DailyAnalysisScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailyAnalysisScheduler.class);
    private final AnalyseService analyseService;

    public DailyAnalysisScheduler(AnalyseService analyseService) {
        this.analyseService = analyseService;
    }

    @Scheduled(cron = "${application.analytics.daily-cron:0 0 2 * * *}")
    public void runDailyAnalysis() {
        Map<String, Object> result = analyseService.runDailyAnalysis();
        logger.info("Analyse quotidienne terminee : {}", result);
    }
}
