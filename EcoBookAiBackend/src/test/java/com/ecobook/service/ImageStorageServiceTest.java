package com.ecobook.service;

import com.ecobook.exception.PayloadTooLargeException;
import com.ecobook.repository.TemporaryUploadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    @Mock
    private TemporaryUploadRepository temporaryUploadRepository;

    @Test
    @DisplayName("validateImage should accept payloads exactly at the 5MB limit")
    void shouldAcceptImageExactlyAtFiveMegabytes() throws Exception {
        ImageStorageService service = new ImageStorageService(temporaryUploadRepository);
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 5L);

        byte[] exactBoundaryImage = paddedToExactSize(validPng(), 5 * 1024 * 1024);

        assertThat(service.validateImage(exactBoundaryImage)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("validateImage should reject payloads above the 5MB limit")
    void shouldRejectImageAboveFiveMegabytes() throws Exception {
        ImageStorageService service = new ImageStorageService(temporaryUploadRepository);
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 5L);

        byte[] oversizedImage = paddedToExactSize(validPng(), (5 * 1024 * 1024) + 1);

        assertThatThrownBy(() -> service.validateImage(oversizedImage))
                .isInstanceOf(PayloadTooLargeException.class)
                .hasMessage("A imagem excede o limite de 5MB");
    }

    private byte[] paddedToExactSize(byte[] baseImage, int targetSize) {
        if (baseImage.length > targetSize) {
            throw new IllegalArgumentException("Base image already exceeds target size");
        }

        byte[] padded = Arrays.copyOf(baseImage, targetSize);
        for (int index = baseImage.length; index < padded.length; index++) {
            padded[index] = 0;
        }
        return padded;
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, 0x00695C);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
