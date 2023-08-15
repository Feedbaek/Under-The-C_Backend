package LikeLion.UnderTheCBackend.service;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageService {

    public ResponseEntity<byte[]> downloadImage(String imageName) throws IOException {
        ClassPathResource imageFile = new ClassPathResource("images/" + imageName);
        if (!imageFile.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.");
        InputStream imageStream = imageFile.getInputStream();

        byte[] imageByteArray = imageStream.readAllBytes();
        imageStream.close();

        return new ResponseEntity<>(imageByteArray, HttpStatus.OK);
    }
}