package org.example.transliterationpersiantoengilish.service;

import lombok.extern.slf4j.Slf4j;
import org.example.transliterationpersiantoengilish.model.DictionaryWord;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

@Slf4j

public class PronunciationLoaderService {



    private final Map<String, String> pronunciationMap = new HashMap<>();
    private final List<DictionaryWord> wordList = new ArrayList<>();
    private static final String DATA_FILE_PATH = "persian_phonetics.txt";



    public Map<String, String> loadPronunciationData() throws IOException {

        ClassPathResource resource = new ClassPathResource(DATA_FILE_PATH);
        if (!resource.exists()) {
            log.error("Data file not found in classpath: {}", DATA_FILE_PATH);
            throw new FileNotFoundException("File not found: " + DATA_FILE_PATH);
        }

        log.info("Loading Persian pronunciation data from: {}", resource.getFilename());
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineCount = 0;
            int errorCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String dictionaryWord = parts[0].trim();
                    String pronunciation = parts[1].trim();
                    dictionaryWord = Normalizer.normalize(dictionaryWord, Normalizer.Form.NFC);
                    if (!dictionaryWord.isEmpty() && !pronunciation.isEmpty()) {
                        if(dictionaryWord.length() <2 ){
                            log.info(dictionaryWord+":"+pronunciation);
                        }else {
                            pronunciationMap.put(getNormalizations(dictionaryWord), pronunciation);
                        }
                        wordList.add(new DictionaryWord(dictionaryWord, pronunciation));
                    } else {
                        errorCount++;
                        log.warn("Empty field on line {}: {}", lineCount, line);
                    }
                } else {
                    errorCount++;
                    log.warn("Invalid format on line {}: {}", lineCount, line);
                }
            }
            log.info("Successfully loaded {} words. Errors: {}",
                    pronunciationMap.size(), errorCount);
        } catch (Exception e) {
            log.error("Failed to load pronunciation data", e);
            throw e;
        }
        return pronunciationMap;
    }

    @Cacheable(value = "pronunciation-cache", key = "'pronunciationMap'")
    public Map<String, String> getPronunciationMap() throws IOException {
        return loadPronunciationData();
    }

    public String getNormalizations(String word) {
        word = Normalizer.normalize(word, Normalizer.Form.NFC);
        word = Normalizer.normalize(word, Normalizer.Form.NFD);
        word = Normalizer.normalize(word, Normalizer.Form.NFKC);
        word = Normalizer.normalize(word, Normalizer.Form.NFKD);
        word = word.replace('ي', 'ی').replace('ك', 'ک');
        word = word.replace('ي', 'ی');
        word = word.replace('ك', 'ک');
        word = word.replaceAll("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]", "");
        word = word.replaceAll("[\\u200C\\u200D\\u200E\\u200F]", "");
        word = word.replaceAll("\\s+", " ");
        word = word.replace("ـ", "");
        return word;
    }
}




