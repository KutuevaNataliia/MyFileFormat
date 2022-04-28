import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class MyFile {
    public static final int id = 88;
    private static final int version = 1;
    private static final int blockSize = 16;
    private static final int captionSize = 12;

    public static void makeFile(String fileName, String text, String charsetName) throws IOException {
        byte[] rawData;
        int blocId;
        switch (charsetName) {
            case "ASCII" -> {
                rawData = text.getBytes(StandardCharsets.US_ASCII);
                blocId = 1;
            }
            case "UNICODE" -> {
                rawData = text.getBytes(StandardCharsets.UTF_16);
                blocId = 2;
            }
            case "BLOB" -> {
                rawData = Base64.getEncoder().encode(text.getBytes(StandardCharsets.UTF_8));
                blocId = 3;
            }
            default -> throw new UnsupportedEncodingException();
        }
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
        Files.write(path, idBytes);
        byte[] verBytes = ByteBuffer.allocate(4).putInt(version).array();
        Files.write(path, verBytes, StandardOpenOption.APPEND);
        byte[] captionSizeBytes = ByteBuffer.allocate(4).putInt(captionSize).array();
        Files.write(path, captionSizeBytes, StandardOpenOption.APPEND);

        byte[] blockIdBytes = ByteBuffer.allocate(4).putInt(blocId).array();
        byte[] blockSizeBytes = ByteBuffer.allocate(4).putInt(blockSize).array();
        List<byte[]> blocks = divideArray(rawData, blockSize);
        for (int i = 0; i < blocks.size() - 1; i++) {
            Files.write(path, blockIdBytes, StandardOpenOption.APPEND);
            Files.write(path, blockSizeBytes, StandardOpenOption.APPEND);
            Files.write(path, blocks.get(i), StandardOpenOption.APPEND);
        }
        Files.write(path, blockIdBytes, StandardOpenOption.APPEND);
        int lastBlockSize = blocks.get(blocks.size() - 1).length;
        byte[] lastBlockSizeBytes = ByteBuffer.allocate(4).putInt(lastBlockSize).array();
        Files.write(path, lastBlockSizeBytes, StandardOpenOption.APPEND);
        Files.write(path, blocks.get(blocks.size() - 1), StandardOpenOption.APPEND);
    }

    public static String readFile(String fileName) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(fileName));
        int id = ByteBuffer.wrap(Arrays.copyOfRange(fileContent, 0, 4)).getInt();
        if (id != 88) {
            throw new RuntimeException("Неправильный формат файла");
        }
        int version = ByteBuffer.wrap(Arrays.copyOfRange(fileContent, 4, 8)).getInt();
        int captionSize = ByteBuffer.wrap(Arrays.copyOfRange(fileContent, 8, 12)).getInt();
        List <byte[]> blocks = divideArray(Arrays.copyOfRange(fileContent, 12, fileContent.length), blockSize + 8);
        StringBuilder stringBuilder = new StringBuilder();
        for (byte[] block : blocks) {
            int blockId = ByteBuffer.wrap(Arrays.copyOfRange(block, 0, 4)).getInt();
            Charset charset;
            switch (blockId) {
                case 1 -> charset = StandardCharsets.US_ASCII;
                case 2 -> charset = StandardCharsets.UTF_16;
                default -> charset = null;
            }
            int blockSizeRead = ByteBuffer.wrap(Arrays.copyOfRange(block, 4, 8)).getInt();
            String str;
            if (charset == StandardCharsets.US_ASCII || charset == StandardCharsets.UTF_16) {
                str = new String(Arrays.copyOfRange(block, 8, blockSizeRead + 8), charset);
            } else {
                str = new String(Base64.getDecoder().decode(Arrays.copyOfRange(block, 8, blockSizeRead + 8)));
            }
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

    static List<byte[]> divideArray(byte[] source, int chunksize) {

        List<byte[]> result = new ArrayList<>();
        int start = 0;
        while (start < source.length) {
            int end = Math.min(source.length, start + chunksize);
            result.add(Arrays.copyOfRange(source, start, end));
            start += chunksize;
        }

        return result;
    }

}
