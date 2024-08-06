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
        if (args.length != 5) {
            System.out.println("Les paramètres attendu sont les suivants:");
            System.out.println("Premier paramètre: Dossier que vous voulez updater (exemple: C:/Users/bob/.m2/repository/com)");
            System.out.println("Deuxième paramètre: Dosser racine du repository (exemple: C:/Users/bob/.m2/repository)");
            System.out.println("Troisème paramètre: Dossier cible (a créer) où copier les jar et pom avant de les upload (exemple: C:/Users/bob/.m2/repository_to_upload)");
            System.out.println("Quatrième paramètre: Repository id identifié dans settings.xml de maven (exemple: gitlab-maven)");
            System.out.println("Cinquième paramètre: Url package registry (exemple: https://mon_gitlab/api/v4/projects/979/packages/maven)");
            return;
        }
        var pathRepositoryToExplore = args[0];
        var pathRepositoryRoot = args[1];
        var pathDirectoryLibToUpload = args[2];
        var repositoryId = args[3];
        var packageRegistryUrl = args[4];

        Path startPath = Paths.get(pathRepositoryToExplore);
        var data = walkFileTree(startPath);

        System.out.println("Voici la liste des commandes à exécuter");
        runThroughDependencies(data, pathDirectoryLibToUpload, pathRepositoryRoot, repositoryId, packageRegistryUrl);
    }

    private static void runThroughDependencies(List<DirectoryInfo> directoryInfos,
                                               String pathDirectoryLibToUpload,
                                               String pathRepositoryRoot,
                                               String repositoryId,
                                               String urlPackageRegistry) {
        directoryInfos.forEach(directoryInfo -> {
            var pathLibrary = directoryInfo.directoryPath;
            //System.out.println(STR."Path:  \{pathLibrary}");
            var version = getVersion(pathLibrary);
            //System.out.println(STR."Version:  \{version}");

            var jar = filterUnwantedJarFiles(directoryInfo.jarFiles, version);
            var groupId = getGroupId(pathRepositoryRoot, pathLibrary);


            if (jar.isPresent()) {
                manageJarFile(pathDirectoryLibToUpload, pathLibrary, jar, version, groupId, repositoryId, urlPackageRegistry);
            } else {
                managePomFile(pathDirectoryLibToUpload, directoryInfo, version, pathLibrary, groupId, repositoryId, urlPackageRegistry);
            }
        });
    }

    private static void manageJarFile(String pathDirectoryLibToUpload,
                                      String pathLibrary, Optional<String> jar,
                                      String version, String groupId,
                                      String repositoryId,
                                      String urlPackageRegistry) {
        var newPath = copyFileToDestination(STR."\{pathLibrary}/\{jar.get()}", pathDirectoryLibToUpload);
        var artifactId = getArtifactId(jar.get(), version);
        //System.out.println(STR." JAR:  \{jar.get()}");
        var commandToExecute = buildCommand(Type.jar, newPath.toString(), groupId, artifactId, version, repositoryId, urlPackageRegistry);
        System.out.println(commandToExecute);
    }

    private static void managePomFile(String pathDirectoryLibToUpload,
                                      DirectoryInfo directoryInfo,
                                      String version,
                                      String pathLibrary,
                                      String groupId,
                                      String repositoryId,
                                      String urlPackageRegistry) {
        var pom = filterUnwantedPomFiles(directoryInfo.pomFiles, version);

        if (pom.isEmpty()) {
            System.err.println(STR."Pom no found for \{pathLibrary}");
        } else {

            var newPath = copyFileToDestination(STR."\{pathLibrary}/\{pom.get()}", pathDirectoryLibToUpload);
            var artifactId = getArtifactId(pom.get(), version);
            //System.out.println(STR." POM:  \{pom.get()}");
            var commandToExecute = buildCommand(Type.pom, newPath.toString(), groupId, artifactId, version, repositoryId, urlPackageRegistry);
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

    private static String buildCommand(Type type,
                                       String filePath,
                                       String groupId,
                                       String artifactId,
                                       String version,
                                       String repositoryId,
                                       String urlPackageRegistry) {
        return STR."mvn deploy:deploy-file -Dfile=\{filePath} -DgroupId=\{groupId} -DartifactId=\{artifactId} -Dversion=\{version} -Dpackaging=\{type.name()} -DrepositoryId=\{repositoryId} -Durl=\{urlPackageRegistry}";
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
        jar("jar"),
        pom("pom");

        Type(String type) {

        }
    }
}