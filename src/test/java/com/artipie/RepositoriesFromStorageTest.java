/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RepositoriesFromStorage}.
 *
 * @since 0.14
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RepositoriesFromStorageTest {

    /**
     * Repo name.
     */
    private static final String REPO = "my-repo";

    /**
     * Type repository.
     */
    private static final String TYPE = "maven";

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void findRepoSettingAndCreateRepoConfigWithStorageAlias() {
        final String alias = "default";
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        this.saveAliasConfig(alias);
        MatcherAssert.assertThat(
            this.repoConfig()
                .storageOpt()
                .isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void findRepoSettingAndCreateRepoConfigWithCustomStorage() {
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withFileStorage(Path.of("some", "somepath"))
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        MatcherAssert.assertThat(
            this.repoConfig()
                .storageOpt()
                .isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void throwsExceptionWhenConfigYamlAbsent() {
        final CompletionException result = Assertions.assertThrows(
            CompletionException.class,
            this::repoConfig
        );
        MatcherAssert.assertThat(
            result.getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
    }

    @Test
    void throwsExceptionWhenConfigYamlMalformedSinceWithoutStorage() {
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storage()
        );
    }

    @Test
    void throwsExceptionWhenAliasesConfigAbsent() {
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias("alias")
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    @Test
    void throwsExceptionWhenAliasConfigMalformedSinceSequenceInsteadMapping() {
        final String alias = "default";
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        this.storage.save(
            new Key.From(StorageAliases.FILE_NAME),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "storages", Yaml.createYamlSequenceBuilder()
                        .add(
                            Yaml.createYamlMappingBuilder().add(
                                alias, Yaml.createYamlMappingBuilder()
                                    .add("type", "fs")
                                    .add("path", "/some/path")
                                    .build()
                        ).build()
                    ).build()
                ).build().toString().getBytes()
            )
        ).join();
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    @Test
    void throwsExceptionForUnknownAlias() {
        this.saveAliasConfig("some alias");
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias("unknown alias")
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    private RepoConfig repoConfig() {
        return new RepositoriesFromStorage(this.storage)
            .config(RepositoriesFromStorageTest.REPO)
            .toCompletableFuture().join();
    }

    private void saveAliasConfig(final String alias) {
        this.storage.save(
            new Key.From(StorageAliases.FILE_NAME),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "storages", Yaml.createYamlMappingBuilder()
                        .add(
                            alias, Yaml.createYamlMappingBuilder()
                                .add("type", "fs")
                                .add("path", "/some/path")
                                .build()
                        ).build()
                ).build().toString().getBytes()
            )
        ).join();
    }

}
