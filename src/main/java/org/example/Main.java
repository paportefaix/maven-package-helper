package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws IOException {
        // dossier que vous voulez updater (susceptible de ne pas marcher avec les path directement sous repository)
        var pathRepositoryToExplore = "C:/Users/a926932/.m2/repository_sae";
        // dosser racine du repository
        var pathRepositoryRoot = "C:/Users/a926932/.m2/repository_sae";
        // dossier cible (a créer) où copier les jar et pom avant de les upload (nécessaire car la commande maven va extraire le pom des jar)
        var pathDirectoryLibToUpload = "C:/Users/a926932/.m2/flat_lib_test";

        Path startPath = Paths.get(pathRepositoryToExplore);
        var data = walkFileTree(startPath);

        System.out.println("Voici la liste des commandes à exécuter");
        runThroughDependencies(data, pathDirectoryLibToUpload, pathRepositoryRoot);
    }


    private static void runThroughDependencies(List<DirectoryInfo> directoryInfos, String pathDirectoryLibToUpload, String pathRepositoryRoot) {
        directoryInfos.forEach(directoryInfo -> {
            var pathLibrary = directoryInfo.directoryPath;
            //System.out.println(STR."Path:  \{pathLibrary}");
            var version = getVersion(pathLibrary);
            //System.out.println(STR."Version:  \{version}");

            var jar = filterUnwantedJarFiles(directoryInfo.jarFiles, version);
            var groupId = getGroupId(pathRepositoryRoot, pathLibrary);


            if (jar.isPresent()) {
                manageJarFile(pathDirectoryLibToUpload, pathLibrary, jar, version, groupId);
            } else {
                managePomFile(pathDirectoryLibToUpload, directoryInfo, version, pathLibrary, groupId);
            }
        });
    }

    private static void manageJarFile(String pathDirectoryLibToUpload, String pathLibrary, Optional<String> jar, String version, String groupId) {
        var newPath = copyFileToDestination(STR."\{pathLibrary}/\{jar.get()}", pathDirectoryLibToUpload);
        var artifactId = getArtifactId(jar.get(), version);
        //System.out.println(STR." JAR:  \{jar.get()}");
        var commandToExecute = buildCommand(Type.JAR, newPath.toString(), groupId, artifactId, version);
        System.out.println(commandToExecute);
    }

    private static void managePomFile(String pathDirectoryLibToUpload, DirectoryInfo directoryInfo, String version, String pathLibrary, String groupId) {
        var pom = filterUnwantedPomFiles(directoryInfo.pomFiles, version);

        if (pom.isEmpty()) {
            System.err.println(STR."Pom no found for \{pathLibrary}");
        } else {

            var newPath = copyFileToDestination(STR."\{pathLibrary}/\{pom.get()}", pathDirectoryLibToUpload);
            var artifactId = getArtifactId(pom.get(), version);
            //System.out.println(STR." POM:  \{pom.get()}");
            var commandToExecute = buildCommand(Type.POM, newPath.toString(), groupId, artifactId, version);
            //executeMavenPush(commandToExecute);
            System.out.println(commandToExecute);
        }
    }

    private static Path copyFileToDestination(String sourcePathStr, String destinationDirStr) {
        Path sourcePath = Paths.get(sourcePathStr);
        Path destinationDir = Paths.get(destinationDirStr);
        Path destinationPath = destinationDir.resolve(sourcePath.getFileName());
        try {
            return Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Optional<String> filterUnwantedJarFiles(List<String> files, String version) {
        return files.stream().filter(file -> file.contains(STR."-\{version}.jar")).findFirst();
    }

    public static Optional<String> filterUnwantedPomFiles(List<String> files, String version) {
        return files.stream().filter(file -> file.contains(STR."-\{version}.pom")).findFirst();
    }

    public static String getVersion(String directoryPath) {
        var path = Paths.get(directoryPath);
        return path.getFileName().toString();
    }

    public static String getArtifactId(String fileName, String version) {
        return fileName.replace(STR."-\{version}", "").replace(".pom", "").replace(".jar", "");
    }

    public static String getGroupId(String pathRepositoryRoot, String pathLibrary) {
        Path repoPath = Paths.get(pathRepositoryRoot);
        Path fileFullPath = Paths.get(pathLibrary);

        // Trouver le chemin relatif à partir du repository
        Path relativePath = repoPath.relativize(fileFullPath);

        Path groupIdPath = relativePath.getParent().getParent();
        // Convertir le chemin en groupId
        String groupId = groupIdPath.toString().replace(File.separatorChar, '.');

        return groupId;
    }

    private static String buildCommand(Type type, String filePath, String groupId, String artifactId, String version) {
        return STR."mvn deploy:deploy-file -Dfile=\{filePath} -DgroupId=\{groupId} -DartifactId=\{artifactId} -Dversion=\{version} -Dpackaging=\{type.name()} -DrepositoryId=gitlab-maven -Durl=https://gitlab.apps.cne1opsin1clu01.cnam-ens.atos.net/api/v4/projects/979/packages/maven";
    }

    private static List<DirectoryInfo> walkFileTree(Path startPath) throws IOException {
        List<DirectoryInfo> directoryInfos = new ArrayList<>();

        Files.walkFileTree(startPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                List<String> jarFiles = new ArrayList<>();
                List<String> pomFiles = new ArrayList<>();

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path entry : stream) {
                        if (entry.toString().endsWith(".jar")) {
                            jarFiles.add(entry.getFileName().toString());
                        } else if (entry.toString().endsWith(".pom")) {
                            pomFiles.add(entry.getFileName().toString());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!jarFiles.isEmpty() || !pomFiles.isEmpty()) {
                    directoryInfos.add(new DirectoryInfo(dir.toAbsolutePath().toString(), jarFiles, pomFiles));
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return directoryInfos;
    }


    public record DirectoryInfo(
            String directoryPath,
            List<String> jarFiles,
            List<String> pomFiles) {
    }

    enum Type {
        JAR("jar"),
        POM("pom");

        Type(String type) {

        }
    }
}