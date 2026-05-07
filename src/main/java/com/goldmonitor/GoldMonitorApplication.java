package com.goldmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class GoldMonitorApplication {

    static {
        String dbPath = System.getenv("DB_PATH");
        if (dbPath == null) dbPath = "./data/gold-monitor.db";
        try {
            Path dir = Paths.get(dbPath).getParent();
            if (dir != null) Files.createDirectories(dir);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SpringApplication.run(GoldMonitorApplication.class, args);
    }
}
