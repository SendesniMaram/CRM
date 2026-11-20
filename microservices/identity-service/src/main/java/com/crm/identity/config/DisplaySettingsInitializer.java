package com.crm.identity.config;
import org.springframework.context.annotation.*;import org.springframework.boot.ApplicationRunner;import com.crm.identity.repository.DisplaySettingsRepository;import com.crm.identity.entity.DisplaySettings;
@Configuration public class DisplaySettingsInitializer {@Bean ApplicationRunner displaySettingsDefaults(DisplaySettingsRepository repository){return args->{if(!repository.existsById(1L))repository.saveAndFlush(new DisplaySettings());};}}
