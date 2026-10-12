package com.nikhil.patientservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Risk scores are cached (Redis in Docker/Kubernetes, in-memory in tests) and evicted when new vitals arrive. */
@Configuration
@EnableCaching
public class CacheConfig {
}
