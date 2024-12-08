package LinkerBell.campus_market_spring.global.config;

import LinkerBell.campus_market_spring.domain.Blacklist;
import LinkerBell.campus_market_spring.repository.BlacklistRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SchedulerConfig {

    private final BlacklistRepository blacklistRepository;

    @Scheduled(cron = "${cron.recovery}")
    public void recoveryUser() {
        LocalDateTime now = LocalDateTime.now();
        List<Blacklist> blacklists = blacklistRepository.findAll();
        List<Blacklist> userList = blacklists.stream().filter(blacklist -> {
            return blacklist.getEndDate().isBefore(now);
        }).toList();
        blacklistRepository.deleteAll(userList);
    }
}
