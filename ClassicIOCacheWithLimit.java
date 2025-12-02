import java.io.*;
import java.util.*;

public class ClassicIOCacheWithLimit {
    private Map<String, FileCacheEntry> cache;
    private int maxSize;

    // Внутренний класс для хранения данных файла
    private static class FileCacheEntry {
        String content;
        long lastReadTime;
        long lastModifiedTimeAtRead;

        public FileCacheEntry(String content, long lastReadTime, long lastModifiedTimeAtRead) {
            this.content = content;
            this.lastReadTime = lastReadTime;
            this.lastModifiedTimeAtRead = lastModifiedTimeAtRead;
        }
    }

    // Конструкторы
    public ClassicIOCacheWithLimit(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, FileCacheEntry> eldest) {
                return size() > maxSize;
            }
        };
    }

    public ClassicIOCacheWithLimit() {
        this(100);
    }

    // Основной метод чтения файла
    public String readFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        String absolutePath = file.getAbsolutePath();
        long currentModifiedTime = file.lastModified();

        // Проверяем кэш
        if (cache.containsKey(absolutePath)) {
            FileCacheEntry cachedEntry = cache.get(absolutePath);
            if (isCacheValid(cachedEntry, currentModifiedTime)) {
                cachedEntry.lastReadTime = System.currentTimeMillis();
                return cachedEntry.content;
            }
        }

        // Обновляем кэш
        return updateCache(file, absolutePath, currentModifiedTime);
    }

    // Вспомогательные методы
    private boolean isCacheValid(FileCacheEntry cachedEntry, long currentModifiedTime) {
        return cachedEntry.lastModifiedTimeAtRead == currentModifiedTime;
    }

    private String updateCache(File file, String absolutePath, long currentModifiedTime) throws IOException {
        String content = readFileContent(file);
        FileCacheEntry newEntry = new FileCacheEntry(content, System.currentTimeMillis(), currentModifiedTime);
        cache.put(absolutePath, newEntry);
        return content;
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // Методы управления кэшем
    public void invalidate(String filePath) {
        cache.remove(filePath);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public boolean isCached(String filePath) {
        return cache.containsKey(filePath);
    }

    public int getCachedFilesCount() {
        return cache.size();
    }

    // Методы статистики
    public long getCacheSizeInMemory() {
        return cache.values().stream()
                .mapToLong(entry -> entry.content.length() * 2L)
                .sum();
    }

    public void printCacheStats() {
        System.out.println("Cache Stats:");
        System.out.println("Files count: " + getCachedFilesCount());
        System.out.println("Size in memory (bytes): " + getCacheSizeInMemory());
        System.out.println("Max size: " + maxSize);
        System.out.println("Cached files:");
        cache.forEach((path, entry) ->
            System.out.printf("  - %s (size: %d bytes, last read: %tc)%n",
                path, entry.content.length() * 2, entry.lastReadTime)
        );
    }

    // Тестовый метод
    public static void main(String[] args) {
        try {
            ClassicIOCacheWithLimit cache = new ClassicIOCacheWithLimit(2);

            // Создаём тестовые файлы
            File testFile1 = new File("test1.txt");
            File testFile2 = new File("test2.txt");
            File testFile3 = new File("test3.txt");

            try (PrintWriter writer = new PrintWriter(testFile1)) {
                writer.println("Content of test1");
            }
            try (PrintWriter writer = new PrintWriter(testFile2)) {
                writer.println("Content of test2");
            }
            try (PrintWriter writer = new PrintWriter(testFile3)) {
                writer.println("Content of test3");
            }

            // Демонстрация работы кэша
            System.out.println(cache.readFile("test1.txt")); // Чтение с диска
            System.out.println(cache.readFile("test1.txt")); // Чтение из кэша
            System.out.println(cache.readFile("test2.txt")); // Чтение с диска
            System.out.println(cache.readFile("test3.txt")); // Вытеснение test1

            cache.printCacheStats();

            // Удаление тестовых файлов
            testFile1.delete();
            testFile2.delete();
            testFile3.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


