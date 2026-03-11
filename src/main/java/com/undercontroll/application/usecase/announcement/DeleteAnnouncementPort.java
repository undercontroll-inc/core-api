package com.undercontroll.application.usecase.announcement;

public interface DeleteAnnouncementPort {
    record Input(Integer id) {}

    void execute(Input input);
}
