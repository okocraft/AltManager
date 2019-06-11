package net.okocraft.playermanager.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import lombok.Getter;
import net.okocraft.playermanager.PlayerManager;

public class InventoryUtil {

    private static final File dataFolder = PlayerManager.getInstance().getDataFolder();

    @Getter
    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH:mm:ss");

    /**
     * A method to serialize an inventory to Base64 string.
     * 
     * <p />
     * 
     * Special thanks to Comphenix in the Bukkit forums or also known as aadnk on
     * GitHub.
     * 
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     * 
     * @param inventory to serialize
     * @return Base64 string of the provided inventory
     */
    public static String toBase64(PlayerInventory inventory) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);) {

            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());

            // Save every element in the list
            for (int i = 0; i < inventory.getSize(); i++)
                dataOutput.writeObject(inventory.getItem(i));

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException | IllegalStateException e) {
            return "";
        }
    }

    /**
     * 
     * A method to get an {@link Inventory} from an encoded, Base64, string.
     * 
     * <p />
     * 
     * Special thanks to Comphenix in the Bukkit forums or also known as aadnk on
     * GitHub.
     * 
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     * 
     * @param data Base64 string of data containing an inventory.
     * @return Inventory created from the Base64 string.
     */
    public static Inventory fromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);) {
            int originalInventorySize = dataInput.readInt();
            int inventorySize = ((originalInventorySize / 9) + 1) * 9;
            Inventory inventory = Bukkit.getServer().createInventory(null, inventorySize, "Inventory Backup");

            // Read the serialized inventory
            for (int i = 0; i < originalInventorySize; i++)
                inventory.setItem(i, (ItemStack) dataInput.readObject());

            return inventory;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return Bukkit.getServer().createInventory(null, 45, "Inventory Backup");
        }
    }

    /**
     * Backup inventory of the {@code player} in
     * /basedir/inventory/year/month/day/player-uuid.log
     * 
     * @param player
     */
    public static void backupInventory(Player player) {
        LocalDateTime time = LocalDateTime.now(ZoneId.systemDefault());

        String timeFormat = time.format(format);

        Path filePath = dataFolder.toPath().resolve("inventory").resolve(timeFormat)
                .resolve(player.getUniqueId().toString() + ".log");
        File logFile = filePath.toFile();

        try {
            if (!logFile.exists()) {
                Files.createDirectories(filePath.getParent());
                logFile.createNewFile();
            } else if (logFile.isDirectory()) {
                logFile.createNewFile();
            }

            FileWriter fw = new FileWriter(logFile, true);
            fw.append(toBase64(player.getInventory()));
            fw.close();
        } catch (IOException exception) {
            exception.printStackTrace();
            return;
        }
    }

    public static String fromBackup(OfflinePlayer player, int year, int month, int day, int hour, int minute,
            int second) {
        Optional<File> backupFile = getBackupFile(player, year, month, day, hour, minute, second);
        if (backupFile.isPresent()) {
            return fromBackup(backupFile.get().toPath());
        } else {
            return "";
        }
    }

    public static String fromBackup(OfflinePlayer player, int year, int month, int day, int hour, int minute) {
        Path invFolderPath = dataFolder.toPath().resolve("inventory");
        Path recentBackupPath = invFolderPath
                .resolve(Arrays.asList(invFolderPath.toFile().list()).stream()
                        .filter(fileName -> fileName.matches("(\\d){4}(-(\\d){2}){2}_(\\d{2}:){2}\\d{2}"))
                        .map(fileName -> LocalDateTime.parse(fileName, format)).filter(date -> date.getYear() == year)
                        .filter(date -> date.getMonthValue() == month).filter(date -> date.getDayOfMonth() == day)
                        .filter(date -> date.getHour() == hour).filter(date -> date.getMinute() == minute)
                        .reduce(LocalDateTime.MIN, (x, y) -> x.compareTo(y) >= 0 ? x : y).format(format))
                .resolve(player.getUniqueId().toString() + ".log");

        return fromBackup(recentBackupPath);
    }

    public static String fromBackup(OfflinePlayer player, int year, int month, int day, int hour) {
        Path invFolderPath = dataFolder.toPath().resolve("inventory");
        Path recentBackupPath = invFolderPath
                .resolve(Arrays.asList(invFolderPath.toFile().list()).stream()
                        .filter(fileName -> fileName.matches("(\\d){4}(-(\\d){2}){2}_(\\d{2}:){2}\\d{2}"))
                        .map(fileName -> LocalDateTime.parse(fileName, format)).filter(date -> date.getYear() == year)
                        .filter(date -> date.getMonthValue() == month).filter(date -> date.getDayOfMonth() == day)
                        .filter(date -> date.getHour() == hour)
                        .reduce(LocalDateTime.MIN, (x, y) -> x.compareTo(y) >= 0 ? x : y).format(format))
                .resolve(player.getUniqueId().toString() + ".log");

        return fromBackup(recentBackupPath);
    }

    public static String fromBackup(OfflinePlayer player, int year, int month, int day) {
        Path invFolderPath = dataFolder.toPath().resolve("inventory");
        Path recentBackupPath = invFolderPath
                .resolve(Arrays.asList(invFolderPath.toFile().list()).stream()
                        .filter(fileName -> fileName.matches("(\\d){4}(-(\\d){2}){2}_(\\d{2}:){2}\\d{2}"))
                        .map(fileName -> LocalDateTime.parse(fileName, format)).filter(date -> date.getYear() == year)
                        .filter(date -> date.getMonthValue() == month).filter(date -> date.getDayOfMonth() == day)
                        .reduce(LocalDateTime.MIN, (x, y) -> x.compareTo(y) >= 0 ? x : y).format(format))
                .resolve(player.getUniqueId().toString() + ".log");

        return fromBackup(recentBackupPath);
    }

    public static String fromBackup(OfflinePlayer player, int year, int month) {
        Path invFolderPath = dataFolder.toPath().resolve("inventory");
        Path recentBackupPath = invFolderPath
                .resolve(Arrays.asList(invFolderPath.toFile().list()).stream()
                        .filter(fileName -> fileName.matches("(\\d){4}(-(\\d){2}){2}_(\\d{2}:){2}\\d{2}"))
                        .map(fileName -> LocalDateTime.parse(fileName, format)).filter(date -> date.getYear() == year)
                        .filter(date -> date.getMonthValue() == month)
                        .reduce(LocalDateTime.MIN, (x, y) -> x.compareTo(y) >= 0 ? x : y).format(format))
                .resolve(player.getUniqueId().toString() + ".log");

        return fromBackup(recentBackupPath);
    }

    public static String fromBackup(OfflinePlayer player, int year) {
        Path invFolderPath = dataFolder.toPath().resolve("inventory");
        Path recentBackupPath = invFolderPath
                .resolve(Arrays.asList(invFolderPath.toFile().list()).stream()
                        .filter(fileName -> fileName.matches("(\\d){4}(-(\\d){2}){2}_(\\d{2}:){2}\\d{2}"))
                        .map(fileName -> LocalDateTime.parse(fileName, format)).filter(date -> date.getYear() == year)
                        .reduce(LocalDateTime.MIN, (x, y) -> x.compareTo(y) >= 0 ? x : y).format(format))
                .resolve(player.getUniqueId().toString() + ".log");

        return fromBackup(recentBackupPath);
    }

    public static String fromBackup(OfflinePlayer player) {
        Path invFolderPath = dataFolder.toPath().resolve("inventory");
        Path recentBackupPath = invFolderPath
                .resolve(Arrays.asList(invFolderPath.toFile().list()).stream()
                        .filter(fileName -> fileName.matches("(\\d){4}(-(\\d){2}){2}_(\\d{2}:){2}\\d{2}"))
                        .map(fileName -> LocalDateTime.parse(fileName, format))
                        .reduce(LocalDateTime.MIN, (x, y) -> x.compareTo(y) >= 0 ? x : y).format(format))
                .resolve(player.getUniqueId().toString() + ".log");

        return fromBackup(recentBackupPath);
    }

    public static String fromBackup(Path path) {

        StringBuilder sb = new StringBuilder();
        try {
            Files.exists(path);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (lines.size() == lines.indexOf(line) + 1)
                    sb.append(line);
                else
                    sb.append(line + "\n");
            }

        } catch (NoSuchFileException exception) {
            return "";
        } catch (IOException exception) {
            exception.printStackTrace();
            return "";
        }
        return sb.toString();
    }

    public static void restoreInventory(Player player, File backupFile) {
        PlayerInventory playerInv = player.getInventory();
        Inventory backupInv = fromBase64(fromBackup(backupFile.toPath()));
        for (int i = 0; i <= 40; i++) {
            playerInv.setItem(i, backupInv.getItem(i));
        }
    }

    public static Optional<File> getBackupFile(OfflinePlayer player, int year, int month, int day, int hour, int minute,
            int second) {
        String specifiedTime = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day) + "_"
                + String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":"
                + String.format("%02d", second);
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .stream().filter(path -> path.getParent().toFile().getName().equals(specifiedTime)).map(Path::toFile)
                .findFirst();
    }

    public static void packDayBackups() {
        Path invBackupFolderPath = dataFolder.toPath().resolve("inventory");
        LocalDateTime today = LocalDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS);
        Arrays.asList(invBackupFolderPath.toFile().list()).stream()
                .filter(fileName -> fileName.matches("^\\d{4}(-\\d{2}){2}_.*$"))
                .map(fileName -> LocalDateTime.parse(fileName, format)).filter(date -> date.compareTo(today) < 0)
                .map(date -> date.format(format)).forEach(fileName -> {
                    Path sourcePath = invBackupFolderPath.resolve(fileName);
                    Path targetPath = invBackupFolderPath
                            .resolve(fileName.replaceAll("^(\\d{4}(-\\d{2}){2})_.*$", "$1")).resolve(fileName);
                    try {
                        Files.createDirectories(targetPath.getParent());
                        Files.move(sourcePath, targetPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void packMonthBackups() {
        Path invBackupFolderPath = dataFolder.toPath().resolve("inventory");
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        LocalDate thisMonth = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1);
        Arrays.asList(invBackupFolderPath.toFile().list()).stream()
                .filter(fileName -> fileName.matches("^\\d{4}(-\\d{2}){2}$"))
                .map(fileName -> LocalDate.parse(fileName, monthFormat)).filter(date -> date.compareTo(thisMonth) < 0)
                .map(date -> date.format(monthFormat)).forEach(fileName -> {
                    Path sourcePath = invBackupFolderPath.resolve(fileName);
                    Path targetPath = invBackupFolderPath.resolve(fileName.replaceAll("^(\\d{4}-\\d{2})-\\d{2}$", "$1"))
                            .resolve(fileName);
                    try {
                        Files.createDirectories(targetPath.getParent());
                        Files.move(sourcePath, targetPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void packYearBackups() {
        Path invBackupFolderPath = dataFolder.toPath().resolve("inventory");
        int thisYear = Year.now(ZoneId.systemDefault()).getValue();
        Arrays.asList(invBackupFolderPath.toFile().list()).stream()
                .filter(fileName -> fileName.matches("^\\d{4}-\\d{2}$"))
                .filter(fileName -> Integer.parseInt(fileName.replaceAll("^(\\d{4})-\\d{2}$", "$1")) < thisYear)
                .forEach(fileName -> {
                    Path sourcePath = invBackupFolderPath.resolve(fileName);
                    Path targetPath = invBackupFolderPath.resolve(fileName.replaceAll("^(\\d{4})-\\d{2}$", "$1"))
                            .resolve(fileName);
                    try {
                        Files.createDirectories(targetPath.getParent());
                        Files.move(sourcePath, targetPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * {@code file} 自身およびその配下にあるファイルのうちで、名前が{@code targetName}にマッチするものを検索する。
     * 
     * @param file       検索するファイル
     * @param targetName 名前 正規表現を使用可能。
     * @return ヒットしたファイルパスのリスト
     */
    public static List<Path> searchFile(File file, String targetName) {
        List<Path> result = new ArrayList<>();
        if (file.getName().matches(targetName))
            result.add(file.toPath());
        if (!file.isDirectory())
            return result;
        for (File subDir : file.listFiles())
            result.addAll(searchFile(subDir, targetName));
        return result;
    }

    public static List<String> getBackupYears(OfflinePlayer player) {
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .parallelStream().map(path -> path.getParent().toFile().getName())
                .map(fileName -> fileName.substring(0, 4)).filter(first4 -> first4.matches("\\d\\d\\d\\d")).distinct()
                .collect(Collectors.toList());
    }

    public static List<String> getBackupMonth(OfflinePlayer player, int year) {
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .parallelStream().map(path -> path.getParent().toFile().getName())
                .filter(fileName -> fileName.substring(0, 4).equals(String.valueOf(year)))
                .map(fileName -> fileName.substring(5, 7)).filter(first2 -> first2.matches("\\d\\d")).distinct()
                .collect(Collectors.toList());
    }

    public static List<String> getBackupDay(OfflinePlayer player, int year, int month) {
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .stream().map(path -> path.getParent().toFile().getName()).peek(test -> System.out.println(test))
                .filter(fileName -> fileName.substring(0, 4).equals(String.valueOf(year)))
                .peek(test -> System.out.println(test))
                .peek(test -> System.out.println(test.substring(5, 7) + " " + month))
                .filter(fileName -> fileName.substring(5, 7).equals(String.format("%02d", month)))
                .peek(test -> System.out.println(test)).map(fileName -> fileName.substring(8, 10))
                .filter(first2 -> first2.matches("\\d\\d")).distinct().peek(test -> System.out.println(test))
                .collect(Collectors.toList());
    }

    public static List<String> getBackupHour(OfflinePlayer player, int year, int month, int day) {
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .parallelStream().map(path -> path.getParent().toFile().getName())
                .filter(fileName -> fileName.substring(0, 4).equals(String.valueOf(year)))
                .filter(fileName -> fileName.substring(5, 7).equals(String.format("%02d", month)))
                .filter(fileName -> fileName.substring(8, 10).equals(String.format("%02d", day)))
                .map(fileName -> fileName.substring(11, 13)).filter(first2 -> first2.matches("\\d\\d")).distinct()
                .collect(Collectors.toList());
    }

    public static List<String> getBackupMinute(OfflinePlayer player, int year, int month, int day, int hour) {
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .parallelStream().map(path -> path.getParent().toFile().getName())
                .filter(fileName -> fileName.substring(0, 4).equals(String.valueOf(year)))
                .filter(fileName -> fileName.substring(5, 7).equals(String.format("%02d", month)))
                .filter(fileName -> fileName.substring(8, 10).equals(String.format("%02d", day)))
                .filter(fileName -> fileName.substring(11, 13).equals(String.format("%02d", hour)))
                .map(fileName -> fileName.substring(14, 16)).filter(first2 -> first2.matches("\\d\\d")).distinct()
                .collect(Collectors.toList());
    }

    public static List<String> getBackupSecond(OfflinePlayer player, int year, int month, int day, int hour,
            int minute) {
        return searchFile(dataFolder.toPath().resolve("inventory").toFile(), player.getUniqueId().toString() + ".log")
                .parallelStream().map(path -> path.getParent().toFile().getName())
                .filter(fileName -> fileName.substring(0, 4).equals(String.valueOf(year)))
                .filter(fileName -> fileName.substring(5, 7).equals(String.format("%02d", month)))
                .filter(fileName -> fileName.substring(8, 10).equals(String.format("%02d", day)))
                .filter(fileName -> fileName.substring(11, 13).equals(String.format("%02d", hour)))
                .filter(fileName -> fileName.substring(14, 16).equals(String.format("%02d", minute)))
                .map(fileName -> fileName.substring(17, 19)).filter(first2 -> first2.matches("\\d\\d")).distinct()
                .collect(Collectors.toList());
    }
}