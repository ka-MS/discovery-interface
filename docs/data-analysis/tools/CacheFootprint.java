import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;

// Local measurement only: no database connection, application changes or dependencies.
public class CacheFootprint {
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation value) {
        instrumentation = value;
    }

    private static long retainedGraphBytes(Object root) throws Exception {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> pending = new ArrayDeque<>();
        pending.push(root);
        long bytes = 0;
        while (!pending.isEmpty()) {
            Object value = pending.pop();
            if (!visited.add(value)) continue;
            bytes += instrumentation.getObjectSize(value);
            Class<?> type = value.getClass();
            if (type.isArray()) {
                if (!type.getComponentType().isPrimitive()) {
                    for (Object item : (Object[]) value) if (item != null) pending.push(item);
                }
                continue;
            }
            for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
                for (Field field : owner.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                    field.setAccessible(true);
                    Object item = field.get(value);
                    if (item != null) pending.push(item);
                }
            }
        }
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("FILE\tROWS\tCOLUMNS\tTSV_BYTES\tARRAY_MAP_HEAP_BYTES\tCOLUMN_MAP_HEAP_BYTES");
        for (String name : args) {
            Path file = Path.of(name);
            Map<Long, String[]> rows = new HashMap<>();
            Map<Long, Map<String, String>> columnRows = new HashMap<>();
            String[] columns;
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                columns = reader.readLine().split("\t", -1);
                int idColumn = Arrays.asList(columns).indexOf(
                        file.getFileName().toString().startsWith("assetattribute") ? "ASSETATTRIBUTEID" : "CLASSSPECID");
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] cells = line.split("\t", -1);
                    long id = Long.parseLong(cells[idColumn]);
                    if (rows.put(id, cells) != null) throw new IllegalStateException("Duplicate key");
                    Map<String, String> entry = new HashMap<>();
                    for (int i = 0; i < columns.length; i++) entry.put(columns[i], cells[i]);
                    columnRows.put(id, entry);
                }
            }
            System.out.printf("%s\t%d\t%d\t%d\t%d\t%d%n", file.getFileName(), rows.size(),
                    columns.length, Files.size(file), retainedGraphBytes(rows), retainedGraphBytes(columnRows));
        }
    }
}
