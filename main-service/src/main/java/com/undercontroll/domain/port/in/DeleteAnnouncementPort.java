package com.undercontroll.domain.port.in;

public interface DeleteAnnouncementPort {
    record Input(Integer id) {}

    void execute(Input input);
}
