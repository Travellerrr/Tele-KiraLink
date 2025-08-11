package cn.travellerr.onebottelegram.converter;

import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import io.github.kasukusakura.silkcodec.SilkCoder;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class AudioConverter {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AudioConverter.class);
    
    /**
     * 将音频路径转换为可用的音频文件
     * @param audioPath 音频路径（支持 http、base64://、file:// 或普通文件路径）
     * @return 处理后的音频文件路径，如果失败则返回 null
     */
    public static String convertToTelegramAudio(String audioPath) {
        try {
            // 检查音频路径类型
            InputStream audioStream = getAudioStream(audioPath);
            if (audioStream == null) {
                log.error("无法获取音频流: {}", audioPath);
                return null;
            }


            String audioFormat = detectAudioFormat(audioStream);
            if (isSupportedFormat(audioFormat)) {
                // 直接支持的格式
                log.info("音频格式已支持，无需转换");
                return "";
            }
            log.info("检测到音频格式: {}", audioFormat);


            // 创建临时文件
            Path tempFile;
            if ("silk".equalsIgnoreCase(audioFormat)) {
                tempFile = convertSilkToPcm(audioPath);
                audioFormat = "pcm";
            } else {
                tempFile = createTempAudioFile(audioStream);
            }

            if (tempFile == null) {
                log.error("无法创建临时音频文件");
                return null;
            }
            
            // 如果需要转换格式
            Path convertedFile = tempFile;
            if (!isSupportedFormat(audioFormat)) {
                if ("pcm".equalsIgnoreCase(audioFormat)) {
                    // 如果是 PCM 格式，转换为 OGG
                    convertedFile = convertToOgg(tempFile, true);
                } else {
                    // 其他格式转换为 OGG
                    convertedFile = convertToOgg(tempFile);
                }
                if (convertedFile == null) {
                    log.error("音频格式转换失败");
                    return null;
                }
                // 删除原始临时文件
                Files.deleteIfExists(tempFile);
            }
            
            log.info("音频处理完成: {}", convertedFile);
            return convertedFile.toString();
            
        } catch (Exception e) {
            log.error("音频转换过程中发生错误", e);
            return null;
        }
    }
    

    
    private static InputStream getAudioStream(String audioPath) {
        try {
            if (audioPath.startsWith("http")) {
                // URL 音频
                URL url = new URL(audioPath);
                return url.openStream();
            } else if (audioPath.startsWith("base64://")) {
                // Base64 编码的音频
                byte[] bytes = Base64.getDecoder().decode(audioPath.substring(9));
                return new ByteArrayInputStream(bytes);
            } else {
                // 文件路径
                String filePath = audioPath.replaceFirst("^file://", "");
                File file = new File(filePath);
                if (file.exists()) {
                    return new FileInputStream(file);
                } else {
                    log.error("音频文件不存在: {}", filePath);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("获取音频流失败: {}", e.getMessage());
            return null;
        }
    }
    
    private static Path createTempAudioFile(InputStream audioStream) {
        try {
            Path tempFile = Files.createTempFile("telegram_audio_", ".tmp");
            
            // 将音频流写入临时文件
            Files.copy(audioStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            audioStream.close();
            
            return tempFile;
        } catch (Exception e) {
            log.error("创建临时音频文件失败: {}", e.getMessage());
            return null;
        }
    }
    
    private static String detectAudioFormat(InputStream stream) {
        try {
            // 读取文件头来检测格式
            byte[] header = new byte[12];
            stream.read(header);

            // 检测常见音频格式
            if (isMp3(header)) {
                return "mp3";
            } else if (isOgg(header)) {
                return "ogg";
            } else if (isWav(header)) {
                return "wav";
            } else if (isFlac(header)) {
                return "flac";
            } else if (isM4a(header)) {
                return "m4a";
            } else if (isAac(header)) {
                return "aac";
            } else if (isSilk(header)) {
                return "silk";
            } else {
                return "unknown";
            }
        } catch (Exception e) {
            log.error("音频格式检测失败: {}", e.getMessage());
            return "unknown";
        }
    }
    
    private static boolean isMp3(byte[] header) {
        return header.length >= 3 && 
               header[0] == (byte) 0xFF && 
               (header[1] & 0xE0) == 0xE0;
    }
    
    private static boolean isOgg(byte[] header) {
        return header.length >= 4 && 
               header[0] == 'O' && 
               header[1] == 'g' && 
               header[2] == 'g' && 
               header[3] == 'S';
    }
    
    private static boolean isWav(byte[] header) {
        return header.length >= 12 && 
               header[0] == 'R' && 
               header[1] == 'I' && 
               header[2] == 'F' && 
               header[3] == 'F' &&
               header[8] == 'W' && 
               header[9] == 'A' && 
               header[10] == 'V' && 
               header[11] == 'E';
    }
    
    private static boolean isFlac(byte[] header) {
        return header.length >= 4 && 
               header[0] == 'f' && 
               header[1] == 'L' && 
               header[2] == 'a' && 
               header[3] == 'C';
    }
    
    private static boolean isM4a(byte[] header) {
        return header.length >= 8 && 
               header[4] == 'f' && 
               header[5] == 't' && 
               header[6] == 'y' && 
               header[7] == 'p';
    }

    private static boolean isSilk(byte[] header) {
        return header.length >= 10 &&
               header[1] == '#' &&
               header[2] == '!' &&
               header[3] == 'S' &&
               header[4] == 'I' &&
               header[5] == 'L' &&
               header[6] == 'K' &&
               header[7] == '_' &&
               header[8] == 'V' &&
               header[9] == '3';
    }
    
    private static boolean isAac(byte[] header) {
        return header.length >= 2 && 
               (header[0] == (byte) 0xFF && (header[1] & 0xF0) == 0xF0);
    }

    
    private static boolean isSupportedFormat(String format) {
        return "mp3".equalsIgnoreCase(format) || "ogg".equalsIgnoreCase(format);
    }

    private static Path convertToOgg(Path inputFile) {
        return convertToOgg(inputFile, false);
    }
    
    public static Path convertToOgg(Path inputFile, boolean isPcm) {
        try {
            String ffmpegPath = cn.travellerr.onebottelegram.TelegramOnebotAdapter.config.getSpring().getFfmpegPath();
            if (ffmpegPath == null || ffmpegPath.trim().isEmpty()) {
                log.error("FFmpeg 路径未配置");
                return null;
            }
            
            Path outputFile = Files.createTempFile("converted_audio_", ".ogg");
            
            // 构建 FFmpeg 命令
            ProcessBuilder pb;
            if (isPcm) {
                pb = new ProcessBuilder(
                        ffmpegPath,
                        "-f", "s16le",
                        "-ar", String.valueOf(TelegramOnebotAdapter.config.getOnebot().getSilkSampleRate() > 0
                                ? TelegramOnebotAdapter.config.getOnebot().getSilkSampleRate()
                                : 24000),
                        "-ac", "1",
                        "-i", inputFile.toString(),
                        "-c:a", "libvorbis",
                        "-q:a", "4",
                        "-y",
                        outputFile.toString()
                );

            } else {
                pb = new ProcessBuilder(
                        ffmpegPath,
                        "-i", inputFile.toString(),
                        "-c:a", "libvorbis",
                        "-q:a", "4",
                        "-y", // 覆盖输出文件
                        outputFile.toString()
                );
            }
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取 FFmpeg 输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 转换失败，退出码: {}", exitCode);
                Files.deleteIfExists(outputFile);
                return null;
            }
            if (isPcm) {
                Files.deleteIfExists(inputFile);
            }
            
            log.info("音频转换成功: {} -> {}", inputFile.getFileName(), outputFile.getFileName());
            return outputFile;
            
        } catch (Exception e) {
            log.error("音频转换过程中发生错误: {}", e.getMessage());
            return null;
        }
    }

    public static Path convertSilkToPcm(String path) {
        try (InputStream inputStream = getAudioStream(path)){
            File tempFile = Files.createTempFile("silk_converted_", ".pcm").toFile();

            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                SilkCoder.decode(inputStream, outputStream, true, 24000, 20);

                outputStream.flush();
            }
            log.info("Silk 转 PCM 成功: {}", tempFile.getAbsolutePath());
            return tempFile.toPath();
        } catch (IOException | UnsupportedOperationException e) {
            log.error("Silk 转 PCM 失败: ", e);
            return null;
        }
    }
}
