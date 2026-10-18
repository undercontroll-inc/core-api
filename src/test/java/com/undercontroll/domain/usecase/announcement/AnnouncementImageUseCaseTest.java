package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.event.AnnouncementCreatedEvent;
import com.undercontroll.domain.exception.AnnouncementNotFoundException;
import com.undercontroll.domain.exception.InvalidAnnouncementException;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.model.PresignedUpload;
import com.undercontroll.domain.usecase.announcement.impl.CreateAnnouncementImpl;
import com.undercontroll.domain.usecase.announcement.impl.DeleteAnnouncementImpl;
import com.undercontroll.domain.usecase.announcement.impl.GetAnnouncementsImpl;
import com.undercontroll.domain.usecase.announcement.impl.GetLastAnnouncementImpl;
import com.undercontroll.domain.usecase.announcement.impl.UpdateAnnouncementImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnouncementImageUseCaseTest {

    private final AnnouncementGateway announcementGateway = mock(AnnouncementGateway.class);
    private final StorageGateway storageGateway = mock(StorageGateway.class);

    @Test
    void shouldGenerateKeyAfterCreatingAnnouncementWithImage() {
        NotificationGateway notificationGateway = mock(NotificationGateway.class);
        CreateAnnouncementImpl useCase = new CreateAnnouncementImpl(
                announcementGateway,
                notificationGateway,
                mock(MetricsGateway.class),
                storageGateway
        );

        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            if (announcement.getId() == null) {
                announcement.setId(9);
            }
            announcement.setPublishedAt(LocalDateTime.now());
            announcement.setUpdatedAt(LocalDateTime.now());
            return announcement;
        });
        when(storageGateway.presignAnnouncementImageUpload(any(String.class)))
                .thenAnswer(invocation -> new PresignedUpload("https://s3.example/upload", invocation.getArgument(0), 123L));

        CreateAnnouncementPort.Output output = useCase.execute(new CreateAnnouncementPort.Input(
                "Title",
                "Content",
                new AnnouncementImageUpload("cover.png", "image/png", 1024L),
                AnnouncementType.UPDATES
        ), "token");

        assertThat(output.imageUpload()).isNotNull();
        assertThat(output.imageUpload().fileKey()).startsWith("announcements/9/");
        assertThat(output.imageUpload().fileKey()).endsWith(".png");

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementGateway, times(2)).save(announcementCaptor.capture());
        assertThat(announcementCaptor.getAllValues().get(1).getImageKey()).isEqualTo(output.imageUpload().fileKey());
        ArgumentCaptor<AnnouncementCreatedEvent> eventCaptor = ArgumentCaptor.forClass(AnnouncementCreatedEvent.class);
        verify(notificationGateway).handleAnnouncementCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().token()).isEqualTo("token");
    }

    @Test
    void shouldNotGenerateUploadUrlWhenCreatingAnnouncementWithoutImage() {
        CreateAnnouncementImpl useCase = new CreateAnnouncementImpl(
                announcementGateway,
                mock(NotificationGateway.class),
                mock(MetricsGateway.class),
                storageGateway
        );

        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setId(10);
            return announcement;
        });

        CreateAnnouncementPort.Output output = useCase.execute(new CreateAnnouncementPort.Input(
                "Title",
                "Content",
                null,
                AnnouncementType.UPDATES
        ), "token");

        assertThat(output.imageUpload()).isNull();
        verify(storageGateway, never()).presignAnnouncementImageUpload(any());
    }

    @Test
    void shouldGenerateReadUrlOnlyWhenAnnouncementHasKey() {
        GetAnnouncementsImpl useCase = new GetAnnouncementsImpl(announcementGateway, storageGateway);

        Announcement withImage = Announcement.builder()
                .id(1)
                .title("With image")
                .content("Content")
                .imageKey("announcements/1/cover.png")
                .type(AnnouncementType.UPDATES)
                .build();
        Announcement withoutImage = Announcement.builder()
                .id(2)
                .title("Without image")
                .content("Content")
                .imageKey(" ")
                .type(AnnouncementType.HOLIDAY)
                .build();

        when(announcementGateway.findAllPaginated(0, 10, null))
                .thenReturn(new PaginatedResult<>(List.of(withImage, withoutImage), 12L));
        when(storageGateway.announcementImageReadUrl("announcements/1/cover.png"))
                .thenReturn("https://s3.example/read");

        GetAnnouncementsPort.Output output = useCase.execute(0, 10, null);

        assertThat(output.announcements()).hasSize(2);
        assertThat(output.announcements().get(0).imageUrl()).isEqualTo("https://s3.example/read");
        assertThat(output.announcements().get(1).imageUrl()).isNull();
        assertThat(output.totalPages()).isEqualTo(2);
        verify(storageGateway).announcementImageReadUrl("announcements/1/cover.png");
    }

    @Test
    void shouldReturnLatestAnnouncementWithReadUrl() {
        GetLastAnnouncementImpl useCase = new GetLastAnnouncementImpl(announcementGateway, storageGateway);
        when(announcementGateway.findLastAnnouncement()).thenReturn(Optional.of(Announcement.builder()
                .id(4)
                .title("Feriado")
                .imageKey("announcements/4/a.png")
                .build()));
        when(storageGateway.announcementImageReadUrl("announcements/4/a.png")).thenReturn("https://s3.example/a");

        Optional<AnnouncementOutput> output = useCase.execute();

        assertThat(output).isPresent();
        assertThat(output.get().title()).isEqualTo("Feriado");
        assertThat(output.get().imageUrl()).isEqualTo("https://s3.example/a");
    }

    @Test
    void shouldClearKeyWhenRemovingAnnouncementImage() {
        UpdateAnnouncementImpl useCase = new UpdateAnnouncementImpl(announcementGateway, storageGateway);

        Announcement announcement = Announcement.builder()
                .id(3)
                .title("Title")
                .content("Content")
                .imageKey("announcements/3/cover.png")
                .type(AnnouncementType.PROMOTIONS)
                .build();

        when(announcementGateway.findById(3)).thenReturn(Optional.of(announcement));
        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAnnouncementPort.Output output = useCase.execute(3, new UpdateAnnouncementPort.Input(
                null,
                null,
                null,
                true,
                null
        ));

        assertThat(output.imageUrl()).isNull();
        assertThat(output.imageUpload()).isNull();
        assertThat(announcement.getImageKey()).isNull();
        verify(storageGateway, never()).announcementImageReadUrl(any());
        verify(storageGateway, never()).presignAnnouncementImageUpload(any());
    }

    @Test
    void shouldReplaceImageAndFieldsOnUpdate() {
        UpdateAnnouncementImpl useCase = new UpdateAnnouncementImpl(announcementGateway, storageGateway);
        Announcement announcement = Announcement.builder().id(5).title("Old").content("Old").type(AnnouncementType.UPDATES).build();
        when(announcementGateway.findById(5)).thenReturn(Optional.of(announcement));
        when(announcementGateway.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageGateway.presignAnnouncementImageUpload(any(String.class)))
                .thenAnswer(invocation -> new PresignedUpload("https://s3.example/put", invocation.getArgument(0), 1L));
        when(storageGateway.announcementImageReadUrl(any(String.class))).thenReturn("https://s3.example/get");

        UpdateAnnouncementPort.Output output = useCase.execute(5, new UpdateAnnouncementPort.Input(
                "New",
                "Body",
                new AnnouncementImageUpload("photo", "image/webp", 10L),
                false,
                AnnouncementType.HOLIDAY
        ));

        assertThat(output.title()).isEqualTo("New");
        assertThat(output.content()).isEqualTo("Body");
        assertThat(output.type()).isEqualTo(AnnouncementType.HOLIDAY);
        assertThat(output.imageUpload().fileKey()).startsWith("announcements/5/").endsWith(".webp");
        assertThat(output.imageUrl()).isEqualTo("https://s3.example/get");
        assertThat(announcement.getImageKey()).isEqualTo(output.imageUpload().fileKey());
    }

    @Test
    void shouldFailUpdateWhenAnnouncementDoesNotExist() {
        UpdateAnnouncementImpl useCase = new UpdateAnnouncementImpl(announcementGateway, storageGateway);
        when(announcementGateway.findById(99)).thenReturn(Optional.empty());
        UpdateAnnouncementPort.Input input = new UpdateAnnouncementPort.Input("t", null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(99, input)).isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void shouldRejectUploadAndRemoveImageTogether() {
        UpdateAnnouncementImpl useCase = new UpdateAnnouncementImpl(announcementGateway, storageGateway);

        UpdateAnnouncementPort.Input request = new UpdateAnnouncementPort.Input(
                null,
                null,
                new AnnouncementImageUpload("cover.png", "image/png", 1024L),
                true,
                null
        );
        assertThatThrownBy(() -> useCase.execute(1, request)).isInstanceOf(InvalidAnnouncementException.class);
    }

    @Test
    void shouldDeleteExistingAnnouncementOrFail() {
        DeleteAnnouncementImpl useCase = new DeleteAnnouncementImpl(announcementGateway);
        when(announcementGateway.findById(1)).thenReturn(Optional.of(Announcement.builder().id(1).build()));
        when(announcementGateway.findById(2)).thenReturn(Optional.empty());

        useCase.execute(1);

        verify(announcementGateway).deleteById(1);
        assertThatThrownBy(() -> useCase.execute(2)).isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void shouldResolveImageExtensionFromNameOrContentType() {
        assertThat(AnnouncementImageKeys.resolveExtension("cover.PNG", null)).isEqualTo("png");
        assertThat(AnnouncementImageKeys.resolveExtension("cover.", "image/jpeg")).isEqualTo("jpg");
        assertThat(AnnouncementImageKeys.resolveExtension(null, "image/gif")).isEqualTo("gif");
        assertThat(AnnouncementImageKeys.resolveExtension("", "application/pdf")).isEqualTo("bin");
        assertThat(AnnouncementImageKeys.resolveExtension("noext", " ")).isEqualTo("bin");
    }
}
