/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooregreatsoftware.gradle.defaults;

import com.mooregreatsoftware.LangUtils;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static com.mooregreatsoftware.LangUtils.tryGet;
import static com.mooregreatsoftware.LangUtils.tryRun;

@SuppressWarnings("WeakerAccess")
public class GitHelper implements Closeable {
    public final Git git;
    private @Nullable File remoteRepoDir;


    private GitHelper(Git git) {
        this.git = git;
        this.remoteRepoDir = null;
    }


    public static GitHelper newRepo(File projectDir) {
        return tryGet(() -> {
            final Git git = Git.init().setDirectory(projectDir).call();
            return new GitHelper(git);
        });
    }


    @Override
    public void close() {
        git.close();
    }


    public GitHelper setupRepo() {
        createGitIgnore();

        addFilePattern(".gitignore");
        commit("init");

        tag("v1.0.0");

        return this;
    }


    public GitHelper commit(String message) {
        tryRun(() -> git.commit().setMessage(message).call());
        return this;
    }


    public GitHelper addFilePattern(String filepattern) {
        tryRun(() -> git.add().addFilepattern(filepattern).call());
        return this;
    }


    public GitHelper tag(String name) {
        tryRun(() -> git.tag().setName(name).call());
        return this;
    }


    public File createGitIgnore() {
        return tryGet(() -> {
            final File file = createFile(git.getRepository().getWorkTree(), ".gitignore");
            write(file, fileWriter ->
                fileWriter.append(
                    ".gradle-test-kit/\n" +
                        ".git-remote-bare-repo/\n" +
                        "userHome/\n")
            );
            return file;
        });
    }


    public GitHelper push() {
        tryRun(() -> {
            createRemoteOrigin();
            git.push().setRemote("origin").setPushAll().call();
        });
        return this;
    }


    public static File createFile(File parentDir, String filename) throws IOException {
        final File file = new File(parentDir, filename);
        if (file.exists()) return file;
        write(file, fileWriter -> fileWriter.write(new char[0]) /* "touch" the file */);
        return file;
    }


    public interface ExceptionConsumer<T> {
        void accept(T value) throws Exception;
    }


    public static File write(File file, ExceptionConsumer<FileWriter> fileWriterConsumer) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriterConsumer.accept(fileWriter);
            return file;
        }
        catch (Exception e) {
            throw LangUtils.softened(e);
        }
        finally {
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                }
                catch (IOException e) {
                    // ignore stupid exceptions
                }
            }
        }
    }


    public GitHelper createRemoteOrigin() {
        if (this.remoteRepoDir != null) return this;

        this.remoteRepoDir = tryGet(this::createRemoteRepoDir);

        return this;
    }


    private File createRemoteRepoDir() throws IOException, GitAPIException, URISyntaxException {
        val remoteRepoDir = Files.createDirectories(new File(git.getRepository().getWorkTree(), ".git-remote-bare-repo").toPath()).toFile();

        val remoteGit = Git.init().setDirectory(remoteRepoDir).setBare(true).call();
        val remotePath = remoteGit.getRepository().getDirectory().getAbsolutePath();
        val uri = new URIish("file://" + remotePath);

        val config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", uri.toString());
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.setString("branch", "master", "remote", "origin");
        config.setString("branch", "master", "merge", "refs/heads/master");
        config.save();
        return remoteRepoDir;
    }

}
