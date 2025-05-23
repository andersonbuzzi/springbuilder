package br.com.buzzi.builder.generator;

import br.com.buzzi.builder.config.Properties;
import br.com.buzzi.builder.Main;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LoadFile {

    private LoadFile() {

    }

    public static File getFile(String filename) throws Exception {
        File fileInRoot = new File(filename);
        if (fileInRoot.exists()) return fileInRoot;


        String jarPath = new File(Properties.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent();
        File fileInJarDir = Paths.get(jarPath, filename).toFile();
        if (fileInJarDir.exists()) return fileInJarDir;

        if (Main.config.getAppArgs().length > 0) {
            File argFile = new File(Main.config.getAppArgs()[0]);
            if (argFile.exists()) return argFile;
        }

        InputStream is = Properties.class.getClassLoader().getResourceAsStream(filename);
        if (is != null) {
            Path temp = Files.createTempFile("builder-", ".file");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toFile();
        }

        throw new Exception("File " +filename + " not found");
    }
}