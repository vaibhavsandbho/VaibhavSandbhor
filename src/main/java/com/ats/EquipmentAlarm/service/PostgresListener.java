package com.ats.EquipmentAlarm.service;


import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresListener {

    private final DataSource dataSource;
    private final AlarmSseService sseService;

    @PostConstruct
    public void listen() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (Connection conn = dataSource.getConnection()) {

                PGConnection pgConn = conn.unwrap(PGConnection.class);
                Statement stmt = conn.createStatement();
                stmt.execute("LISTEN alarm_channel");

                while (true) {
                    PGNotification[] notifications = pgConn.getNotifications();
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            sseService.push(notification.getParameter());
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
