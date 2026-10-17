package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.application.dto.announcement.AnnouncementImageUploadDto;
import com.undercontroll.application.dto.announcement.CreateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.CreateAnnouncementResponse;
import com.undercontroll.application.dto.announcement.GenerateUploadUrlResponse;
import com.undercontroll.application.dto.announcement.GetPaginatedAnnouncementResponse;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementResponse;
import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.exception.InvalidAnnouncementException;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.usecase.announcement.impl.CreateAnnouncementImpl;
import com.undercontroll.domain.usecase.announcement.impl.GetAnnouncementsImpl;
import com.undercontroll.domain.usecase.announcement.impl.UpdateAnnouncementImpl;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementImageUseCaseTest {

    @Test
    void shouldGenerateKeyAfterCreatingAnnouncementWithImage() {
        AnnouncementGateway announcementGateway = mock(AnnouncementGateway.class);
        StorageGateway storageGateway = mock(StorageGateway.class);
        CreateAnnouncementImpl useCase = new CreateAnnouncementImpl(
                announcementGateway,
                mock(NotificationGateway.class),
                mock(MetricsGateway.class),
                storageGateway
        );

        ReflectionTestUtils.setField(useCase, "bucket", "announcement-bucket");
        ReflectionTestUtils.setField(useCase, "uploadExpirationMinutes", 15);

        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            if (announcement.getId() == null) {
                announcement.setId(9);
            }
            announcement.setPublishedAt(LocalDateTime.now());
            announcement.setUpdatedAt(LocalDateTime.now());
            return announcement;
        });
        when(storageGateway.generatePresignedUploadUrl(eq("announcement-bucket"), any(String.class), eq(15)))
                .thenAnswer(invocation -> new GenerateUploadUrlResponse(
                        "https://s3.example/upload",
                        invocation.getArgument(1),
                        123L
                ));

        CreateAnnouncementResponse output = useCase.execute(new CreateAnnouncementRequest(
                "Title",
                "Content",
                new AnnouncementImageUploadDto("cover.png", "image/png", 1024L),
                AnnouncementType.UPDATES
        ), "token");

        assertThat(output.imageUpload()).isNotNull();
        assertThat(output.imageUpload().fileKey()).startsWith("announcements/9/");
        assertThat(output.imageUpload().fileKey()).endsWith(".png");

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementGateway, times(2)).save(announcementCaptor.capture());
        assertThat(announcementCaptor.getAllValues().get(1).getImageKey()).isEqualTo(output.imageUpload().fileKey());
    }

    @Test
    void shouldNotGenerateUploadUrlWhenCreatingAnnouncementWithoutImage() {
        AnnouncementGateway announcementGateway = mock(AnnouncementGateway.class);
        StorageGateway storageGateway = mock(StorageGateway.class);
        CreateAnnouncementImpl useCase = new CreateAnnouncementImpl(
                announcementGateway,
                mock(NotificationGateway.class),
                mock(MetricsGateway.class),
                storageGateway
        );

        ReflectionTestUtils.setField(useCase, "bucket", "announcement-bucket");
        ReflectionTestUtils.setField(useCase, "uploadExpirationMinutes", 15);

        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setId(10);
            announcement.setPublishedAt(LocalDateTime.now());
            announcement.setUpdatedAt(LocalDateTime.now());
            return announcement;
        });

        CreateAnnouncementResponse output = useCase.execute(new CreateAnnouncementRequest(
                "Title",
                "Content",
                null,
                AnnouncementType.UPDATES
        ), "token");

        assertThat(output.imageUpload()).isNull();
        verify(storageGateway, never()).generatePresignedUploadUrl(any(), any(), any());
    }

    @Test
    void shouldGenerateReadUrlOnlyWhenAnnouncementHasKey() {
        AnnouncementGateway announcementGateway = mock(AnnouncementGateway.class);
        StorageGateway storageGateway = mock(StorageGateway.class);
        GetAnnouncementsImpl useCase = new GetAnnouncementsImpl(announcementGateway, storageGateway);

        ReflectionTestUtils.setField(useCase, "bucket", "announcement-bucket");
        ReflectionTestUtils.setField(useCase, "readExpirationMinutes", 1440L);

        Announcement withImage = Announcement.builder()
                .id(1)
                .title("With image")
                .content("Content")
                .imageKey("announcements/1/cover.png")
                .type(AnnouncementType.UPDATES)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Announcement withoutImage = Announcement.builder()
                .id(2)
                .title("Without image")
                .content("Content")
                .type(AnnouncementType.HOLIDAY)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(announcementGateway.findAllPaginated(0, 10, null))
                .thenReturn(new PaginatedResult<>(List.of(withImage, withoutImage), 2L));
        when(storageGateway.generateReadPresignedUrl("announcement-bucket", "announcements/1/cover.png", 1440L))
                .thenReturn("https://s3.example/read");

        GetPaginatedAnnouncementResponse output = useCase.execute(0, 10, null);

        assertThat(output.announcements()).hasSize(2);
        assertThat(output.announcements().get(0).imageUrl()).isEqualTo("https://s3.example/read");
        assertThat(output.announcements().get(1).imageUrl()).isNull();
        verify(storageGateway).generateReadPresignedUrl("announcement-bucket", "announcements/1/cover.png", 1440L);
    }

    @Test
    void shouldClearKeyWhenRemovingAnnouncementImage() {
        AnnouncementGateway announcementGateway = mock(AnnouncementGateway.class);
        StorageGateway storageGateway = mock(StorageGateway.class);
        UpdateAnnouncementImpl useCase = new UpdateAnnouncementImpl(announcementGateway, storageGateway);

        ReflectionTestUtils.setField(useCase, "bucket", "announcement-bucket");
        ReflectionTestUtils.setField(useCase, "uploadExpirationMinutes", 15);
        ReflectionTestUtils.setField(useCase, "readExpirationMinutes", 1440L);

        Announcement announcement = Announcement.builder()
                .id(3)
                .title("Title")
                .content("Content")
                .imageKey("announcements/3/cover.png")
                .type(AnnouncementType.PROMOTIONS)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(announcementGateway.findById(3)).thenReturn(Optional.of(announcement));
        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAnnouncementResponse output = useCase.execute(3, new UpdateAnnouncementRequest(
                null,
                null,
                null,
                true,
                null
        ));

        assertThat(output.imageUrl()).isNull();
        assertThat(output.imageUpload()).isNull();
        assertThat(announcement.getImageKey()).isNull();
        verify(storageGateway, never()).generateReadPresignedUrl(any(), any(), anyLong());
        verify(storageGateway, never()).generatePresignedUploadUrl(any(), any(), any());
    }

    @Test
    void shouldRejectUploadAndRemoveImageTogether() {
        UpdateAnnouncementImpl useCase = new UpdateAnnouncementImpl(mock(AnnouncementGateway.class), mock(StorageGateway.class));

        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                null,
                null,
                new AnnouncementImageUploadDto("cover.png", "image/png", 1024L),
                true,
                null
        );
        assertThatThrownBy(() -> useCase.execute(1, request)).isInstanceOf(InvalidAnnouncementException.class);
    }
}
